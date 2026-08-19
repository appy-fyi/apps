package com.appyfyi.steadygridgallery.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageHiddenLock: () -> Unit,
    onReviewMediaPermission: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SettingsSection(title = "Display") {
                        SettingsRow(label = "Sort order") {
                            SortMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.defaultSort == mode,
                                    onClick = { viewModel.setDefaultSort(mode) },
                                    label = { Text(mode.name) },
                                )
                            }
                        }
                        SettingsRow(label = "Grid column size") {
                            listOf(96, 128, 160).forEach { dp ->
                                FilterChip(
                                    selected = uiState.settings.gridCellDp == dp,
                                    onClick = { viewModel.setGridCellDp(dp) },
                                    label = { Text("${dp}dp") },
                                )
                            }
                        }
                        SettingsRow(label = "Theme", isLast = true) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.name) },
                                )
                            }
                        }
                    }

                    SettingsSection(title = "Privacy") {
                        Row(modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                            )
                            Text(
                                "Your photos and videos stay on this device. There is no account or cloud sync.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            "Built for current Android media permissions. Target Android SDK: 35.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SettingsSection(title = "Security & permissions") {
                        OutlinedButton(onClick = onManageHiddenLock, modifier = Modifier.fillMaxWidth()) {
                            Text("Manage hidden lock")
                        }
                        Button(
                            onClick = onReviewMediaPermission,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text("Review media permission")
                        }
                    }

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsRow(
    label: String,
    isLast: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
