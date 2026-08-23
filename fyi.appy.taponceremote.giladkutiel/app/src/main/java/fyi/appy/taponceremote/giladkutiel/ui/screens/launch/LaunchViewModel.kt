package fyi.appy.taponceremote.giladkutiel.ui.screens.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDeviceDao
import fyi.appy.taponceremote.giladkutiel.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** The app is fully free with no ads and no purchases — this screen just routes past a brief load. */
class LaunchViewModel(private val savedDeviceDao: SavedDeviceDao) : ViewModel() {
    private val _navigateTo = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateTo: SharedFlow<String> = _navigateTo.asSharedFlow()

    init {
        viewModelScope.launch {
            val lastUsed = savedDeviceDao.getLastUsed()
            val route = if (lastUsed != null) Routes.remote(lastUsed.id) else Routes.DEVICES
            _navigateTo.emit(route)
        }
    }
}
