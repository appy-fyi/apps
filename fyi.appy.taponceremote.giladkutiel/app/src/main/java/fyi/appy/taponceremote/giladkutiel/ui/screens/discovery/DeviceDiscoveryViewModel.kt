package fyi.appy.taponceremote.giladkutiel.ui.screens.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import fyi.appy.taponceremote.giladkutiel.data.discovery.DiscoveredDevice
import fyi.appy.taponceremote.giladkutiel.data.discovery.DiscoveryRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DiscoveryScreenState {
    LOADING_SAVED_DEVICES,
    SCANNING,
    EMPTY_NO_SAVED_OR_DISCOVERED,
    POPULATED_SAVED_ONLY,
    POPULATED_DISCOVERED,
    NETWORK_ERROR,
    MANUAL_IP_VALIDATION_ERROR,
}

data class DiscoveryUiState(
    val isLoadingSaved: Boolean = true,
    val isScanning: Boolean = false,
    val hasScannedOnce: Boolean = false,
    val savedDevices: List<SavedDevice> = emptyList(),
    val discovered: List<DiscoveredDevice> = emptyList(),
    val networkError: Boolean = false,
    val manualIpError: String? = null,
) {
    val screenState: DiscoveryScreenState
        get() = when {
            isLoadingSaved -> DiscoveryScreenState.LOADING_SAVED_DEVICES
            manualIpError != null -> DiscoveryScreenState.MANUAL_IP_VALIDATION_ERROR
            networkError -> DiscoveryScreenState.NETWORK_ERROR
            isScanning -> DiscoveryScreenState.SCANNING
            discovered.isNotEmpty() -> DiscoveryScreenState.POPULATED_DISCOVERED
            savedDevices.isNotEmpty() -> DiscoveryScreenState.POPULATED_SAVED_ONLY
            hasScannedOnce -> DiscoveryScreenState.EMPTY_NO_SAVED_OR_DISCOVERED
            else -> DiscoveryScreenState.SCANNING
        }
}

class DeviceDiscoveryViewModel(
    private val discoveryRepository: DiscoveryRepository,
    private val savedDeviceDao: SavedDeviceDao,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private val _navigateToRemote = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val navigateToRemote: SharedFlow<Long> = _navigateToRemote.asSharedFlow()

    init {
        viewModelScope.launch {
            savedDeviceDao.observeAll().collect { saved ->
                _uiState.update { it.copy(savedDevices = saved, isLoadingSaved = false) }
            }
        }
        scan()
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, networkError = false) }
            try {
                val discovered = discoveryRepository.scan()
                _uiState.update { it.copy(discovered = discovered, isScanning = false, hasScannedOnce = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanning = false, hasScannedOnce = true, networkError = true) }
            }
        }
    }

    fun connectDiscovered(device: DiscoveredDevice) {
        viewModelScope.launch {
            val id = savedDeviceDao.insert(
                SavedDevice(
                    displayName = device.displayName,
                    protocol = device.protocol,
                    ipAddress = device.ipAddress,
                    port = device.port,
                    castDeviceId = device.castDeviceId,
                    lastSeenAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            savedDeviceDao.markAsLastUsed(id, System.currentTimeMillis())
            _navigateToRemote.emit(id)
        }
    }

    fun connectSaved(device: SavedDevice) {
        viewModelScope.launch {
            savedDeviceDao.markAsLastUsed(device.id, System.currentTimeMillis())
            _navigateToRemote.emit(device.id)
        }
    }

    fun submitManualIp(ip: String) {
        viewModelScope.launch {
            val device = discoveryRepository.probeManualIp(ip)
            if (device == null) {
                _uiState.update { it.copy(manualIpError = "Couldn't reach a device at $ip") }
            } else {
                _uiState.update { it.copy(manualIpError = null) }
                connectDiscovered(device)
            }
        }
    }

    fun clearManualIpError() {
        _uiState.update { it.copy(manualIpError = null) }
    }
}
