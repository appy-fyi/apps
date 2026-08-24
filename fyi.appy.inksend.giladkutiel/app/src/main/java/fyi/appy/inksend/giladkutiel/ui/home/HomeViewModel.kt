package fyi.appy.inksend.giladkutiel.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.inksend.giladkutiel.data.StyleRepository
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    /** Only the free built-in presets exist yet — no user-created style. */
    data class EmptyNoCustomStyles(val builtInPresets: List<StylePresetEntity>) : HomeUiState
    data class Populated(val presets: List<StylePresetEntity>) : HomeUiState
    data object ErrorFailedToLoadPresets : HomeUiState
}

class HomeViewModel(private val styleRepository: StyleRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = styleRepository.observeStyles()
        .map<List<StylePresetEntity>, HomeUiState> { presets ->
            if (presets.isEmpty()) HomeUiState.Loading
            else if (presets.none { !it.isBuiltIn }) HomeUiState.EmptyNoCustomStyles(presets)
            else HomeUiState.Populated(presets)
        }
        .catch { emit(HomeUiState.ErrorFailedToLoadPresets) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun setDefault(id: Long) {
        viewModelScope.launch { styleRepository.setDefault(id) }
    }
}
