package fyi.appy.steadygridgallery.ui.screens.hiddenunlock

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.prefs.HiddenUnlockSession
import fyi.appy.steadygridgallery.data.prefs.LockCredentialStore
import fyi.appy.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class HiddenUnlockPhase { NO_LOCK_CONFIGURED, LOCKED, AUTHENTICATING, AUTH_ERROR, UNLOCKED }

data class HiddenUnlockUiState(
    val phase: HiddenUnlockPhase = HiddenUnlockPhase.LOCKED,
    val biometricEnabled: Boolean = false,
    val errorMessage: String? = null,
)

class HiddenUnlockViewModel(
    private val lockCredentialStore: LockCredentialStore,
    private val session: HiddenUnlockSession,
    private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HiddenUnlockUiState())
    val uiState: StateFlow<HiddenUnlockUiState> = _uiState

    fun load() {
        if (session.isUnlocked.value) {
            // Still within the 5-minute in-memory grace period: no need to re-authenticate.
            unlock(lockCredentialStore.isBiometricEnabled())
            return
        }
        _uiState.value = _uiState.value.copy(
            phase = if (lockCredentialStore.hasCredential()) {
                HiddenUnlockPhase.LOCKED
            } else {
                HiddenUnlockPhase.NO_LOCK_CONFIGURED
            },
            biometricEnabled = lockCredentialStore.isBiometricEnabled(),
        )
    }

    fun setUpPin(pin: String, confirmPin: String, enableBiometric: Boolean) {
        if (pin.length !in 4..12 || pin.any { !it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                phase = HiddenUnlockPhase.NO_LOCK_CONFIGURED,
                errorMessage = appContext.getString(R.string.hidden_unlock_pin_length_error),
            )
            return
        }
        if (pin != confirmPin) {
            _uiState.value = _uiState.value.copy(
                phase = HiddenUnlockPhase.NO_LOCK_CONFIGURED,
                errorMessage = appContext.getString(R.string.hidden_unlock_pin_mismatch_error),
            )
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                lockCredentialStore.createCredential(pin, enableBiometric)
            }
            unlock(enableBiometric)
        }
    }

    fun submitPin(pin: String) {
        _uiState.value = _uiState.value.copy(phase = HiddenUnlockPhase.AUTHENTICATING, errorMessage = null)
        viewModelScope.launch {
            val correct = withContext(Dispatchers.Default) { lockCredentialStore.verifyPin(pin) }
            if (correct) {
                unlock(_uiState.value.biometricEnabled)
            } else {
                _uiState.value = _uiState.value.copy(
                    phase = HiddenUnlockPhase.AUTH_ERROR,
                    errorMessage = appContext.getString(R.string.hidden_unlock_incorrect_pin_fallback),
                )
            }
        }
    }

    fun onBiometricSuccess() {
        unlock(_uiState.value.biometricEnabled)
    }

    fun onBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(phase = HiddenUnlockPhase.AUTH_ERROR, errorMessage = message)
    }

    private fun unlock(biometricEnabled: Boolean) {
        session.unlock()
        _uiState.value = _uiState.value.copy(
            phase = HiddenUnlockPhase.UNLOCKED,
            biometricEnabled = biometricEnabled,
            errorMessage = null,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HiddenUnlockViewModel(
                    lockCredentialStore = container.lockCredentialStore,
                    session = container.hiddenUnlockSession,
                    appContext = container.appContext,
                )
            }
        }
    }
}
