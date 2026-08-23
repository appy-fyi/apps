package fyi.appy.taponceremote.giladkutiel.ui.screens.ir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import fyi.appy.taponceremote.giladkutiel.data.ir.IrProfile
import fyi.appy.taponceremote.giladkutiel.data.ir.IrProfileRepository
import fyi.appy.taponceremote.giladkutiel.data.ir.IrTransmitter
import fyi.appy.taponceremote.giladkutiel.data.ir.NecWaveformGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface IrState {
    data object CheckingHardware : IrState
    data object NoHardware : IrState
    data class Ready(
        val profiles: List<IrProfile> = emptyList(),
        val selected: IrProfile? = null,
        val transmitting: Boolean = false,
        val error: String? = null,
        val saved: Boolean = false,
    ) : IrState
}

class IrFallbackViewModel(
    private val irTransmitter: IrTransmitter,
    private val irProfileRepository: IrProfileRepository,
    private val savedDeviceDao: SavedDeviceDao,
) : ViewModel() {
    private val _state = MutableStateFlow<IrState>(IrState.CheckingHardware)
    val state: StateFlow<IrState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (!irTransmitter.hasIrEmitter()) {
                _state.value = IrState.NoHardware
            } else {
                val profiles = irProfileRepository.loadProfiles()
                _state.value = IrState.Ready(profiles = profiles)
            }
        }
    }

    fun selectProfile(profile: IrProfile) {
        val current = _state.value as? IrState.Ready ?: return
        _state.value = current.copy(selected = profile, error = null, saved = false)
    }

    fun sendCommand(commandKey: String) {
        val current = _state.value as? IrState.Ready ?: return
        val profile = current.selected ?: return
        val code = profile.commands[commandKey] ?: return
        viewModelScope.launch {
            _state.value = current.copy(transmitting = true, error = null)
            try {
                val pattern = NecWaveformGenerator.generate(code)
                irTransmitter.transmit(profile.carrierFrequencyHz, pattern)
                _state.value = (_state.value as IrState.Ready).copy(transmitting = false)
            } catch (e: Exception) {
                _state.value = (_state.value as IrState.Ready).copy(
                    transmitting = false,
                    error = e.message ?: "transmit failed",
                )
            }
        }
    }

    fun saveProfile() {
        val current = _state.value as? IrState.Ready ?: return
        val profile = current.selected ?: return
        viewModelScope.launch {
            savedDeviceDao.insert(
                SavedDevice(
                    displayName = profile.name,
                    protocol = RemoteProtocol.IR_PROFILE,
                    irProfileName = profile.name,
                    lastSeenAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            _state.value = current.copy(saved = true)
        }
    }
}
