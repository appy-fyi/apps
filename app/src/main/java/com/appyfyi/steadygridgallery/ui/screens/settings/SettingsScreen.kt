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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.BuildConfig
import com.appyfyi.steadygridgallery.R
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import com.appyfyi.steadygridgallery.data.prefs.ThemeMode
import com.appyfyi.steadygridgallery.ui.common.AppLanguage
import com.appyfyi.steadygridgallery.ui.common.AppLocale

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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                        text = uiState.errorMessage ?: stringResource(R.string.settings_error_fallback),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                SettingsPhase.POPULATED -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SettingsSection(title = stringResource(R.string.settings_section_display)) {
                        SettingsRow(label = stringResource(R.string.settings_sort_order_label)) {
                            SortMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.defaultSort == mode,
                                    onClick = { viewModel.setDefaultSort(mode) },
                                    label = { Text(stringResource(mode.labelRes())) },
                                )
                            }
                        }
                        SettingsRow(label = stringResource(R.string.settings_grid_column_size_label)) {
                            listOf(96, 128, 160).forEach { dp ->
                                FilterChip(
                                    selected = uiState.settings.gridCellDp == dp,
                                    onClick = { viewModel.setGridCellDp(dp) },
                                    label = { Text(stringResource(R.string.settings_grid_cell_size_format, dp)) },
                                )
                            }
                        }
                        SettingsRow(label = stringResource(R.string.settings_theme_label)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.settings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(stringResource(mode.labelRes())) },
                                )
                            }
                        }
                        val context = LocalContext.current
                        var currentLanguage by remember { mutableStateOf(AppLocale.current(context)) }
                        SettingsRow(label = stringResource(R.string.settings_language_label), isLast = true) {
                            AppLanguage.entries.forEach { language ->
                                FilterChip(
                                    selected = currentLanguage == language,
                                    onClick = {
                                        currentLanguage = language
                                        AppLocale.set(context, language)
                                    },
                                    label = { Text(stringResource(language.labelRes)) },
                                )
                            }
                        }
                    }

                    SettingsSection(title = stringResource(R.string.settings_section_privacy)) {
                        Row(modifier = Modifier.padding(bottom = 12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                            )
                            Text(
                                stringResource(R.string.settings_privacy_local_only),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            stringResource(R.string.settings_privacy_target_sdk),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    SettingsSection(title = stringResource(R.string.settings_section_security)) {
                        OutlinedButton(onClick = onManageHiddenLock, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_manage_hidden_lock))
                        }
                        if (uiState.hiddenPhotosUnlocked) {
                            Button(
                                onClick = viewModel::hideAllNow,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) {
                                Text(stringResource(R.string.settings_hide_all_now))
                            }
                        }
                        Button(
                            onClick = onReviewMediaPermission,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.settings_review_media_permission))
                        }
                    }

                    Text(
                        text = stringResource(R.string.settings_version_format, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

private fun SortMode.labelRes(): Int = when (this) {
    SortMode.DATE_DESC -> R.string.sort_mode_date_desc
    SortMode.DATE_ASC -> R.string.sort_mode_date_asc
    SortMode.NAME_ASC -> R.string.sort_mode_name_asc
    SortMode.NAME_DESC -> R.string.sort_mode_name_desc
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
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
