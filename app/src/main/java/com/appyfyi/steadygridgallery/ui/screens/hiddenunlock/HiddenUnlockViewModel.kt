package com.appyfyi.steadygridgallery.ui.screens.hiddenunlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.data.db.dao.FolderStateDao
import com.appyfyi.steadygridgallery.data.prefs.HiddenUnlockSession
import com.appyfyi.steadygridgallery.data.prefs.LockCredentialStore
import com.appyfyi.steadygridgallery.ui.common.appContainer
import com.appyfyi.steadygridgallery.ui.navigation.Routes
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
    private val folderStateDao: FolderStateDao,
    private val pendingHideFolderKey: String?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HiddenUnlockUiState())
    val uiState: StateFlow<HiddenUnlockUiState> = _uiState

    fun load() {
        if (session.isUnlocked.value) {
            // Still within the 5-minute in-memory grace period: no need to re-authenticate.
            viewModelScope.launch { applyPendingHideAndUnlock(lockCredentialStore.isBiometricEnabled()) }
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
                errorMessage = "PIN must be 4 to 12 digits",
            )
            return
        }
        if (pin != confirmPin) {
            _uiState.value = _uiState.value.copy(
                phase = HiddenUnlockPhase.NO_LOCK_CONFIGURED,
                errorMessage = "PINs do not match",
            )
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                lockCredentialStore.createCredential(pin, enableBiometric)
            }
            applyPendingHideAndUnlock(enableBiometric)
        }
    }

    fun submitPin(pin: String) {
        _uiState.value = _uiState.value.copy(phase = HiddenUnlockPhase.AUTHENTICATING, errorMessage = null)
        viewModelScope.launch {
            val correct = withContext(Dispatchers.Default) { lockCredentialStore.verifyPin(pin) }
            if (correct) {
                applyPendingHideAndUnlock(_uiState.value.biometricEnabled)
            } else {
                _uiState.value = _uiState.value.copy(
                    phase = HiddenUnlockPhase.AUTH_ERROR,
                    errorMessage = "Incorrect PIN",
                )
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch { applyPendingHideAndUnlock(_uiState.value.biometricEnabled) }
    }

    fun onBiometricError(message: String) {
        _uiState.value = _uiState.value.copy(phase = HiddenUnlockPhase.AUTH_ERROR, errorMessage = message)
    }

    private suspend fun applyPendingHideAndUnlock(biometricEnabled: Boolean) {
        if (!pendingHideFolderKey.isNullOrEmpty()) {
            folderStateDao.setHidden(pendingHideFolderKey, true)
        }
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
                val savedStateHandle = createSavedStateHandle()
                val encodedPendingKey = savedStateHandle.get<String>(Routes.ARG_PENDING_HIDE_FOLDER_KEY)
                val pendingKey = encodedPendingKey?.takeIf { it.isNotEmpty() }?.let { Routes.decode(it) }
                val container = appContainer()
                HiddenUnlockViewModel(
                    lockCredentialStore = container.lockCredentialStore,
                    session = container.hiddenUnlockSession,
                    folderStateDao = container.database.folderStateDao(),
                    pendingHideFolderKey = pendingKey,
                )
            }
        }
    }
}
