package fyi.appy.inksend.giladkutiel.ui.settings

import android.app.Activity
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import fyi.appy.inksend.giladkutiel.R
import fyi.appy.inksend.giladkutiel.data.model.Intent
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

            AutoStyleSection()
        }
    }
}

/**
 * Explains that the look is now chosen automatically from what the user types, and shows a
 * scrollable gallery of one sample render per [Intent] so the range is visible at a glance.
 */
@Composable
private fun AutoStyleSection() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.auto_style_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.auto_style_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Intent.entries.forEach { intent -> IntentPreview(intent) }
            }
        }
    }
}

/** One labelled sample render for [intent], using its first look and the shared sample text. */
@Composable
private fun IntentPreview(intent: Intent) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(120.dp),
    ) {
        StylePreviewImage(intent.styles.first(), Modifier.size(120.dp))
        Text(
            intentLabel(intent),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StylePreviewImage(config: StyleConfig, modifier: Modifier = Modifier) {
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

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.preview_content_description),
            )
        } else {
            Text(stringResource(R.string.preview_unavailable))
        }
    }
}

@Composable
private fun intentLabel(intent: Intent): String = when (intent) {
    Intent.FUNNY -> stringResource(R.string.intent_funny)
    Intent.SAD -> stringResource(R.string.intent_sad)
    Intent.ROMANTIC -> stringResource(R.string.intent_romantic)
    Intent.ANGRY -> stringResource(R.string.intent_angry)
    Intent.INFORMATIVE -> stringResource(R.string.intent_informative)
    Intent.EXCITED -> stringResource(R.string.intent_excited)
    Intent.CELEBRATORY -> stringResource(R.string.intent_celebratory)
    Intent.CALM -> stringResource(R.string.intent_calm)
    Intent.MOTIVATIONAL -> stringResource(R.string.intent_motivational)
    Intent.GRATEFUL -> stringResource(R.string.intent_grateful)
    Intent.NEUTRAL -> stringResource(R.string.intent_neutral)
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
