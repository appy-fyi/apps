package fyi.appy.steadygridgallery.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.db.entity.SortMode
import fyi.appy.steadygridgallery.data.prefs.AppSettings
import fyi.appy.steadygridgallery.data.prefs.HiddenUnlockSession
import fyi.appy.steadygridgallery.data.prefs.SettingsRepository
import fyi.appy.steadygridgallery.data.prefs.ThemeMode
import fyi.appy.steadygridgallery.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SettingsPhase { LOADING, POPULATED, ERROR }

data class SettingsUiState(
    val phase: SettingsPhase = SettingsPhase.LOADING,
    val settings: AppSettings = AppSettings(),
    val hiddenPhotosUnlocked: Boolean = false,
    val errorMessage: String? = null,
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val hiddenUnlockSession: HiddenUnlockSession,
    private val appContext: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            hiddenUnlockSession.isUnlocked.collect { unlocked ->
                _uiState.value = _uiState.value.copy(hiddenPhotosUnlocked = unlocked)
            }
        }
        viewModelScope.launch {
            runCatching {
                repository.settings.collect { settings ->
                    _uiState.value = _uiState.value.copy(phase = SettingsPhase.POPULATED, settings = settings)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    phase = SettingsPhase.ERROR,
                    errorMessage = error.message ?: appContext.getString(R.string.settings_load_error_fallback),
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }

    fun setGridCellDp(dp: Int) = viewModelScope.launch { repository.setGridCellDp(dp) }

    fun setDefaultSort(sortMode: SortMode) = viewModelScope.launch { repository.setDefaultSort(sortMode) }

    /** Global panic switch: immediately re-locks hidden photos without waiting for the background timeout. */
    fun hideAllNow() = hiddenUnlockSession.lock()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                SettingsViewModel(container.settingsRepository, container.hiddenUnlockSession, container.appContext)
            }
        }
    }
}
