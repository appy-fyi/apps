package com.appyfyi.steadygridgallery.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.BuildConfig
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import com.appyfyi.steadygridgallery.data.prefs.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onManageHiddenLock: () -> Unit,
    onReviewMediaPermission: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                SettingsPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                SettingsPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: "Unable to load settings.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                SettingsPhase.POPULATED -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                ) {
                    Text("Sort order", style = MaterialTheme.typography.titleMedium)
                    Row {
                        SortMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.settings.defaultSort == mode,
                                onClick = { viewModel.setDefaultSort(mode) },
                                label = { Text(mode.name) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Grid column size", style = MaterialTheme.typography.titleMedium)
                    Row {
                        listOf(96, 128, 160).forEach { dp ->
                            FilterChip(
                                selected = uiState.settings.gridCellDp == dp,
                                onClick = { viewModel.setGridCellDp(dp) },
                                label = { Text("${dp}dp") },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Row {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(mode.name) },
                                modifier = Modifier.padding(end = 4.dp),
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    Text(
                        "Your photos and videos stay on this device. There is no account or cloud sync.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Built for current Android media permissions. Target Android SDK: 35.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                    OutlinedButton(onClick = onManageHiddenLock, modifier = Modifier.fillMaxWidth()) {
                        Text("Manage hidden lock")
                    }
                    Button(
                        onClick = onReviewMediaPermission,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text("Review media permission")
                    }

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
    }
}
