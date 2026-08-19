package com.appyfyi.steadygridgallery.ui.screens.hiddenfolders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.data.media.FolderSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenFoldersScreen(
    onBack: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onRequiresUnlock: () -> Unit,
    viewModel: HiddenFoldersViewModel = viewModel(factory = HiddenFoldersViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hidden Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                HiddenFoldersPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                HiddenFoldersPhase.LOCKED -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Session locked.")
                        androidx.compose.material3.Button(onClick = onRequiresUnlock) { Text("Unlock") }
                    }
                }

                HiddenFoldersPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: "Unable to load hidden folders.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                HiddenFoldersPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No folders are hidden.")
                }

                HiddenFoldersPhase.POPULATED -> {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "Unlocked",
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 128.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.folders, key = { it.folderKey }) { folder ->
                            HiddenFolderTile(
                                folder = folder,
                                onOpen = { onOpenFolder(folder.folderKey) },
                                onUnhide = { viewModel.unhideFolder(folder.folderKey) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenFolderTile(folder: FolderSummary, onOpen: () -> Unit, onUnhide: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onOpen)) {
            AsyncImage(
                model = folder.coverUri,
                contentDescription = folder.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(onClick = onUnhide, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Visibility, contentDescription = "Unhide folder")
            }
        }
        Text(folder.displayName, maxLines = 1)
        Text("${folder.itemCount} items", style = MaterialTheme.typography.bodySmall)
    }
}
