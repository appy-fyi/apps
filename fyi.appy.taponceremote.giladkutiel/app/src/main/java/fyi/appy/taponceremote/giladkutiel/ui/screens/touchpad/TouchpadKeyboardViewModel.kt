package fyi.appy.taponceremote.giladkutiel.ui.screens.touchpad

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import fyi.appy.taponceremote.giladkutiel.data.remote.CommandResult
import fyi.appy.taponceremote.giladkutiel.data.remote.ProtocolAdapterFactory
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteProtocolAdapter
import fyi.appy.taponceremote.giladkutiel.ui.screens.remote.ConnectionPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GestureState { IDLE, SENDING_GESTURE, SENDING_TEXT, TEXT_UNSUPPORTED, COMMAND_FAILED }

data class TouchpadUiState(
    val phase: ConnectionPhase = ConnectionPhase.LOADING_DEVICE,
    val device: SavedDevice? = null,
    val gestureState: GestureState = GestureState.IDLE,
)

class TouchpadKeyboardViewModel(
    private val deviceId: Long,
    private val appContext: Context,
    private val savedDeviceDao: SavedDeviceDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var adapter: RemoteProtocolAdapter? = null

    init {
        viewModelScope.launch {
            val device = savedDeviceDao.getById(deviceId)
            if (device == null) {
                _uiState.update { it.copy(phase = ConnectionPhase.DEVICE_NOT_FOUND) }
                return@launch
            }
            adapter = ProtocolAdapterFactory.create(appContext, device)
            _uiState.update { it.copy(phase = ConnectionPhase.CONNECTED, device = device) }
        }
    }

    fun sendGesture(command: RemoteCommand) {
        val currentAdapter = adapter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(gestureState = GestureState.SENDING_GESTURE) }
            val result = currentAdapter.send(command)
            _uiState.update {
                it.copy(gestureState = if (result is CommandResult.Failure) GestureState.COMMAND_FAILED else GestureState.IDLE)
            }
        }
    }

    fun sendText(text: String, onHandled: (CommandResult) -> Unit) {
        val currentAdapter = adapter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(gestureState = GestureState.SENDING_TEXT) }
            val result = currentAdapter.sendText(text)
            _uiState.update {
                it.copy(
                    gestureState = when (result) {
                        is CommandResult.TextUnsupported -> GestureState.TEXT_UNSUPPORTED
                        is CommandResult.Failure -> GestureState.COMMAND_FAILED
                        else -> GestureState.IDLE
                    },
                )
            }
            onHandled(result)
        }
    }
}
