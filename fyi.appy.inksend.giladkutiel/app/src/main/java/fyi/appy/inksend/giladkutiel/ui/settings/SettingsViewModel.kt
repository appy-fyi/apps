package fyi.appy.inksend.giladkutiel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val triggerState: StateFlow<TriggerConfig> = repository.triggerConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TriggerConfig())

    fun updateTriggerConfig(config: TriggerConfig) {
        viewModelScope.launch {
            repository.updateTriggerConfig(config)
        }
    }
}
