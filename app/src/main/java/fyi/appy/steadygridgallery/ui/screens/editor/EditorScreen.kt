package fyi.appy.steadygridgallery.ui.screens.editor

import android.app.Activity
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.media.EditFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val overwriteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onOverwriteConfirmed(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.ConfirmOverwrite ->
                    overwriteRequestLauncher.launch(IntentSenderRequest.Builder(event.pendingIntent.intentSender).build())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.editor_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                EditorPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                EditorPhase.PURCHASE_REQUIRED -> EditorMessageState(
                    icon = Icons.Filled.WorkspacePremium,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    title = stringResource(R.string.editor_unlock_title),
                    message = stringResource(R.string.editor_unlock_message),
                ) {
                    Button(onClick = onNavigateToPurchase) { Text(stringResource(R.string.common_unlock)) }
                }

                EditorPhase.EDITING -> EditingContent(uiState, viewModel)

                EditorPhase.EXPORTING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.editor_exporting), style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { uiState.exportProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        )
                        Text(
                            stringResource(R.string.editor_export_percent_format, uiState.exportProgressPercent),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                EditorPhase.EXPORT_SUCCESS -> EditorMessageState(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    title = stringResource(
                        if (uiState.saveMode == SaveMode.OVERWRITE) {
                            R.string.editor_save_success_title
                        } else {
                            R.string.editor_export_success_title
                        },
                    ),
                    message = stringResource(
                        if (uiState.saveMode == SaveMode.OVERWRITE) {
                            R.string.editor_save_success_message
                        } else {
                            R.string.editor_export_success_message
                        },
                    ),
                ) {
                    Button(onClick = onBack) { Text(stringResource(R.string.common_done)) }
                }

                EditorPhase.EXPORT_ERROR -> EditorMessageState(
                    icon = Icons.Filled.ErrorOutline,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    title = stringResource(R.string.editor_export_failed_title),
                    message = uiState.errorMessage ?: stringResource(R.string.editor_export_error_fallback),
                ) {
                    Button(onClick = viewModel::retry) { Text(stringResource(R.string.common_retry)) }
                }
            }
        }
    }
}

@Composable
private fun EditorMessageState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    message: String,
    action: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = CircleShape, color = iconContainerColor) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(20.dp).size(40.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        action()
    }
}

@Composable
private fun EditingContent(uiState: EditorUiState, viewModel: EditorViewModel) {
    val bitmap = uiState.decodedBitmap ?: return
    val cropRect = uiState.cropRectPx ?: Rect(0, 0, bitmap.width, bitmap.height)
    val density = LocalDensity.current
    val rotationDegrees = uiState.rotationDegrees

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            // Horizontal padding is wider than vertical: a full-width crop handle sits right in
            // Android's edge back-gesture zone, which can swallow the drag before this screen
            // ever sees it (systemGestureExclusionRects on CropOverlay covers most of that, but
            // OEMs can widen the back-gesture sensitivity past what's excludable, so keep a
            // deliberate safety margin too).
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 48.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Fit the bitmap within BOTH available dimensions (like ContentScale.Fit), not just
            // width -- a width-only fit lets tall/portrait images overflow past this weighted
            // Box's bounded height and draw over the Rotate/filter/Export controls below it.
            // The fit also accounts for the live rotation preview: a 90/270 rotation swaps which
            // bitmap dimension maps to on-screen width vs height.
            val rotatedQuarterTurn = rotationDegrees % 180 != 0
            val effectiveBitmapWidth = if (rotatedQuarterTurn) bitmap.height else bitmap.width
            val effectiveBitmapHeight = if (rotatedQuarterTurn) bitmap.width else bitmap.height
            val maxWidthPx = with(density) { maxWidth.toPx() }
            val maxHeightPx = with(density) { maxHeight.toPx() }
            val scale = minOf(maxWidthPx / effectiveBitmapWidth, maxHeightPx / effectiveBitmapHeight)
            val displayWidth = with(density) { (bitmap.width * scale).toDp() }
            val displayHeight = with(density) { (bitmap.height * scale).toDp() }

            Box(
                modifier = Modifier
                    .size(displayWidth, displayHeight)
                    .graphicsLayer(rotationZ = rotationDegrees.toFloat()),
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.editor_image_being_edited),
                    colorFilter = previewColorFilter(uiState.filter),
                    modifier = Modifier.fillMaxSize(),
                )
                CropOverlay(
                    cropRectPx = cropRect,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    scale = scale,
                    onCropRectChanged = viewModel::setCropRect,
                    // Extend the overlay's hit-test region beyond the image bounds by the handle
                    // touch radius: a handle at the crop box's default (full-image) extent sits
                    // exactly on the image edge, which is also this Canvas's own boundary, so a
                    // touch landing exactly on that edge can miss the hit-test region entirely.
                    // Padding it out gives every handle real touchable area on every side.
                    // requiredSize (not size) is essential here: this Box's parent already
                    // constrains children tightly to displayWidth x displayHeight, and plain
                    // size() coerces its request within incoming constraints -- it would get
                    // silently clamped back down instead of actually enlarging the hit area.
                    // align(Center) -- rather than a manually computed negative offset -- lets
                    // Box's own alignment math center the oversized overlay, so the padding
                    // comes out symmetric on all four sides without hand-rolled offset math.
                    modifier = Modifier
                        .align(Alignment.Center)
                        .requiredSize(
                            displayWidth + HANDLE_TOUCH_RADIUS_DP.dp * 2,
                            displayHeight + HANDLE_TOUCH_RADIUS_DP.dp * 2,
                        ),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = viewModel::rotateCounterClockwise) {
                Icon(
                    Icons.Filled.RotateLeft,
                    contentDescription = stringResource(R.string.editor_rotate_counterclockwise),
                )
            }
            IconButton(onClick = viewModel::rotateClockwise) {
                Icon(
                    Icons.Filled.RotateRight,
                    contentDescription = stringResource(R.string.editor_rotate_clockwise),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            EditFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(stringResource(filter.labelRes())) },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = viewModel::saveCopy,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.editor_save_copy_button))
            }
            Button(
                onClick = viewModel::save,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.editor_save_button))
            }
        }
    }
}

private fun EditFilter.labelRes(): Int = when (this) {
    EditFilter.ORIGINAL -> R.string.edit_filter_original
    EditFilter.GRAYSCALE -> R.string.edit_filter_grayscale
    EditFilter.SEPIA -> R.string.edit_filter_sepia
    EditFilter.HIGH_CONTRAST -> R.string.edit_filter_high_contrast
}

/** Mirrors ImageEditProcessor's color matrices so the live preview matches what Export produces. */
private fun previewColorFilter(filter: EditFilter): ColorFilter? = when (filter) {
    EditFilter.ORIGINAL -> null
    EditFilter.GRAYSCALE -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    EditFilter.SEPIA -> ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
    EditFilter.HIGH_CONTRAST -> {
        val contrast = 1.6f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            ),
        )
    }
}

private const val HANDLE_TOUCH_RADIUS_DP = 16

/** Which part of the crop box a drag gesture is manipulating, fixed for the gesture's duration. */
private enum class CropHandle { TOP_LEFT, TOP, TOP_RIGHT, RIGHT, BOTTOM_RIGHT, BOTTOM, BOTTOM_LEFT, LEFT, MOVE }

@Composable
private fun CropOverlay(
    cropRectPx: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    scale: Float,
    onCropRectChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val handleRadiusPx = with(LocalDensity.current) { HANDLE_TOUCH_RADIUS_DP.dp.toPx() }
    // This Canvas is padded handleRadiusPx beyond the image on every side (see call site), so
    // every bitmap-space coordinate is shifted by this offset to land in the padded canvas's
    // local space -- image-relative (0, 0) is at canvas-local (handleRadiusPx, handleRadiusPx).
    val originOffset = handleRadiusPx
    // Read the live rect/callback via rememberUpdatedState instead of keying pointerInput on
    // cropRectPx: onCropRectChanged fires on every drag delta, so keying on it would restart
    // this pointerInput coroutine mid-gesture and cancel detectDragGestures after one event
    // (the drag would start, then appear to get stuck).
    val currentCropRectPx by rememberUpdatedState(cropRectPx)
    val currentOnCropRectChanged by rememberUpdatedState(onCropRectChanged)

    // On gesture-nav devices, an edge/corner handle can sit within ~24dp of the screen edge
    // (e.g. a wide image's crop box spans nearly full width). Without this, Android's
    // back-gesture recognizer swallows that first touch/drag before this Canvas ever sees it,
    // so the handle appears completely unresponsive right at the point it's needed most.
    val view = LocalView.current
    // Guard against redundant writes: setting systemGestureExclusionRects triggers a window
    // insets pass, and onGloballyPositioned re-fires after that pass completes. Writing on every
    // callback regardless of whether the bounds actually changed risks a relayout loop.
    var lastExclusionBounds by remember { mutableStateOf<Rect?>(null) }
    DisposableEffect(view) {
        onDispose { view.systemGestureExclusionRects = emptyList() }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val rect = Rect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                )
                if (rect != lastExclusionBounds) {
                    lastExclusionBounds = rect
                    view.systemGestureExclusionRects = listOf(rect)
                }
            }
            .pointerInput(scale, bitmapWidth, bitmapHeight) {
                // The active handle is picked once in onDragStart and reused for every delta in
                // the gesture. Re-picking it on every move (the previous approach) let the choice
                // flip mid-drag as the box resized, which read as the drag "getting stuck" or
                // jumping.
                var activeHandle: CropHandle? = null

                detectDragGestures(
                    onDragStart = { pos ->
                        val rect = currentCropRectPx
                        val left = rect.left * scale + originOffset
                        val top = rect.top * scale + originOffset
                        val right = rect.right * scale + originOffset
                        val bottom = rect.bottom * scale + originOffset
                        val midX = (left + right) / 2f
                        val midY = (top + bottom) / 2f
                        val threshold = handleRadiusPx * 2

                        val candidates = listOf(
                            CropHandle.TOP_LEFT to Offset(left, top),
                            CropHandle.TOP to Offset(midX, top),
                            CropHandle.TOP_RIGHT to Offset(right, top),
                            CropHandle.RIGHT to Offset(right, midY),
                            CropHandle.BOTTOM_RIGHT to Offset(right, bottom),
                            CropHandle.BOTTOM to Offset(midX, bottom),
                            CropHandle.BOTTOM_LEFT to Offset(left, bottom),
                            CropHandle.LEFT to Offset(left, midY),
                        )
                        val nearest = candidates.minByOrNull { (_, handlePos) -> distance(pos, handlePos) }
                        activeHandle = when {
                            nearest != null && distance(pos, nearest.second) < threshold -> nearest.first
                            pos.x in left..right && pos.y in top..bottom -> CropHandle.MOVE
                            else -> null
                        }
                    },
                ) { change, dragAmount ->
                    val handle = activeHandle ?: return@detectDragGestures
                    change.consume()
                    val rect = currentCropRectPx
                    val left = rect.left * scale + originOffset
                    val top = rect.top * scale + originOffset
                    val right = rect.right * scale + originOffset
                    val bottom = rect.bottom * scale + originOffset
                    val minX = originOffset
                    val minY = originOffset
                    val maxWidthPx = bitmapWidth * scale + originOffset
                    val maxHeightPx = bitmapHeight * scale + originOffset

                    var newLeft = left
                    var newTop = top
                    var newRight = right
                    var newBottom = bottom

                    when (handle) {
                        CropHandle.TOP_LEFT -> {
                            newLeft = (left + dragAmount.x).coerceIn(minX, right - handleRadiusPx)
                            newTop = (top + dragAmount.y).coerceIn(minY, bottom - handleRadiusPx)
                        }
                        CropHandle.TOP_RIGHT -> {
                            newRight = (right + dragAmount.x).coerceIn(left + handleRadiusPx, maxWidthPx)
                            newTop = (top + dragAmount.y).coerceIn(minY, bottom - handleRadiusPx)
                        }
                        CropHandle.BOTTOM_LEFT -> {
                            newLeft = (left + dragAmount.x).coerceIn(minX, right - handleRadiusPx)
                            newBottom = (bottom + dragAmount.y).coerceIn(top + handleRadiusPx, maxHeightPx)
                        }
                        CropHandle.BOTTOM_RIGHT -> {
                            newRight = (right + dragAmount.x).coerceIn(left + handleRadiusPx, maxWidthPx)
                            newBottom = (bottom + dragAmount.y).coerceIn(top + handleRadiusPx, maxHeightPx)
                        }
                        CropHandle.TOP -> newTop = (top + dragAmount.y).coerceIn(minY, bottom - handleRadiusPx)
                        CropHandle.BOTTOM -> newBottom = (bottom + dragAmount.y).coerceIn(top + handleRadiusPx, maxHeightPx)
                        CropHandle.LEFT -> newLeft = (left + dragAmount.x).coerceIn(minX, right - handleRadiusPx)
                        CropHandle.RIGHT -> newRight = (right + dragAmount.x).coerceIn(left + handleRadiusPx, maxWidthPx)
                        CropHandle.MOVE -> {
                            val width = right - left
                            val height = bottom - top
                            newLeft = (left + dragAmount.x).coerceIn(minX, maxWidthPx - width)
                            newTop = (top + dragAmount.y).coerceIn(minY, maxHeightPx - height)
                            newRight = newLeft + width
                            newBottom = newTop + height
                        }
                    }

                    currentOnCropRectChanged(
                        Rect(
                            ((newLeft - originOffset) / scale).roundToInt(),
                            ((newTop - originOffset) / scale).roundToInt(),
                            ((newRight - originOffset) / scale).roundToInt(),
                            ((newBottom - originOffset) / scale).roundToInt(),
                        ),
                    )
                }
            },
    ) {
        val left = cropRectPx.left * scale + originOffset
        val top = cropRectPx.top * scale + originOffset
        val right = cropRectPx.right * scale + originOffset
        val bottom = cropRectPx.bottom * scale + originOffset
        val midX = (left + right) / 2f
        val midY = (top + bottom) / 2f

        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        val handlePositions = listOf(
            Offset(left, top),
            Offset(midX, top),
            Offset(right, top),
            Offset(right, midY),
            Offset(right, bottom),
            Offset(midX, bottom),
            Offset(left, bottom),
            Offset(left, midY),
        )
        handlePositions.forEach { center ->
            drawCircle(color = Color.White, radius = handleRadiusPx / 2, center = center)
        }
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
