package fyi.appy.taponceremote.giladkutiel.ui.screens.remote

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import fyi.appy.taponceremote.giladkutiel.data.remote.CommandResult
import fyi.appy.taponceremote.giladkutiel.data.remote.ProtocolAdapterFactory
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteProtocolAdapter
import fyi.appy.taponceremote.giladkutiel.data.remote.RokuProtocolAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectionPhase {
    LOADING_DEVICE,
    CONNECTING,
    CONNECTED,
    DEVICE_OFFLINE,
    DEVICE_NOT_FOUND,
}

data class RemoteUiState(
    val phase: ConnectionPhase = ConnectionPhase.LOADING_DEVICE,
    val device: SavedDevice? = null,
    val sendingCommand: RemoteCommand? = null,
    val lastResult: CommandResult? = null,
    val reviewRequested: Boolean = false,
)

class RemoteControlViewModel(
    private val deviceId: Long,
    private val appContext: Context,
    private val savedDeviceDao: SavedDeviceDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    private var adapter: RemoteProtocolAdapter? = null
    private var successfulCommandCount = 0

    init {
        viewModelScope.launch {
            val device = savedDeviceDao.getById(deviceId)
            if (device == null) {
                _uiState.update { it.copy(phase = ConnectionPhase.DEVICE_NOT_FOUND) }
                return@launch
            }
            _uiState.update { it.copy(phase = ConnectionPhase.CONNECTING, device = device) }
            adapter = ProtocolAdapterFactory.create(appContext, device)
            val reachable = probeReachability(device)
            _uiState.update {
                it.copy(phase = if (reachable) ConnectionPhase.CONNECTED else ConnectionPhase.DEVICE_OFFLINE)
            }
        }
    }

    private suspend fun probeReachability(device: SavedDevice): Boolean = when (device.protocol) {
        RemoteProtocol.ROKU_ECP, RemoteProtocol.MANUAL_ROKU_ECP ->
            RokuProtocolAdapter.probeDeviceInfo("http://${device.ipAddress}:${device.port ?: 8060}")
        RemoteProtocol.GOOGLE_CAST -> true
        RemoteProtocol.SSDP_DIAL, RemoteProtocol.IR_PROFILE -> true
    }

    fun sendCommand(command: RemoteCommand, onComplete: (CommandResult) -> Unit = {}) {
        val currentAdapter = adapter ?: return
        if (_uiState.value.phase != ConnectionPhase.CONNECTED) return
        viewModelScope.launch {
            _uiState.update { it.copy(sendingCommand = command) }
            val result = currentAdapter.send(command)
            _uiState.update { it.copy(sendingCommand = null, lastResult = result) }
            if (result is CommandResult.Success) {
                successfulCommandCount++
                if (successfulCommandCount == 1) {
                    _uiState.update { it.copy(reviewRequested = true) }
                }
            }
            onComplete(result)
        }
    }

    fun onReviewRequestHandled() {
        _uiState.update { it.copy(reviewRequested = false) }
    }

    fun protocolAdapter(): RemoteProtocolAdapter? = adapter
}
