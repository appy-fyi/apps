package fyi.appy.taponceremote.giladkutiel.ui.screens.touchpad

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.taponceremote.giladkutiel.TapOnceApplication
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchpadKeyboardScreen(deviceId: Long) {
    val context = LocalContext.current
    val app = context.applicationContext as TapOnceApplication
    val viewModel: TouchpadKeyboardViewModel = viewModel(
        key = "touchpad-$deviceId",
        factory = viewModelFactory {
            initializer { TouchpadKeyboardViewModel(deviceId, app, app.database.savedDeviceDao()) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    var text by remember { mutableStateOf("") }
    val dragThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }

    Scaffold(topBar = { TopAppBar(title = { Text("Touchpad") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Swipe to navigate, tap to select", style = MaterialTheme.typography.bodyMedium)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { viewModel.sendGesture(RemoteCommand.Select) })
                    }
                    .pointerInput(Unit) {
                        var totalDrag = Offset.Zero
                        detectDragGestures(
                            onDragStart = { totalDrag = Offset.Zero },
                            onDragEnd = {
                                val absX = abs(totalDrag.x)
                                val absY = abs(totalDrag.y)
                                when {
                                    absX > absY && absX > dragThresholdPx ->
                                        viewModel.sendGesture(if (totalDrag.x > 0) RemoteCommand.Right else RemoteCommand.Left)
                                    absY >= absX && absY > dragThresholdPx ->
                                        viewModel.sendGesture(if (totalDrag.y > 0) RemoteCommand.Down else RemoteCommand.Up)
                                }
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Touchpad", style = MaterialTheme.typography.labelLarge)
            }

            when (uiState.gestureState) {
                GestureState.TEXT_UNSUPPORTED -> Text(
                    "Text entry isn't supported for this TV.",
                    color = MaterialTheme.colorScheme.error,
                )
                GestureState.COMMAND_FAILED -> Text(
                    "Command failed.",
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Type to send") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    viewModel.sendText(text) { result ->
                        if (result !is fyi.appy.taponceremote.giladkutiel.data.remote.CommandResult.TextUnsupported) {
                            text = ""
                        }
                    }
                }) { Text("Send text") }
                Button(onClick = { text = "" }) { Text("Clear") }
            }
        }
    }
}
