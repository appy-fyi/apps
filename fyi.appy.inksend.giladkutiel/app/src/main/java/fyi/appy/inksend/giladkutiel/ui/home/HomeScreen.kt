package fyi.appy.inksend.giladkutiel.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.ime.KeyboardStatus
import fyi.appy.inksend.giladkutiel.ui.localAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewStyle: () -> Unit,
    onEditStyle: (Long) -> Unit,
    onOpenKeyboardSetup: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHandwritingCreator: () -> Unit,
) {
    val container = localAppContainer()
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.styleRepository) } },
    )
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()
    val purchased by container.preferencesRepository.purchased.collectAsState(initial = false)
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var keyboardConfigured by remember { mutableStateOf(KeyboardStatus.isSelected(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                keyboardConfigured = KeyboardStatus.isSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val gatedOpenHandwritingCreator: () -> Unit = {
        if (purchased) onOpenHandwritingCreator() else showPurchaseDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("InkSend") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewStyle) {
                Icon(Icons.Filled.Add, contentDescription = "New Style")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (!keyboardConfigured) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable(onClick = onOpenKeyboardSetup),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "Enable Keyboard to start styling text anywhere",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            when (val s = state) {
                is HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is HomeUiState.ErrorFailedToLoadPresets -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Couldn't load your styles. Please restart the app.", textAlign = TextAlign.Center)
                }

                is HomeUiState.EmptyNoCustomStyles -> StyleGrid(
                    presets = s.builtInPresets,
                    onEditStyle = onEditStyle,
                    onSetDefault = viewModel::setDefault,
                    onOpenHandwritingCreator = gatedOpenHandwritingCreator,
                )

                is HomeUiState.Populated -> StyleGrid(
                    presets = s.presets,
                    onEditStyle = onEditStyle,
                    onSetDefault = viewModel::setDefault,
                    onOpenHandwritingCreator = gatedOpenHandwritingCreator,
                )
            }
        }
    }

    if (showPurchaseDialog) {
        AlertDialog(
            onDismissRequest = { showPurchaseDialog = false },
            title = { Text("Unlock all styles") },
            text = { Text("The handwriting font creator is part of the one-time \$4.99 unlock — no ads, no subscription.") },
            confirmButton = {
                TextButton(onClick = {
                    showPurchaseDialog = false
                    activity?.let { a -> coroutineScope.launch { container.billingRepository.launchPurchase(a) } }
                }) { Text("Unlock for \$4.99") }
            },
            dismissButton = { TextButton(onClick = { showPurchaseDialog = false }) { Text("Not now") } },
        )
    }
}

@Composable
private fun StyleGrid(
    presets: List<StylePresetEntity>,
    onEditStyle: (Long) -> Unit,
    onSetDefault: (Long) -> Unit,
    onOpenHandwritingCreator: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(presets, key = { it.id }) { preset ->
            StylePresetCard(preset = preset, onClick = { onEditStyle(preset.id) }, onSetDefault = { onSetDefault(preset.id) })
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Card(
                onClick = onOpenHandwritingCreator,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Create your own handwriting font",
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StylePresetCard(preset: StylePresetEntity, onClick: () -> Unit, onSetDefault: () -> Unit) {
    Card(onClick = onClick) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(runCatching { Color(android.graphics.Color.parseColor(preset.backgroundColorHex)) }.getOrDefault(Color.Gray)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                preset.name,
                color = runCatching { Color(android.graphics.Color.parseColor(preset.textColorHex)) }.getOrDefault(Color.White),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )
            if (preset.isDefault) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        "Default",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                IconButton(onClick = onSetDefault, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Outlined.StarBorder, contentDescription = "Set as default")
                }
            }
        }
    }
}
