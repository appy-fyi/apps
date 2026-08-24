package fyi.appy.inksend.giladkutiel.ui.styleeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** An inline hue-slider + saturation/value square HSV picker — no third-party color-picker library. */
@Composable
fun HsvColorPicker(initialHex: String, onColorChanged: (String) -> Unit) {
    val initialColor = runCatching { android.graphics.Color.parseColor(initialHex) }.getOrDefault(android.graphics.Color.BLACK)
    val hsv = remember {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, out)
        out
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var hexInput by remember { mutableStateOf(initialHex) }

    fun emit() {
        val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        hexInput = String.format("#%06X", 0xFFFFFF and color)
        onColorChanged(hexInput)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Color")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(vertical = 8.dp)
                .pointerInput(hue) {
                    detectDragGestures { change, _ ->
                        sat = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                }
                .pointerInput(hue) {
                    detectTapGestures { offset ->
                        sat = (offset.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        emit()
                    }
                },
        ) {
            val hueColor = Color.hsv(hue, 1f, 1f)
            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.White, hueColor)),
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(vertical = 4.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        hue = ((change.position.x / size.width) * 360f).coerceIn(0f, 360f)
                        emit()
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = ((offset.x / size.width) * 360f).coerceIn(0f, 360f)
                        emit()
                    }
                },
        ) {
            val hueColors = (0..360 step 30).map { Color.hsv(it.toFloat().coerceAtMost(359f), 1f, 1f) }
            drawRect(brush = Brush.horizontalGradient(hueColors))
        }

        OutlinedTextField(
            value = hexInput,
            onValueChange = { text ->
                hexInput = text
                val parsed = runCatching { android.graphics.Color.parseColor(text) }.getOrNull()
                if (parsed != null) {
                    val out = FloatArray(3)
                    android.graphics.Color.colorToHSV(parsed, out)
                    hue = out[0]; sat = out[1]; value = out[2]
                    onColorChanged(text)
                }
            },
            label = { Text("Hex") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
