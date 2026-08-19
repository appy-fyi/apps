package com.appyfyi.steadygridgallery.ui.screens.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.data.media.FolderSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    onOpenFolder: (String) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenHiddenFolders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPurchase: () -> Unit,
    viewModel: FoldersViewModel = viewModel(factory = FoldersViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steady Gallery", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Recycle Bin") }, onClick = {
                            menuExpanded = false
                            onOpenRecycleBin()
                        })
                        DropdownMenuItem(text = { Text("Hidden Folders") }, onClick = {
                            menuExpanded = false
                            onOpenHiddenFolders()
                        })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = {
                            menuExpanded = false
                            onOpenSettings()
                        })
                        DropdownMenuItem(text = { Text("Unlock Pro") }, onClick = {
                            menuExpanded = false
                            onOpenPurchase()
                        })
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = { Text("Search folders") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
            )

            when (uiState.phase) {
                FoldersPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                FoldersPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: "Something went wrong loading your folders.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                FoldersPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No media folders found on this device yet.")
                }

                FoldersPhase.POPULATED -> {
                    val folders = uiState.visibleFolders
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(folders, key = { it.folderKey }) { folder ->
                            FolderTile(folder = folder, onClick = { onOpenFolder(folder.folderKey) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderTile(folder: FolderSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (folder.coverUri != null) {
                AsyncImage(
                    model = folder.coverUri,
                    contentDescription = folder.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = folder.displayName,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).size(36.dp),
                )
            }
            if (folder.isCameraFolder) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = "Camera folder",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(6.dp).size(16.dp),
                    )
                }
            }
        }
        Text(
            text = folder.displayName,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "${folder.itemCount} items • ${folder.relativePath}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
