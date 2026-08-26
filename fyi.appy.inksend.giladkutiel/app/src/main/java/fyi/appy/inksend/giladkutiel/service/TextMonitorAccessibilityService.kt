package fyi.appy.inksend.giladkutiel.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import fyi.appy.inksend.giladkutiel.R
import fyi.appy.inksend.giladkutiel.data.model.AutoStyle
import fyi.appy.inksend.giladkutiel.data.model.TriggerConfig
import fyi.appy.inksend.giladkutiel.data.repository.SettingsRepository
import fyi.appy.inksend.giladkutiel.engine.ClipboardManagerHelper
import fyi.appy.inksend.giladkutiel.engine.ImageRenderer
import fyi.appy.inksend.giladkutiel.util.TextLengthEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val WHATSAPP_PACKAGE = "com.whatsapp"

/**
 * How long the text must stay unchanged before the button preview translates it. Keeps a
 * translation from firing on every keystroke while the user is still typing.
 */
private const val PREVIEW_DEBOUNCE_MS = 350L

/**
 * Watches WhatsApp's active editable field; when its text length falls within the
 * configured bounds, shows a single floating overlay button that renders the text into a
 * styled image — the style is chosen automatically from the text's content via [AutoStyle] —
 * copies it to the clipboard, and clears the field.
 *
 * The accessibility service is no longer package-scoped in its config (that's what lets it
 * observe the foreground app changing), so styling is kept WhatsApp-only in code: every
 * text event whose packageName isn't [WHATSAPP_PACKAGE] is ignored. The extra reach is
 * used only to auto-hide the overlay the instant the user leaves WhatsApp — a
 * TYPE_WINDOW_STATE_CHANGED from any non-WhatsApp, non-keyboard window tears the button
 * down, and returning to WhatsApp restores it if the tracked field is still focused.
 */
@AndroidEntryPoint
class TextMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var overlayManager: OverlayWindowManager? = null
    private val translator = TextTranslator()
    private var previewJob: Job? = null

    private var currentText: String = ""
    private var activeNode: AccessibilityNodeInfo? = null

    private var currentTrigger = TriggerConfig()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayWindowManager(
            context = this,
            onButtonClicked = { handleStyleButtonPressed() },
        )
        translator.warmUp()

        serviceScope.launch {
            settingsRepository.triggerConfigFlow.collect { trigger ->
                currentTrigger = trigger
                evaluateOverlayVisibility(currentText)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleForegroundWindowChanged(event)

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            -> {
                // Styling is WhatsApp-only; the service just isn't package-scoped anymore.
                if (event.packageName?.toString() != WHATSAPP_PACKAGE) return
                val node = event.source ?: return
                if (node.isEditable) {
                    activeNode = node
                    val text = node.text?.toString() ?: ""
                    currentText = text
                    evaluateOverlayVisibility(text)
                } else if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                    // Focus moved to a non-editable view — nothing left to style.
                    activeNode = null
                    currentText = ""
                    overlayManager?.hideOverlay()
                }
            }
        }
    }

    /**
     * Hide the overlay the moment the foreground window belongs to something other than
     * WhatsApp (home screen, another app, the notification shade), so the button never
     * lingers over an app the user isn't styling text in. The soft keyboard popping up over
     * WhatsApp is itself a separate window whose state change we must ignore, otherwise the
     * button would vanish as soon as the user tapped the message field. Coming back to
     * WhatsApp re-shows the button if the field we were tracking is still the focused one.
     */
    private fun handleForegroundWindowChanged(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        when {
            pkg == WHATSAPP_PACKAGE -> {
                val node = activeNode ?: return
                if (node.refresh() && node.isEditable && node.isFocused) {
                    currentText = node.text?.toString() ?: ""
                    evaluateOverlayVisibility(currentText)
                }
            }
            isKeyboardPackage(pkg) -> Unit
            else -> overlayManager?.hideOverlay()
        }
    }

    /** The current input method (and our own process) show windows over WhatsApp that must not count as "left WhatsApp". */
    private fun isKeyboardPackage(pkg: String): Boolean {
        if (pkg == packageName) return true
        val ime = Settings.Secure
            .getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        return pkg == ime
    }

    private fun evaluateOverlayVisibility(text: String) {
        val withinBounds = TextLengthEvaluator.isWithinBounds(
            trimmedLength = text.trim().length,
            minLength = currentTrigger.minTextLength,
            maxLength = currentTrigger.maxTextLength,
        )
        if (withinBounds) {
            applyButtonPreview(text)
            overlayManager?.showOverlay()
        } else {
            previewJob?.cancel()
            overlayManager?.hideOverlay()
        }
    }

    /**
     * Keeps the overlay button's colour and glyph in sync with the mood [AutoStyle] detects
     * in [text], so it previews the look a tap will produce. The text is first translated to
     * English on-device via [translator] so a single English mood dictionary covers every
     * language; that call is async, so the work is debounced ([PREVIEW_DEBOUNCE_MS]) and the
     * result is discarded if the field changed underneath it. Text matching no mood — or a
     * translation that failed while offline — falls back to the button's neutral default.
     */
    private fun applyButtonPreview(text: String) {
        previewJob?.cancel()
        previewJob = serviceScope.launch {
            delay(PREVIEW_DEBOUNCE_MS)
            val english = translator.toEnglish(text)
            if (text != currentText) return@launch // field moved on while we translated
            val hint = AutoStyle.buttonHintFor(english)
            if (hint != null) {
                overlayManager?.setAppearance(hint.backgroundColorHex, hint.emoji)
            } else {
                overlayManager?.resetAppearance()
            }
        }
    }

    private fun handleStyleButtonPressed() {
        val text = currentText
        if (text.isBlank()) return

        serviceScope.launch {
            val style = AutoStyle.styleFor(translator.toEnglish(text))
            val imageUri = ImageRenderer.generateStyledImageUri(
                this@TextMonitorAccessibilityService, text, style,
            )
            ClipboardManagerHelper.copyImageToClipboard(
                this@TextMonitorAccessibilityService, imageUri,
            )

            activeNode?.let { node ->
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            }

            currentText = ""
            overlayManager?.hideOverlay()
            Toast.makeText(
                this@TextMonitorAccessibilityService,
                getString(R.string.toast_image_copied),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onInterrupt() {
        overlayManager?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        translator.close()
        overlayManager?.hideOverlay()
    }
}
