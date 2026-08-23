package fyi.appy.taponceremote.giladkutiel.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val savedDeviceDao: SavedDeviceDao) : ViewModel() {
    val savedDevices: StateFlow<List<SavedDevice>> = savedDeviceDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDevice(device: SavedDevice) {
        viewModelScope.launch { savedDeviceDao.delete(device) }
    }
}
