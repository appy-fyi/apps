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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import com.appyfyi.steadygridgallery.data.media.MediaItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaGridScreen(
    onBack: () -> Unit,
    onOpenMedia: (String) -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToHiddenUnlock: (pendingHideFolderKey: String) -> Unit,
    viewModel: MediaGridViewModel = viewModel(factory = MediaGridViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var pendingRecycleItemIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK, pendingRecycleItemIds)
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
                MediaGridEvent.RequiresPurchaseToHide -> onNavigateToPurchase()
                is MediaGridEvent.RequiresPinSetupToHide -> onNavigateToHiddenUnlock(event.folderKey)
                MediaGridEvent.FolderHidden -> onBack()
                is MediaGridEvent.RecycleFailed -> scope.launch {
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
                    title = { Text("${uiState.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::deleteSelectedToRecycle) {
                            Icon(Icons.Filled.Delete, contentDescription = "Move to Recycle Bin")
                        }
                        IconButton(onClick = viewModel::requestHideFolder) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = "Hide folder")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.folderDisplayName.ifBlank { "Folder" },
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.name) }, onClick = {
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
                        text = uiState.errorMessage ?: "Unable to load this folder.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                MediaGridPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This folder has no photos or videos to show.")
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
            .padding(2.dp)
            .aspectRatio(1f)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
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
        if (selectionActive && selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                modifier = Modifier.padding(4.dp).background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
