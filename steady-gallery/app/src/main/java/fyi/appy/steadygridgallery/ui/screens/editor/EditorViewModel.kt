package fyi.appy.steadygridgallery.ui.screens.editor

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.imageLoader
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.media.EditFilter
import fyi.appy.steadygridgallery.data.media.ImageEditProcessor
import fyi.appy.steadygridgallery.data.media.ImageExporter
import fyi.appy.steadygridgallery.data.media.MediaItem
import fyi.appy.steadygridgallery.data.media.MediaStoreRepository
import fyi.appy.steadygridgallery.data.prefs.PurchaseEntitlementStore
import fyi.appy.steadygridgallery.ui.common.appContainer
import fyi.appy.steadygridgallery.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class EditorPhase { LOADING, PURCHASE_REQUIRED, EDITING, EXPORTING, EXPORT_SUCCESS, EXPORT_ERROR }

/** Which button triggered the current save flow, so the terminal phases can show the right copy. */
enum class SaveMode { OVERWRITE, COPY }

sealed interface EditorEvent {
    /** Android requires user consent (via this system PendingIntent) to overwrite media this app didn't create. */
    data class ConfirmOverwrite(val pendingIntent: PendingIntent) : EditorEvent
}

private const val EXPORT_TIMEOUT_MILLIS = 60_000L

data class EditorUiState(
    val phase: EditorPhase = EditorPhase.LOADING,
    val sourceItem: MediaItem? = null,
    val decodedBitmap: Bitmap? = null,
    val cropRectPx: Rect? = null,
    val rotationDegrees: Int = 0,
    val filter: EditFilter = EditFilter.ORIGINAL,
    val exportProgressPercent: Int = 0,
    val exportedUri: Uri? = null,
    val errorMessage: String? = null,
    val saveMode: SaveMode? = null,
)

class EditorViewModel(
    private val mediaId: String,
    private val appContext: Context,
    private val mediaRepository: MediaStoreRepository,
    private val entitlementStore: PurchaseEntitlementStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

    private val eventChannel = Channel<EditorEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    // Held between save() requesting write consent and onOverwriteConfirmed() receiving the
    // user's decision, since that round trip goes out to the system UI and back.
    private var pendingOverwriteBitmap: Bitmap? = null

    fun load() {
        if (!entitlementStore.isPurchased.value) {
            _uiState.value = _uiState.value.copy(phase = EditorPhase.PURCHASE_REQUIRED)
            return
        }
        _uiState.value = _uiState.value.copy(phase = EditorPhase.LOADING)
        viewModelScope.launch {
            val item = mediaRepository.findMediaItem(mediaId)
            if (item == null) {
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = appContext.getString(R.string.editor_media_not_found),
                )
                return@launch
            }
            val bitmap = withContext(Dispatchers.IO) {
                ImageEditProcessor.decodeWithExifOrientation(appContext, item.contentUri)
            }
            _uiState.value = _uiState.value.copy(
                phase = EditorPhase.EDITING,
                sourceItem = item,
                decodedBitmap = bitmap,
                cropRectPx = Rect(0, 0, bitmap.width, bitmap.height),
                rotationDegrees = 0,
                filter = EditFilter.ORIGINAL,
                exportProgressPercent = 0,
            )
        }
    }

    fun setCropRect(rect: Rect) {
        _uiState.value = _uiState.value.copy(cropRectPx = rect)
    }

    fun rotateClockwise() {
        val next = (_uiState.value.rotationDegrees + 90) % 360
        _uiState.value = _uiState.value.copy(rotationDegrees = next)
    }

    fun rotateCounterClockwise() {
        val next = (_uiState.value.rotationDegrees + 270) % 360
        _uiState.value = _uiState.value.copy(rotationDegrees = next)
    }

    fun setFilter(filter: EditFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    /** "Save a Copy": writes the edited image to a new file under Pictures/Steady Gallery. */
    fun saveCopy() {
        val state = _uiState.value
        val source = state.sourceItem
        val bitmap = state.decodedBitmap
        val cropRect = state.cropRectPx
        if (source == null || bitmap == null || cropRect == null) return

        _uiState.value = state.copy(
            phase = EditorPhase.EXPORTING,
            exportProgressPercent = 0,
            errorMessage = null,
            saveMode = SaveMode.COPY,
        )

        viewModelScope.launch {
            var pendingUri: Uri? = null
            try {
                withTimeout(EXPORT_TIMEOUT_MILLIS) {
                    val filtered = processBitmap(bitmap, cropRect, state.rotationDegrees, state.filter)
                    reportProgress(75)

                    val uri = withContext(Dispatchers.IO) {
                        ImageExporter.writePending(appContext, filtered, source.mimeType)
                    }
                    pendingUri = uri
                    reportProgress(90)

                    withContext(Dispatchers.IO) { ImageExporter.publish(appContext, uri) }
                    reportProgress(100)

                    _uiState.value = _uiState.value.copy(
                        phase = EditorPhase.EXPORT_SUCCESS,
                        exportedUri = uri,
                    )
                }
            } catch (timeout: TimeoutCancellationException) {
                pendingUri?.let { ImageExporter.deletePending(appContext, it) }
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = appContext.getString(R.string.editor_export_timed_out),
                )
            } catch (t: Throwable) {
                pendingUri?.let { ImageExporter.deletePending(appContext, it) }
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = t.message ?: appContext.getString(R.string.editor_export_failed_fallback),
                )
            }
        }
    }

    /**
     * "Save": overwrites the original photo in place. Android requires explicit user consent to
     * modify a MediaStore entry this app didn't create, so this processes the edit, then asks for
     * that consent via [MediaStore.createWriteRequest] and finishes in [onOverwriteConfirmed]
     * once the system UI reports back.
     */
    fun save() {
        val state = _uiState.value
        val source = state.sourceItem
        val bitmap = state.decodedBitmap
        val cropRect = state.cropRectPx
        if (source == null || bitmap == null || cropRect == null) return

        _uiState.value = state.copy(
            phase = EditorPhase.EXPORTING,
            exportProgressPercent = 0,
            errorMessage = null,
            saveMode = SaveMode.OVERWRITE,
        )

        viewModelScope.launch {
            try {
                withTimeout(EXPORT_TIMEOUT_MILLIS) {
                    val filtered = processBitmap(bitmap, cropRect, state.rotationDegrees, state.filter)
                    reportProgress(75)

                    pendingOverwriteBitmap = filtered
                    val pendingIntent = withContext(Dispatchers.IO) {
                        MediaStore.createWriteRequest(appContext.contentResolver, listOf(source.contentUri))
                    }
                    eventChannel.send(EditorEvent.ConfirmOverwrite(pendingIntent))
                }
            } catch (timeout: TimeoutCancellationException) {
                pendingOverwriteBitmap = null
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = appContext.getString(R.string.editor_export_timed_out),
                )
            } catch (t: Throwable) {
                pendingOverwriteBitmap = null
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = t.message ?: appContext.getString(R.string.editor_export_failed_fallback),
                )
            }
        }
    }

    fun onOverwriteConfirmed(confirmed: Boolean) {
        val bitmap = pendingOverwriteBitmap
        val source = _uiState.value.sourceItem
        pendingOverwriteBitmap = null
        if (!confirmed || bitmap == null || source == null) {
            _uiState.value = _uiState.value.copy(phase = EditorPhase.EDITING)
            return
        }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ImageExporter.overwrite(appContext, source.contentUri, bitmap, source.mimeType)
                }
                // The content URI doesn't change on overwrite, but the bytes behind it do -- Coil
                // caches decoded bitmaps by request URI, so without this, every AsyncImage for
                // this photo (grid thumbnail, viewer, this editor's own reload) would keep
                // serving the pre-edit bitmap out of cache instead of the file we just wrote.
                // Memory cache is cleared outright rather than by key: Coil's default memory
                // cache key can fold in request size/precision, so a single keyed removal isn't
                // guaranteed to catch every cached variant (e.g. grid thumbnail vs full viewer).
                // This only runs on an explicit Save, so evicting the whole (small) memory cache
                // is cheap; the disk cache's key is always just the URI, so a targeted remove
                // there is safe.
                withContext(Dispatchers.IO) {
                    val imageLoader = appContext.imageLoader
                    imageLoader.memoryCache?.clear()
                    imageLoader.diskCache?.remove(source.contentUri.toString())
                }
                reportProgress(100)
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_SUCCESS,
                    exportedUri = source.contentUri,
                )
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = t.message ?: appContext.getString(R.string.editor_export_failed_fallback),
                )
            }
        }
    }

    /** Retries whichever save flow last failed. */
    fun retry() {
        when (_uiState.value.saveMode) {
            SaveMode.OVERWRITE -> save()
            SaveMode.COPY -> saveCopy()
            null -> Unit
        }
    }

    private suspend fun processBitmap(bitmap: Bitmap, cropRect: Rect, rotationDegrees: Int, filter: EditFilter): Bitmap =
        withContext(Dispatchers.Default) {
            reportProgress(5)
            // Source bitmap is already decoded with EXIF orientation applied by load().
            reportProgress(20)

            val cropped = ImageEditProcessor.crop(bitmap, cropRect)
            reportProgress(40)

            val rotated = ImageEditProcessor.rotate(cropped, rotationDegrees)
            reportProgress(60)

            ImageEditProcessor.applyFilter(rotated, filter)
        }

    private fun reportProgress(percent: Int) {
        _uiState.value = _uiState.value.copy(exportProgressPercent = percent)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                val encodedMediaId = savedStateHandle.get<String>(Routes.ARG_MEDIA_ID).orEmpty()
                val container = appContainer()
                EditorViewModel(
                    mediaId = Routes.decode(encodedMediaId),
                    appContext = container.appContext,
                    mediaRepository = container.mediaStoreRepository,
                    entitlementStore = container.purchaseEntitlementStore,
                )
            }
        }
    }
}
