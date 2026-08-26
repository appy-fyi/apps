package fyi.appy.inksend.giladkutiel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import fyi.appy.inksend.giladkutiel.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<TextStyleConfig> = repository.styleConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TextStyleConfig())

    fun updateConfig(newConfig: TextStyleConfig) {
        viewModelScope.launch {
            repository.updateConfig(newConfig)
        }
    }
}
