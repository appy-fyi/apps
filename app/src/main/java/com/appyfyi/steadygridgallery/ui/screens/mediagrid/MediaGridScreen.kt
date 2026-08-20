package com.appyfyi.steadygridgallery.ui.screens.mediagrid

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.R
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import com.appyfyi.steadygridgallery.data.media.MediaItem
import com.appyfyi.steadygridgallery.data.media.MediaKind
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGridScreen(
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToHiddenUnlock: () -> Unit,
    viewModel: MediaGridViewModel = viewModel(factory = MediaGridViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var pendingRecycleItemIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var pendingHideItemIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK, pendingRecycleItemIds)
    }

    val hideRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onSystemHideResult(result.resultCode == Activity.RESULT_OK, pendingHideItemIds)
    }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MediaGridEvent.ConfirmSystemDelete -> {
                    pendingRecycleItemIds = event.recycleItemIds
                    deleteRequestLauncher.launch(
                        IntentSenderRequest.Builder(event.pendingIntent.intentSender).build(),
                    )
                }
                is MediaGridEvent.ConfirmSystemHide -> {
                    pendingHideItemIds = event.hiddenItemIds
                    hideRequestLauncher.launch(
                        IntentSenderRequest.Builder(event.pendingIntent.intentSender).build(),
                    )
                }
                MediaGridEvent.RequiresPurchaseToHide -> onNavigateToPurchase()
                MediaGridEvent.RequiresPinSetupToHide -> onNavigateToHiddenUnlock()
                is MediaGridEvent.RecycleFailed -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
                is MediaGridEvent.HideFailed -> scope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState.phase == MediaGridPhase.SELECTION_ACTIVE) {
                TopAppBar(
                    title = { Text(stringResource(R.string.media_grid_selected_count_format, uiState.selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::deleteSelectedToRecycle) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_move_to_recycle_bin))
                        }
                        IconButton(onClick = viewModel::hideSelected) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = stringResource(R.string.media_grid_hide_selected))
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.folderDisplayName.ifBlank { stringResource(R.string.media_grid_folder_fallback_title) },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.common_sort))
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(stringResource(mode.labelRes())) }, onClick = {
                                    sortMenuExpanded = false
                                    viewModel.changeSortMode(mode)
                                })
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                MediaGridPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                MediaGridPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.media_grid_error_fallback),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                MediaGridPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.media_grid_empty))
                }

                MediaGridPhase.POPULATED, MediaGridPhase.SELECTION_ACTIVE -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        items(uiState.items, key = { it.mediaId }) { item ->
                            MediaTile(
                                item = item,
                                selected = item.mediaId in uiState.selectedIds,
                                selectionActive = uiState.phase == MediaGridPhase.SELECTION_ACTIVE,
                                onClick = {
                                    if (uiState.phase == MediaGridPhase.SELECTION_ACTIVE) {
                                        viewModel.toggleSelection(item.mediaId)
                                    } else {
                                        onOpenMedia(item.mediaId)
                                    }
                                },
                                onLongClick = { viewModel.toggleSelection(item.mediaId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(
    item: MediaItem,
    selected: Boolean,
    selectionActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.kind == MediaKind.VIDEO) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.padding(6.dp).align(Alignment.BottomEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.media_video_content_description),
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp).size(16.dp),
                )
            }
        }
        if (selectionActive && selected) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(6.dp).align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.media_selected_content_description),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(2.dp).size(18.dp),
                )
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
