package fyi.appy.inksend.giladkutiel.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.theme.InkSendKeyboardTheme

private val KEY_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

@Composable
fun KeyboardPanelContent(
    hasText: Boolean,
    shiftEnabled: Boolean,
    styles: List<StylePresetEntity>,
    activeStyleId: Long?,
    onKeyTap: (Char) -> Unit,
    onShiftToggle: () -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onStyleSelected: (Long) -> Unit,
    onStyleAndSend: () -> Unit,
) {
    InkSendKeyboardTheme {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                ActionBar(
                    hasText = hasText,
                    styles = styles,
                    activeStyleId = activeStyleId,
                    onStyleSelected = onStyleSelected,
                    onStyleAndSend = onStyleAndSend,
                )
                Spacer(Modifier.height(8.dp))
                KEY_ROWS.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (rowIndex == 2) {
                            KeyButton(modifier = Modifier.weight(1.5f), onClick = onShiftToggle) {
                                Icon(Icons.Filled.KeyboardCapslock, contentDescription = "Shift")
                            }
                        }
                        row.forEach { c ->
                            val display = if (shiftEnabled) c.uppercaseChar() else c
                            KeyButton(modifier = Modifier.weight(1f), onClick = { onKeyTap(display) }) {
                                Text(display.toString())
                            }
                        }
                        if (rowIndex == 2) {
                            KeyButton(modifier = Modifier.weight(1.5f), onClick = onBackspace) {
                                Icon(Icons.Filled.Backspace, contentDescription = "Backspace")
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    KeyButton(modifier = Modifier.weight(5f), onClick = onSpace) { Text("space") }
                    KeyButton(modifier = Modifier.weight(2f), onClick = onEnter) { Text("enter") }
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    hasText: Boolean,
    styles: List<StylePresetEntity>,
    activeStyleId: Long?,
    onStyleSelected: (Long) -> Unit,
    onStyleAndSend: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val activeName = styles.firstOrNull { it.id == activeStyleId }?.name ?: "Default"
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
            ) {
                Text(activeName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onStyleAndSend, enabled = hasText) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Text(" Style & Send", modifier = Modifier.padding(start = 4.dp))
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(styles, key = { it.id }) { style ->
                val bg = runCatching { Color(android.graphics.Color.parseColor(style.backgroundColorHex)) }.getOrDefault(Color.Gray)
                Surface(
                    shape = CircleShape,
                    color = bg,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = if (style.id == activeStyleId) 2.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                        .clickable { onStyleSelected(style.id) },
                ) {}
            }
        }
    }
}

@Composable
private fun KeyButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
