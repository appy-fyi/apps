package fyi.appy.inksend.giladkutiel.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import fyi.appy.inksend.giladkutiel.InkSendApp
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.render.ImageShareHelper
import fyi.appy.inksend.giladkutiel.render.TextImageRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The custom system-wide keyboard. Composing/committed text is tracked in [typedText] — a local
 * buffer synced on every [android.view.inputmethod.InputConnection] call — since
 * `InputConnection.getExtractedText()` alone is unreliable across host apps; this buffer is what
 * [TextImageRenderer] receives as `text` for the one-tap "Style & Send" action.
 */
class InkSendIme : InputMethodService() {
    private val imeLifecycleOwner = ImeLifecycleOwner()
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var typedText by mutableStateOf("")
    private var shiftEnabled by mutableStateOf(false)
    private var styles by mutableStateOf<List<StylePresetEntity>>(emptyList())
    private var activeStyleId by mutableStateOf<Long?>(null)

    override fun onCreate() {
        super.onCreate()
        imeLifecycleOwner.onCreate()
        val container = (application as InkSendApp).container
        serviceScope.launch {
            combine(container.styleRepository.observeStyles(), container.preferencesRepository.defaultStyleId) { s, defaultId ->
                s to defaultId
            }.collect { (s, defaultId) ->
                styles = s
                if (activeStyleId == null || styles.none { it.id == activeStyleId }) {
                    activeStyleId = defaultId ?: s.firstOrNull()?.id
                }
            }
        }
    }

    override fun onCreateInputView(): View {
        // The IME's window is backed by a Dialog, so Compose's window-level recomposer lookup
        // climbs to that Dialog's decor view (the "parentPanel" root) — not just to this
        // ComposeView — to find the ViewTree owners. They must be set there too, or Compose
        // crashes with "ViewTreeLifecycleOwner not found" the moment the keyboard shows.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(imeLifecycleOwner)
            decorView.setViewTreeViewModelStoreOwner(imeLifecycleOwner)
            decorView.setViewTreeSavedStateRegistryOwner(imeLifecycleOwner)
        }

        val composeView = ComposeView(this)
        composeView.setViewTreeLifecycleOwner(imeLifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(imeLifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(imeLifecycleOwner)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        composeView.setContent {
            KeyboardPanelContent(
                hasText = typedText.isNotEmpty(),
                shiftEnabled = shiftEnabled,
                styles = styles,
                activeStyleId = activeStyleId,
                onKeyTap = ::onKeyTap,
                onShiftToggle = { shiftEnabled = !shiftEnabled },
                onBackspace = ::onBackspace,
                onSpace = { commitAndTrack(" ") },
                onEnter = { commitAndTrack("\n") },
                onSwitchKeyboard = ::onSwitchKeyboard,
                onStyleSelected = { id -> activeStyleId = id },
                onStyleAndSend = ::onStyleAndSend,
            )
        }
        return composeView
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        imeLifecycleOwner.onResume()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        imeLifecycleOwner.onPause()
        // Deliberately not clearing typedText here: switching away from InkSend's keyboard and
        // back must preserve the last-typed, not-yet-sent text (see the Guided Setup feature's
        // acceptance criteria).
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        imeLifecycleOwner.onDestroy()
    }

    private fun onKeyTap(char: Char) {
        commitAndTrack(char.toString())
    }

    private fun onBackspace() {
        // Always forward to the InputConnection: the local typedText buffer can be out of sync
        // with the field's real content (e.g. pre-existing text, or a fresh IME instance after
        // the system recreated the service), and gating on it made backspace silently no-op in
        // those cases even though there was visibly text to delete.
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (typedText.isNotEmpty()) {
            typedText = typedText.dropLast(1)
        }
    }

    private fun commitAndTrack(text: String) {
        currentInputConnection?.commitText(text, 1)
        typedText += text
    }

    private fun onSwitchKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun onStyleAndSend() {
        if (typedText.isEmpty()) return
        val style = styles.firstOrNull { it.id == activeStyleId } ?: styles.firstOrNull() ?: return
        val bitmap = TextImageRenderer.render(this, typedText, style)
        val uri = ImageShareHelper.saveToCache(this, bitmap)
        val intent = ImageShareHelper.buildSendIntent(this, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
