package fyi.appy.inksend.giladkutiel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fyi.appy.inksend.giladkutiel.data.model.DEFAULT_STYLES
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
import fyi.appy.inksend.giladkutiel.data.model.TriggerConfig
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

    val stylesState: StateFlow<List<StyleConfig>> = repository.stylesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_STYLES)

    val triggerState: StateFlow<TriggerConfig> = repository.triggerConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TriggerConfig())

    fun updateStyle(updated: StyleConfig) {
        viewModelScope.launch {
            repository.updateStyles(stylesState.value.map { if (it.id == updated.id) updated else it })
        }
    }

    fun addStyle() {
        viewModelScope.launch {
            repository.updateStyles(stylesState.value + StyleConfig())
        }
    }

    /** No-ops if [id] is the last remaining style — the overlay always needs at least one. */
    fun removeStyle(id: String) {
        viewModelScope.launch {
            val remaining = stylesState.value.filterNot { it.id == id }
            if (remaining.isNotEmpty()) {
                repository.updateStyles(remaining)
            }
        }
    }

    fun updateTriggerConfig(config: TriggerConfig) {
        viewModelScope.launch {
            repository.updateTriggerConfig(config)
        }
    }
}
