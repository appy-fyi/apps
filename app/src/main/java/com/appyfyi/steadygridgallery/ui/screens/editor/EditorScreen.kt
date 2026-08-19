package com.appyfyi.steadygridgallery.ui.screens.editor

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                    title = "Unlock the Editor",
                    message = "Crop, rotate, filters, and export are part of Steady Gallery Pro.",
                ) {
                    Button(onClick = onNavigateToPurchase) { Text("Unlock") }
                }

                EditorPhase.EDITING -> EditingContent(uiState, viewModel)

                EditorPhase.EXPORTING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Exporting…", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(
                            progress = { uiState.exportProgressPercent / 100f },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        )
                        Text(
                            "${uiState.exportProgressPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                EditorPhase.EXPORT_SUCCESS -> EditorMessageState(
                    icon = Icons.Filled.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    title = "Export complete",
                    message = "Saved to Pictures/Steady Gallery.",
                ) {
                    Button(onClick = onBack) { Text("Done") }
                }

                EditorPhase.EXPORT_ERROR -> EditorMessageState(
                    icon = Icons.Filled.ErrorOutline,
                    iconTint = MaterialTheme.colorScheme.error,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    title = "Export failed",
                    message = uiState.errorMessage ?: "Something went wrong while exporting.",
                ) {
                    Button(onClick = viewModel::export) { Text("Retry") }
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
