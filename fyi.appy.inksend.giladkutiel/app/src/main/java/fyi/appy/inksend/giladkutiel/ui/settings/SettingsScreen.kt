package fyi.appy.inksend.giladkutiel.ui.settings

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import fyi.appy.inksend.giladkutiel.R
import fyi.appy.inksend.giladkutiel.data.model.FontChoice
import fyi.appy.inksend.giladkutiel.data.model.StyleConfig
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
    val styles by viewModel.stylesState.collectAsState()
    val trigger by viewModel.triggerState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
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

            LanguagePicker()

            Text(stringResource(R.string.text_length_triggers_title), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trigger.minTextLength.toString(),
                    onValueChange = { raw ->
                        val value = raw.toIntOrNull() ?: return@OutlinedTextField
                        viewModel.updateTriggerConfig(trigger.copy(minTextLength = value))
                    },
                    label = { Text(stringResource(R.string.min_length_label)) },
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = trigger.maxTextLength.toString(),
                    onValueChange = { raw ->
                        val value = raw.toIntOrNull() ?: return@OutlinedTextField
                        viewModel.updateTriggerConfig(trigger.copy(maxTextLength = value))
                    },
                    label = { Text(stringResource(R.string.max_length_label)) },
                    modifier = Modifier.weight(1f),
                )
            }

            styles.forEachIndexed { index, style ->
                StyleSection(
                    index = index,
                    style = style,
                    canRemove = styles.size > 1,
                    onUpdate = { viewModel.updateStyle(it) },
                    onRemove = { viewModel.removeStyle(style.id) },
                )
            }

            Button(onClick = { viewModel.addStyle() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.add_style_button))
            }
        }
    }
}

/** One style's full set of editable controls, plus its own remove button when more than one style exists. */
@Composable
private fun StyleSection(
    index: Int,
    style: StyleConfig,
    canRemove: Boolean,
    onUpdate: (StyleConfig) -> Unit,
    onRemove: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.style_section_title_format, index + 1),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (canRemove) {
                    Button(onClick = onRemove) { Text(stringResource(R.string.remove_style_button)) }
                }
            }
            LivePreview(style)
            FontPicker(
                selected = style.font,
                onSelect = { onUpdate(style.copy(font = it)) },
            )
            EmojiPicker(
                selected = style.emoji,
                onSelect = { onUpdate(style.copy(emoji = it)) },
            )
            ColorPicker(
                label = stringResource(R.string.text_color_label),
                selected = style.textColorHex,
                onSelect = { onUpdate(style.copy(textColorHex = it)) },
            )
            ColorPicker(
                label = stringResource(R.string.background_color_label),
                selected = style.backgroundColorHex,
                onSelect = { onUpdate(style.copy(backgroundColorHex = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.gradient_toggle_label))
                Box(modifier = Modifier.weight(1f))
                Switch(
                    checked = style.isGradientEnabled,
                    onCheckedChange = { onUpdate(style.copy(isGradientEnabled = it)) },
                )
            }
            if (style.isGradientEnabled) {
                ColorPicker(
                    label = stringResource(R.string.gradient_end_color_label),
                    selected = style.gradientEndColorHex,
                    onSelect = { onUpdate(style.copy(gradientEndColorHex = it)) },
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
            Text(stringResource(R.string.permissions_card_title), style = MaterialTheme.typography.titleMedium)
            Spacer()
            PermissionRow(
                granted = overlayGranted,
                label = stringResource(R.string.permission_overlay_label),
                onClick = onRequestOverlayPermission,
            )
            Spacer()
            PermissionRow(
                granted = accessibilityGranted,
                label = stringResource(R.string.permission_accessibility_label),
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
            Text(if (granted) stringResource(R.string.permission_granted_format, label) else label)
        }
        if (!granted) {
            Button(onClick = onClick) { Text(stringResource(R.string.permission_open_button)) }
        }
    }
}

@Composable
private fun Spacer() = Box(modifier = Modifier.height(8.dp))

/** Language options for the in-app language override: null means "follow the system language". */
private data class LanguageOption(val tag: String?, val nativeName: String)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption(null, ""),
    LanguageOption("en", "English"),
    LanguageOption("es", "Español"),
    LanguageOption("fr", "Français"),
    LanguageOption("de", "Deutsch"),
    LanguageOption("pt", "Português"),
    LanguageOption("hi", "हिन्दी"),
)

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Reads the app's current per-app language override, preferring the platform LocaleManager
 * (API 33+) since that's the source of truth the OS itself uses; falls back to AppCompat's
 * compat storage on older devices.
 */
private fun currentLanguageTag(context: android.content.Context): String? =
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(android.app.LocaleManager::class.java)
            ?.applicationLocales
            ?.takeIf { !it.isEmpty }
            ?.get(0)
            ?.language
    } else {
        AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').takeIf { it.isNotBlank() }
    }

private fun applyLanguageTag(context: android.content.Context, tag: String?) {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        context.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales =
            if (tag == null) android.os.LocaleList.getEmptyLocaleList() else android.os.LocaleList.forLanguageTags(tag)
    } else {
        AppCompatDelegate.setApplicationLocales(
            if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }
    context.findActivity()?.recreate()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagePicker() {
    val context = LocalContext.current
    val currentTag = currentLanguageTag(context)

    Column {
        Text(stringResource(R.string.app_language_title), style = MaterialTheme.typography.titleMedium)
        Spacer()
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LANGUAGE_OPTIONS.forEach { option ->
                val isSelected = if (option.tag == null) currentTag == null else currentTag == option.tag
                Button(
                    onClick = { applyLanguageTag(context, option.tag) },
                    colors = if (isSelected) {
                        androidx.compose.material3.ButtonDefaults.buttonColors()
                    } else {
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(if (option.tag == null) stringResource(R.string.language_system_default) else option.nativeName)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontPicker(selected: FontChoice, onSelect: (FontChoice) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                Text(stringResource(choice.labelRes))
            }
        }
    }
}

/** A small, popular, and category-diverse set of emoji offered as badge choices. */
private val EMOJI_BADGE_CHOICES = listOf(
    "✨", "🎉", "❤️", "🔥", "😂", "👍", "🙌", "🎨",
    "🌈", "⭐", "💯", "🎵", "🍀", "🌸", "📌", "🚀", "🏆",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(stringResource(R.string.emoji_badge_label), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.emoji_badge_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            EmojiChoiceChip(
                content = { Box(modifier = Modifier.size(22.dp)) },
                isSelected = selected.isBlank(),
                onClick = { onSelect("") },
                backgroundColor = Color.White,
            )
            EMOJI_BADGE_CHOICES.forEach { emoji ->
                EmojiChoiceChip(
                    content = { Text(emoji, fontSize = 22.sp) },
                    isSelected = selected == emoji,
                    onClick = { onSelect(emoji) },
                )
            }
        }
    }
}

@Composable
private fun EmojiChoiceChip(
    content: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color? = null,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                backgroundColor
                    ?: if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A small, predefined palette of colors offered for text/background/gradient color choices. */
private val COLOR_PALETTE = listOf(
    "#FFFFFF", "#000000", "#1E1E2E", "#F5E9DA",
    "#89B4FA", "#F7B267", "#5B47E0", "#D64545",
    "#4CAF7D", "#2DB6A3", "#E85D9E", "#9B59D0",
    "#F2C94C", "#9AA0A6", "#1B2A4A", "#C9B8FF",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPicker(label: String, selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            COLOR_PALETTE.forEach { hex ->
                ColorSwatch(
                    hex = hex,
                    isSelected = selected.equals(hex, ignoreCase = true),
                    onClick = { onSelect(hex) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = colorOrFallback(hex)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.color_swatch_selected_description),
                tint = if (isLightColor(color)) Color.Black else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.6f
}

@Composable
private fun LivePreview(config: StyleConfig) {
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
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = stringResource(R.string.preview_content_description))
        } else {
            Text(stringResource(R.string.preview_unavailable))
        }
    }
}

private fun colorOrFallback(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: IllegalArgumentException) {
        Color.Gray
    }
