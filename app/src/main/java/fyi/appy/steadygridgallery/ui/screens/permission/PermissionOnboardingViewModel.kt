package fyi.appy.steadygridgallery.ui.screens.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PermissionOnboardingState {
    CHECKING_PERMISSION,
    NEEDS_PERMISSION,
    PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED,
    READY,
}

class PermissionOnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(PermissionOnboardingState.CHECKING_PERMISSION)
    val state: StateFlow<PermissionOnboardingState> = _state

    fun onInitialCheck(granted: Boolean) {
        _state.value = if (granted) PermissionOnboardingState.READY else PermissionOnboardingState.NEEDS_PERMISSION
    }

    fun onPermissionResult(granted: Boolean, canShowRationale: Boolean) {
        _state.value = when {
            granted -> PermissionOnboardingState.READY
            canShowRationale -> PermissionOnboardingState.PERMISSION_DENIED
            else -> PermissionOnboardingState.PERMISSION_PERMANENTLY_DENIED
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { PermissionOnboardingViewModel() }
        }
    }
}
