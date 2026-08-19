package com.appyfyi.steadygridgallery.ui.screens.recyclebin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.data.db.entity.RecycleItemEntity
import java.io.File
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    viewModel: RecycleBinViewModel = viewModel(factory = RecycleBinViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.start() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
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
                RecycleBinPhase.LOADING, RecycleBinPhase.RESTORING, RecycleBinPhase.PERMANENTLY_DELETING ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

                RecycleBinPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: "Unable to load Recycle Bin.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                RecycleBinPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle Bin is empty.")
                }

                RecycleBinPhase.POPULATED -> Box(Modifier.fillMaxSize()) {
                    Column {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.items, key = { it.id }) { item ->
                                RecycleRow(
                                    item = item,
                                    selected = item.id in uiState.selectedIds,
                                    onToggle = { viewModel.toggleSelection(item.id) },
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            TextButton(
                                onClick = viewModel::restoreSelected,
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) { Text("Restore selected") }
                            TextButton(
                                onClick = { showDeleteConfirmation = true },
                                enabled = uiState.selectedIds.isNotEmpty(),
                            ) { Text("Delete permanently") }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete permanently?") },
            text = { Text("This cannot be undone. The protected copy in Recycle Bin will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    viewModel.permanentlyDeleteSelected()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun RecycleRow(item: RecycleItemEntity, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        AsyncImage(
            model = File(item.trashedCopyPath),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(item.displayName)
            Text(item.relativePath, style = MaterialTheme.typography.bodySmall)
            Text(
                DateFormat.getDateTimeInstance().format(Date(item.deletedAt.toEpochMilli())),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
