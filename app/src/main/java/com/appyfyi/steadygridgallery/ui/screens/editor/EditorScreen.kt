package com.appyfyi.steadygridgallery.ui.screens.editor

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import com.appyfyi.steadygridgallery.data.media.EditFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Editor") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                EditorPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                EditorPhase.PURCHASE_REQUIRED -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text("The editor is part of Steady Gallery Pro.")
                    Button(onClick = onNavigateToPurchase) { Text("Unlock") }
                }

                EditorPhase.EDITING -> EditingContent(uiState, viewModel)

                EditorPhase.EXPORTING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { uiState.exportProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        )
                        Text("${uiState.exportProgressPercent}%")
                    }
                }

                EditorPhase.EXPORT_SUCCESS -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text("Export complete.")
                    Button(onClick = onBack) { Text("Done") }
                }

                EditorPhase.EXPORT_ERROR -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Export failed.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = viewModel::export) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun EditingContent(uiState: EditorUiState, viewModel: EditorViewModel) {
    val bitmap = uiState.decodedBitmap ?: return
    val cropRect = uiState.cropRectPx ?: Rect(0, 0, bitmap.width, bitmap.height)
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val displayWidthPx = with(density) { maxWidth.toPx() }
            val scale = displayWidthPx / bitmap.width

            Box(modifier = Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height)) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Image being edited",
                    modifier = Modifier.fillMaxSize(),
                )
                CropOverlay(
                    cropRectPx = cropRect,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height,
                    scale = scale,
                    onCropRectChanged = viewModel::setCropRect,
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Button(onClick = viewModel::rotate90) { Text("Rotate 90°") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            EditFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.filter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter.displayName) },
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }

        Button(
            onClick = viewModel::export,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
        ) {
            Text("Export")
        }
    }
}

private const val HANDLE_TOUCH_RADIUS_DP = 16

@Composable
private fun CropOverlay(
    cropRectPx: Rect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    scale: Float,
    onCropRectChanged: (Rect) -> Unit,
) {
    val handleRadiusPx = with(LocalDensity.current) { HANDLE_TOUCH_RADIUS_DP.dp.toPx() }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(cropRectPx, scale) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val pos = change.position
                    val left = cropRectPx.left * scale
                    val top = cropRectPx.top * scale
                    val right = cropRectPx.right * scale
                    val bottom = cropRectPx.bottom * scale

                    val nearTopLeft = distance(pos, Offset(left, top)) < handleRadiusPx * 2
                    val nearBottomRight = distance(pos, Offset(right, bottom)) < handleRadiusPx * 2

                    val newLeft: Float
                    val newTop: Float
                    val newRight: Float
                    val newBottom: Float
                    if (nearTopLeft) {
                        newLeft = (left + dragAmount.x).coerceIn(0f, right - handleRadiusPx)
                        newTop = (top + dragAmount.y).coerceIn(0f, bottom - handleRadiusPx)
                        newRight = right
                        newBottom = bottom
                    } else if (nearBottomRight) {
                        newLeft = left
                        newTop = top
                        newRight = (right + dragAmount.x).coerceIn(left + handleRadiusPx, bitmapWidth * scale)
                        newBottom = (bottom + dragAmount.y).coerceIn(top + handleRadiusPx, bitmapHeight * scale)
                    } else {
                        val width = right - left
                        val height = bottom - top
                        newLeft = (left + dragAmount.x).coerceIn(0f, bitmapWidth * scale - width)
                        newTop = (top + dragAmount.y).coerceIn(0f, bitmapHeight * scale - height)
                        newRight = newLeft + width
                        newBottom = newTop + height
                    }

                    onCropRectChanged(
                        Rect(
                            (newLeft / scale).roundToInt(),
                            (newTop / scale).roundToInt(),
                            (newRight / scale).roundToInt(),
                            (newBottom / scale).roundToInt(),
                        ),
                    )
                }
            },
    ) {
        val left = cropRectPx.left * scale
        val top = cropRectPx.top * scale
        val right = cropRectPx.right * scale
        val bottom = cropRectPx.bottom * scale

        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
        )
        drawCircle(color = Color.White, radius = handleRadiusPx / 2, center = Offset(left, top))
        drawCircle(color = Color.White, radius = handleRadiusPx / 2, center = Offset(right, bottom))
    }
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
