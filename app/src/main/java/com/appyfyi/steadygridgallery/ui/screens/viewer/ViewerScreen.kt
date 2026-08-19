package com.appyfyi.steadygridgallery.ui.screens.viewer

import android.app.Activity
import android.content.Intent
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appyfyi.steadygridgallery.data.media.MediaItem
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    onEditMedia: (String) -> Unit,
    viewModel: ViewerViewModel = viewModel(factory = ViewerViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showInfoSheet by remember { mutableStateOf(false) }
    var pendingRecycleItemId by remember { mutableLongStateOf(-1L) }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK, pendingRecycleItemId)
    }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ViewerEvent.ConfirmSystemDelete -> {
                    pendingRecycleItemId = event.recycleItemId
                    deleteRequestLauncher.launch(IntentSenderRequest.Builder(event.pendingIntent.intentSender).build())
                }
                is ViewerEvent.RecycleFailed -> scope.launch { snackbarHostState.showSnackbar(event.message) }
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.currentItem?.displayName.orEmpty(), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    val item = uiState.currentItem
                    if (item != null && uiState.phase != ViewerPhase.DELETED_TO_RECYCLE) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType
                                putExtra(Intent.EXTRA_STREAM, item.contentUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = Color.White)
                        }
                        if (uiState.phase == ViewerPhase.DISPLAYING_IMAGE) {
                            IconButton(onClick = { onEditMedia(item.mediaId) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White)
                            }
                        }
                        IconButton(onClick = viewModel::moveCurrentToRecycle) {
                            Icon(Icons.Filled.Delete, contentDescription = "Move to Recycle Bin", tint = Color.White)
                        }
                    }
                },
                // Without this, the default M3 app bar background is the theme's light surface
                // color, so these white-tinted icons/title become invisible against it.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .pointerInput(uiState.currentItem?.mediaId) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -50) viewModel.showNext()
                        if (dragAmount > 50) viewModel.showPrevious()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when (uiState.phase) {
                ViewerPhase.LOADING -> CircularProgressIndicator()

                ViewerPhase.ERROR -> Text(
                    text = uiState.errorMessage ?: "Unable to display this item.",
                    color = Color.White,
                )

                ViewerPhase.DISPLAYING_IMAGE -> uiState.currentItem?.let { item ->
                    AsyncImage(
                        model = item.contentUri,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ViewerPhase.DISPLAYING_VIDEO -> uiState.currentItem?.let { item ->
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(item.contentUri)
                                setOnPreparedListener { it.isLooping = false; start() }
                            }
                        },
                        update = { view -> view.setVideoURI(item.contentUri) },
                    )
                }

                ViewerPhase.DELETED_TO_RECYCLE -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Moved to Recycle Bin.", color = Color.White)
                    Button(onClick = onBack) { Text("Back to folder") }
                }
            }
        }
    }

    if (showInfoSheet && uiState.currentItem != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showInfoSheet = false }, sheetState = sheetState) {
            MediaInfoContent(uiState.currentItem!!)
        }
    }
}

@Composable
private fun MediaInfoContent(item: MediaItem) {
    val dateText = remember(item.dateTakenMillis, item.dateAddedMillis) {
        val millis = item.dateTakenMillis.takeIf { it > 0 } ?: item.dateAddedMillis
        if (millis > 0) DateFormat.getDateTimeInstance().format(Date(millis)) else "Unknown"
    }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(item.displayName, style = MaterialTheme.typography.titleMedium)
        Text("Date: $dateText")
        Text("Dimensions: ${item.width} x ${item.height}")
        Text("Type: ${item.mimeType}")
        Text("Path: ${item.relativePath}")
    }
}
