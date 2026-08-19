package com.appyfyi.steadygridgallery.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.appyfyi.steadygridgallery.data.media.EditFilter
import com.appyfyi.steadygridgallery.data.media.ImageEditProcessor
import com.appyfyi.steadygridgallery.data.media.ImageExporter
import com.appyfyi.steadygridgallery.data.media.MediaItem
import com.appyfyi.steadygridgallery.data.media.MediaStoreRepository
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import com.appyfyi.steadygridgallery.ui.common.appContainer
import com.appyfyi.steadygridgallery.ui.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.roundToInt

enum class EditorPhase { LOADING, PURCHASE_REQUIRED, EDITING, EXPORTING, EXPORT_SUCCESS, EXPORT_ERROR }

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
)

class EditorViewModel(
    private val mediaId: String,
    private val appContext: Context,
    private val mediaRepository: MediaStoreRepository,
    private val entitlementStore: PurchaseEntitlementStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState

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
                    errorMessage = "Media not found",
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

    /** Centers a 3:4 portrait crop box, sized to the largest that fits the source bitmap. */
    fun applyVerticalCrop() = applyAspectCrop(widthRatio = 3f, heightRatio = 4f)

    /** Centers a 4:3 landscape crop box, sized to the largest that fits the source bitmap. */
    fun applyHorizontalCrop() = applyAspectCrop(widthRatio = 4f, heightRatio = 3f)

    private fun applyAspectCrop(widthRatio: Float, heightRatio: Float) {
        val bitmap = _uiState.value.decodedBitmap ?: return
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val targetRatio = widthRatio / heightRatio
        val currentRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        if (currentRatio > targetRatio) {
            cropHeight = bitmapHeight
            cropWidth = (bitmapHeight * targetRatio).roundToInt().coerceIn(1, bitmapWidth)
        } else {
            cropWidth = bitmapWidth
            cropHeight = (bitmapWidth / targetRatio).roundToInt().coerceIn(1, bitmapHeight)
        }
        val left = (bitmapWidth - cropWidth) / 2
        val top = (bitmapHeight - cropHeight) / 2
        setCropRect(Rect(left, top, left + cropWidth, top + cropHeight))
    }

    fun rotate90() {
        val next = (_uiState.value.rotationDegrees + 90) % 360
        _uiState.value = _uiState.value.copy(rotationDegrees = next)
    }

    fun setFilter(filter: EditFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun export() {
        val state = _uiState.value
        val source = state.sourceItem
        val bitmap = state.decodedBitmap
        val cropRect = state.cropRectPx
        if (source == null || bitmap == null || cropRect == null) return

        _uiState.value = state.copy(phase = EditorPhase.EXPORTING, exportProgressPercent = 0, errorMessage = null)

        viewModelScope.launch {
            var pendingUri: Uri? = null
            try {
                withTimeout(EXPORT_TIMEOUT_MILLIS) {
                    withContext(Dispatchers.Default) {
                        reportProgress(5)
                        // Source bitmap is already decoded with EXIF orientation applied by load().
                        reportProgress(20)

                        val cropped = ImageEditProcessor.crop(bitmap, cropRect)
                        reportProgress(40)

                        val rotated = ImageEditProcessor.rotate(cropped, state.rotationDegrees)
                        reportProgress(60)

                        val filtered = ImageEditProcessor.applyFilter(rotated, state.filter)
                        reportProgress(75)

                        val uri = ImageExporter.writePending(appContext, filtered, source.mimeType)
                        pendingUri = uri
                        reportProgress(90)

                        ImageExporter.publish(appContext, uri)
                        reportProgress(100)

                        _uiState.value = _uiState.value.copy(
                            phase = EditorPhase.EXPORT_SUCCESS,
                            exportedUri = uri,
                        )
                    }
                }
            } catch (timeout: TimeoutCancellationException) {
                pendingUri?.let { ImageExporter.deletePending(appContext, it) }
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = "Export timed out",
                )
            } catch (t: Throwable) {
                pendingUri?.let { ImageExporter.deletePending(appContext, it) }
                _uiState.value = _uiState.value.copy(
                    phase = EditorPhase.EXPORT_ERROR,
                    errorMessage = t.message ?: "Export failed",
                )
            }
        }
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
