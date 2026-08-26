package fyi.appy.inksend.giladkutiel.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import fyi.appy.inksend.giladkutiel.R
import fyi.appy.inksend.giladkutiel.data.model.SecondaryStyleConfig
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import fyi.appy.inksend.giladkutiel.data.model.toRenderConfig
import fyi.appy.inksend.giladkutiel.data.repository.SettingsRepository
import fyi.appy.inksend.giladkutiel.engine.ClipboardManagerHelper
import fyi.appy.inksend.giladkutiel.engine.ImageRenderer
import fyi.appy.inksend.giladkutiel.util.TextLengthEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Watches WhatsApp's active editable field (scoped via accessibility_service_config's
 * android:packageNames, so events from other apps never reach this service); when its
 * text length falls within the configured bounds, shows two floating overlay buttons —
 * one per configured style — that render the text into a styled image using whichever
 * style was tapped, copy it to the clipboard, and clear the field.
 */
@AndroidEntryPoint
class TextMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var overlayManager: OverlayWindowManager? = null

    private var currentText: String = ""
    private var activeNode: AccessibilityNodeInfo? = null

    private var currentConfig = TextStyleConfig()
    private var currentSecondaryConfig = SecondaryStyleConfig()

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayWindowManager(
            context = this,
            onPrimaryClicked = { handleStyleButtonPressed(currentConfig) },
            onSecondaryClicked = { handleStyleButtonPressed(currentSecondaryConfig.toRenderConfig(currentConfig)) },
        )

        serviceScope.launch {
            settingsRepository.styleConfigFlow.collect { config ->
                currentConfig = config
                evaluateOverlayVisibility(currentText)
            }
        }
        serviceScope.launch {
            settingsRepository.secondaryStyleConfigFlow.collect { config ->
                currentSecondaryConfig = config
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            -> {
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

    private fun evaluateOverlayVisibility(text: String) {
        val withinBounds = TextLengthEvaluator.isWithinBounds(
            trimmedLength = text.trim().length,
            minLength = currentConfig.minTextLength,
            maxLength = currentConfig.maxTextLength,
        )
        if (withinBounds) {
            overlayManager?.showOverlay()
        } else {
            overlayManager?.hideOverlay()
        }
    }

    private fun handleStyleButtonPressed(renderConfig: TextStyleConfig) {
        if (currentText.isBlank()) return

        val imageUri = ImageRenderer.generateStyledImageUri(this, currentText, renderConfig)
        ClipboardManagerHelper.copyImageToClipboard(this, imageUri)

        activeNode?.let { node ->
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        currentText = ""
        overlayManager?.hideOverlay()
        Toast.makeText(this, getString(R.string.toast_image_copied), Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {
        overlayManager?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayManager?.hideOverlay()
    }
}
