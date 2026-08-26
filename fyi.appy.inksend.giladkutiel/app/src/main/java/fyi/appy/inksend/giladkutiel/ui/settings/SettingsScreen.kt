package fyi.appy.inksend.giladkutiel.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fyi.appy.inksend.giladkutiel.data.model.FontChoice
import fyi.appy.inksend.giladkutiel.data.model.SecondaryStyleConfig
import fyi.appy.inksend.giladkutiel.data.model.TextStyleConfig
import fyi.appy.inksend.giladkutiel.data.model.toRenderConfig
import fyi.appy.inksend.giladkutiel.engine.ImageRenderer
import kotlinx.coroutines.Dispatchers

private const val PREVIEW_TEXT = "Styled Text Preview"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
) {
    val config by viewModel.uiState.collectAsState()
    val secondaryConfig by viewModel.secondaryUiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Text-to-Image Settings") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PermissionSection(
                overlayGranted = overlayGranted,
                accessibilityGranted = accessibilityGranted,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission,
            )

            Text("Text Length Triggers", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = config.minTextLength.toString(),
                    onValueChange = { raw ->
                        val value = raw.toIntOrNull() ?: return@OutlinedTextField
                        viewModel.updateConfig(config.copy(minTextLength = value))
                    },
                    label = { Text("Min Length") },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = config.maxTextLength.toString(),
                    onValueChange = { raw ->
                        val value = raw.toIntOrNull() ?: return@OutlinedTextField
                        viewModel.updateConfig(config.copy(maxTextLength = value))
                    },
                    label = { Text("Max Length") },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                "Style 1 — first overlay button",
                style = MaterialTheme.typography.titleMedium,
            )
            LivePreview(config)
            FontPicker(
                selected = config.font,
                onSelect = { viewModel.updateConfig(config.copy(font = it)) },
            )
            HexColorField(
                label = "Text Color (Hex)",
                value = config.textColorHex,
                onValueChange = { viewModel.updateConfig(config.copy(textColorHex = it)) },
            )
            HexColorField(
                label = "Background Color (Hex)",
                value = config.backgroundColorHex,
                onValueChange = { viewModel.updateConfig(config.copy(backgroundColorHex = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Gradient Background")
                Box(modifier = Modifier.weight(1f))
                Switch(
                    checked = config.isGradientEnabled,
                    onCheckedChange = { viewModel.updateConfig(config.copy(isGradientEnabled = it)) },
                )
            }
            if (config.isGradientEnabled) {
                HexColorField(
                    label = "Gradient End Color (Hex)",
                    value = config.gradientEndColorHex,
                    onValueChange = { viewModel.updateConfig(config.copy(gradientEndColorHex = it)) },
                )
            }

            Text(
                "Style 2 — second overlay button",
                style = MaterialTheme.typography.titleMedium,
            )
            LivePreview(secondaryConfig.toRenderConfig(config))
            FontPicker(
                selected = secondaryConfig.font,
                onSelect = { viewModel.updateSecondaryConfig(secondaryConfig.copy(font = it)) },
            )
            HexColorField(
                label = "Text Color (Hex)",
                value = secondaryConfig.textColorHex,
                onValueChange = { viewModel.updateSecondaryConfig(secondaryConfig.copy(textColorHex = it)) },
            )
            HexColorField(
                label = "Background Color (Hex)",
                value = secondaryConfig.backgroundColorHex,
                onValueChange = { viewModel.updateSecondaryConfig(secondaryConfig.copy(backgroundColorHex = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Gradient Background")
                Box(modifier = Modifier.weight(1f))
                Switch(
                    checked = secondaryConfig.isGradientEnabled,
                    onCheckedChange = { viewModel.updateSecondaryConfig(secondaryConfig.copy(isGradientEnabled = it)) },
                )
            }
            if (secondaryConfig.isGradientEnabled) {
                HexColorField(
                    label = "Gradient End Color (Hex)",
                    value = secondaryConfig.gradientEndColorHex,
                    onValueChange = {
                        viewModel.updateSecondaryConfig(secondaryConfig.copy(gradientEndColorHex = it))
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionSection(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Required System Permissions", style = MaterialTheme.typography.titleMedium)
            Spacer()
            PermissionRow(
                granted = overlayGranted,
                label = "1. Grant Overlay Permission",
                onClick = onRequestOverlayPermission,
            )
            Spacer()
            PermissionRow(
                granted = accessibilityGranted,
                label = "2. Enable Accessibility Service",
                onClick = onRequestAccessibilityPermission,
            )
        }
    }
}

@Composable
private fun PermissionRow(granted: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Box(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(if (granted) "$label — Granted" else label)
        }
        if (!granted) {
            Button(onClick = onClick) { Text("Open") }
        }
    }
}

@Composable
private fun Spacer() = Box(modifier = Modifier.height(8.dp))

@Composable
private fun FontPicker(selected: FontChoice, onSelect: (FontChoice) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FontChoice.entries.forEach { choice ->
            val isSelected = choice == selected
            Button(
                onClick = { onSelect(choice) },
                colors = if (isSelected) {
                    androidx.compose.material3.ButtonDefaults.buttonColors()
                } else {
                    androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(choice.label)
            }
        }
    }
}

@Composable
private fun HexColorField(label: String, value: String, onValueChange: (String) -> Unit) {
    val isValid = remember(value) { isValidHexColor(value) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = !isValid,
        supportingText = if (!isValid) {
            { Text("Enter a valid hex color, e.g. #FFAA00") }
        } else {
            null
        },
        trailingIcon = {
            if (isValid) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(color = colorOrFallback(value)),
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LivePreview(config: TextStyleConfig) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(initialValue = null, config) {
        value = try {
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                ImageRenderer.renderBitmap(context, PREVIEW_TEXT, config)
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Style preview")
        } else {
            Text("Preview unavailable")
        }
    }
}

private fun isValidHexColor(hex: String): Boolean =
    try {
        android.graphics.Color.parseColor(hex)
        true
    } catch (_: IllegalArgumentException) {
        false
    }

private fun colorOrFallback(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
