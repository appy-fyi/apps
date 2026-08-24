package fyi.appy.inksend.giladkutiel.ui.styleeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.inksend.giladkutiel.font.BundledFont
import fyi.appy.inksend.giladkutiel.review.InAppReviewHelper
import fyi.appy.inksend.giladkutiel.ui.localAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleEditorScreen(
    styleId: Long?,
    onSaved: () -> Unit,
) {
    val container = localAppContainer()
    val viewModel: StyleEditorViewModel = viewModel(
        key = "style-editor-$styleId",
        factory = viewModelFactory {
            initializer { StyleEditorViewModel(container.styleRepository, styleId) }
        },
    )
    val state by viewModel.uiState.collectAsState()
    val purchased by container.preferencesRepository.purchased.collectAsState(initial = false)
    val handwritingFonts by container.styleRepository.observeHandwritingFonts().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()

    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBackgroundColorPicker by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var fontMenuExpanded by remember { mutableStateOf(false) }

    val draft = viewModel.draft
    val completedFonts = handwritingFonts.filter { it.glyphsCompleted >= 62 }
    val fontOptions = BundledFont.entries.map { it.id to it.displayName } +
        completedFonts.map { it.filePath to it.name }
    val currentFontLabel = fontOptions.firstOrNull { it.first == draft.fontFamily }?.second ?: draft.fontFamily

    fun gatedColorTap(open: () -> Unit) {
        if (purchased) open() else showPurchaseDialog = true
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (styleId == null) "New Style" else "Edit Style") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            // Live preview canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(previewBackgroundBrush(draft.backgroundType, draft.backgroundColorHex, draft.backgroundColorHex2)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Style preview",
                    color = runCatching { Color(android.graphics.Color.parseColor(draft.textColorHex)) }.getOrDefault(Color.Black),
                    textAlign = TextAlign.Center,
                )
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = { name -> viewModel.update { it.copy(name = name) } },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            ExposedDropdownMenuBox(
                expanded = fontMenuExpanded,
                onExpandedChange = { fontMenuExpanded = it },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                OutlinedTextField(
                    value = currentFontLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Font family") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = fontMenuExpanded,
                    onDismissRequest = { fontMenuExpanded = false },
                    modifier = Modifier.exposedDropdownSize(),
                ) {
                    fontOptions.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.update { it.copy(fontFamily = id) }
                                fontMenuExpanded = false
                            },
                        )
                    }
                }
            }

            TextButton(onClick = { gatedColorTap { showTextColorPicker = !showTextColorPicker } }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Text color: ${draft.textColorHex}")
            }
            if (showTextColorPicker) {
                HsvColorPicker(initialHex = draft.textColorHex) { hex ->
                    viewModel.update { it.copy(textColorHex = hex) }
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                SegmentedButton(
                    selected = draft.backgroundType == "solid",
                    onClick = { viewModel.update { it.copy(backgroundType = "solid") } },
                    shape = MaterialTheme.shapes.small,
                ) { Text("Solid") }
                SegmentedButton(
                    selected = draft.backgroundType == "gradient",
                    onClick = { viewModel.update { it.copy(backgroundType = "gradient", backgroundColorHex2 = it.backgroundColorHex2.ifEmpty { "#000000" }) } },
                    shape = MaterialTheme.shapes.small,
                ) { Text("Gradient") }
            }

            TextButton(onClick = { gatedColorTap { showBackgroundColorPicker = !showBackgroundColorPicker } }) {
                Text("Background color: ${draft.backgroundColorHex}" + if (draft.backgroundType == "gradient") " → ${draft.backgroundColorHex2}" else "")
            }
            if (showBackgroundColorPicker) {
                HsvColorPicker(initialHex = draft.backgroundColorHex) { hex ->
                    viewModel.update { it.copy(backgroundColorHex = hex) }
                }
                if (draft.backgroundType == "gradient") {
                    HsvColorPicker(initialHex = draft.backgroundColorHex2.ifEmpty { "#000000" }) { hex ->
                        viewModel.update { it.copy(backgroundColorHex2 = hex) }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Checkbox(checked = draft.isDefault, onCheckedChange = { checked -> viewModel.update { it.copy(isDefault = checked) } })
                Text("Set as default")
            }

            Button(
                onClick = {
                    viewModel.save {
                        activity?.let { InAppReviewHelper.maybeRequestReview(it) }
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) {
                Text("Save")
            }
        }
    }

    if (showPurchaseDialog) {
        PurchasePromptDialog(
            onDismiss = { showPurchaseDialog = false },
            onPurchase = {
                showPurchaseDialog = false
                activity?.let { a -> coroutineScope.launch { container.billingRepository.launchPurchase(a) } }
            },
        )
    }
}

@Composable
private fun PurchasePromptDialog(onDismiss: () -> Unit, onPurchase: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock all styles") },
        text = { Text("Custom colors and the handwriting font creator are part of the one-time $4.99 unlock — no ads, no subscription.") },
        confirmButton = { TextButton(onClick = onPurchase) { Text("Unlock for \$4.99") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

private fun previewBackgroundBrush(type: String, hex1: String, hex2: String): Brush {
    val c1 = runCatching { Color(android.graphics.Color.parseColor(hex1)) }.getOrDefault(Color.Gray)
    return if (type == "gradient" && hex2.isNotEmpty()) {
        val c2 = runCatching { Color(android.graphics.Color.parseColor(hex2)) }.getOrDefault(c1)
        Brush.linearGradient(listOf(c1, c2))
    } else {
        Brush.linearGradient(listOf(c1, c1))
    }
}
