package fyi.appy.steadygridgallery.ui.screens.viewer

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.media.MediaItem
import fyi.appy.steadygridgallery.data.media.MediaKind
import fyi.appy.steadygridgallery.data.media.MediaStoreRepository
import fyi.appy.steadygridgallery.data.recycle.RecycleRepository
import fyi.appy.steadygridgallery.ui.common.appContainer
import fyi.appy.steadygridgallery.ui.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class ViewerPhase { LOADING, DISPLAYING_IMAGE, DISPLAYING_VIDEO, ERROR, DELETED_TO_RECYCLE }

data class ViewerUiState(
    val phase: ViewerPhase = ViewerPhase.LOADING,
    val currentItem: MediaItem? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ViewerEvent {
    data class ConfirmSystemDelete(val pendingIntent: PendingIntent, val recycleItemId: Long) : ViewerEvent
    data class RecycleFailed(val message: String) : ViewerEvent
}

class ViewerViewModel(
    private val initialMediaId: String,
    private val mediaRepository: MediaStoreRepository,
    private val recycleRepository: RecycleRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewerUiState())
    val uiState: StateFlow<ViewerUiState> = _uiState

    private val eventChannel = Channel<ViewerEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var siblings: List<MediaItem> = emptyList()
    private var currentIndex: Int = -1

    fun load() {
        _uiState.value = _uiState.value.copy(phase = ViewerPhase.LOADING, errorMessage = null)
        viewModelScope.launch {
            val item = mediaRepository.findMediaItem(initialMediaId)
            if (item == null) {
                _uiState.value = _uiState.value.copy(
                    phase = ViewerPhase.ERROR,
                    errorMessage = appContext.getString(R.string.viewer_media_not_found),
                )
                return@launch
            }
            mediaRepository.loadMediaInFolder(item.folderKey)
                .onSuccess { items ->
                    siblings = items
                    currentIndex = items.indexOfFirst { it.mediaId == item.mediaId }.coerceAtLeast(0)
                    showCurrent()
                }
                .onFailure {
                    siblings = listOf(item)
                    currentIndex = 0
                    showCurrent()
                }
        }
    }

    private fun showCurrent() {
        val item = siblings.getOrNull(currentIndex) ?: return
        _uiState.value = _uiState.value.copy(
            phase = if (item.kind == MediaKind.IMAGE) ViewerPhase.DISPLAYING_IMAGE else ViewerPhase.DISPLAYING_VIDEO,
            currentItem = item,
            hasPrevious = currentIndex > 0,
            hasNext = currentIndex < siblings.size - 1,
        )
    }

    fun showNext() {
        if (currentIndex < siblings.size - 1) {
            currentIndex += 1
            showCurrent()
        }
    }

    fun showPrevious() {
        if (currentIndex > 0) {
            currentIndex -= 1
            showCurrent()
        }
    }

    fun moveCurrentToRecycle() {
        val item = _uiState.value.currentItem ?: return
        viewModelScope.launch {
            recycleRepository.copyAndVerify(item)
                .onSuccess { recycleItemId ->
                    val pendingIntent = recycleRepository.buildDeleteRequest(listOf(item.contentUri))
                    eventChannel.send(ViewerEvent.ConfirmSystemDelete(pendingIntent, recycleItemId))
                }
                .onFailure {
                    eventChannel.send(
                        ViewerEvent.RecycleFailed(
                            it.message ?: appContext.getString(R.string.viewer_recycle_failed_format, item.displayName),
                        ),
                    )
                }
        }
    }

    fun onSystemDeleteResult(confirmed: Boolean, recycleItemId: Long) {
        viewModelScope.launch {
            if (confirmed) {
                recycleRepository.markRecycled(recycleItemId)
                _uiState.value = _uiState.value.copy(phase = ViewerPhase.DELETED_TO_RECYCLE)
            }
            // Cancelled: row stays COPIED_PENDING_SYSTEM_DELETE, original media is untouched.
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val encodedMediaId = savedStateHandle.get<String>(Routes.ARG_MEDIA_ID).orEmpty()
                val container = appContainer()
                ViewerViewModel(
                    initialMediaId = Routes.decode(encodedMediaId),
                    mediaRepository = container.mediaStoreRepository,
                    recycleRepository = container.recycleRepository,
                    appContext = container.appContext,
                )
            }
        }
    }
}
