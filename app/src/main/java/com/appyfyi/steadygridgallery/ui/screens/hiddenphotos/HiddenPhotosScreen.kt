package com.appyfyi.steadygridgallery.ui.screens.hiddenphotos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaEntity
import com.appyfyi.steadygridgallery.data.media.MediaKind
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenPhotosScreen(
    onBack: () -> Unit,
    onRequiresUnlock: () -> Unit,
    viewModel: HiddenPhotosViewModel = viewModel(factory = HiddenPhotosViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    // Leaving this screen -- by any route, not just the back button -- must require unlocking
    // again to come back in, per the "hidden photos" spec.
    DisposableEffect(Unit) {
        onDispose { viewModel.lockNow() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_photos_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.phase) {
                HiddenPhotosPhase.LOADING -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                HiddenPhotosPhase.LOCKED -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.hidden_photos_session_locked))
                        Button(onClick = onRequiresUnlock) { Text(stringResource(R.string.common_unlock)) }
                    }
                }

                HiddenPhotosPhase.ERROR -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage ?: stringResource(R.string.hidden_photos_error_fallback),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                HiddenPhotosPhase.EMPTY -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.hidden_photos_empty))
                }

                HiddenPhotosPhase.POPULATED -> {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            stringResource(R.string.hidden_photos_unlocked_banner),
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            HiddenPhotoTile(
                                item = item,
                                onUnhide = { viewModel.unhide(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenPhotoTile(item: HiddenMediaEntity, onUnhide: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = File(item.hiddenCopyPath),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.kind == MediaKind.VIDEO.name) {
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
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        ) {
            IconButton(onClick = onUnhide) {
                Icon(Icons.Filled.Visibility, contentDescription = stringResource(R.string.hidden_photos_unhide))
            }
        }
    }
}
