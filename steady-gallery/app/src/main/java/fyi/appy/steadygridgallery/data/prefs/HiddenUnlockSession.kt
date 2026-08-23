package fyi.appy.steadygridgallery.data.prefs

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val BACKGROUND_TIMEOUT_MILLIS = 5 * 60 * 1000L

/**
 * In-memory-only unlocked flag for hidden folders: it never touches disk, and it self-expires
 * after 5 minutes in the background or on process death (a fresh process starts locked).
 */
class HiddenUnlockSession {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    private var backgroundedAtMillis: Long? = null

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun onAppBackgrounded() {
        backgroundedAtMillis = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val backgroundedAt = backgroundedAtMillis
        if (backgroundedAt != null && System.currentTimeMillis() - backgroundedAt > BACKGROUND_TIMEOUT_MILLIS) {
            _isUnlocked.value = false
        }
        backgroundedAtMillis = null
    }
}
