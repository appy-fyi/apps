package fyi.appy.taponceremote.giladkutiel.ui.screens.remote

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.taponceremote.giladkutiel.TapOnceApplication
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.remote.CommandResult
import fyi.appy.taponceremote.giladkutiel.data.remote.RemoteCommand
import fyi.appy.taponceremote.giladkutiel.review.ReviewHelper

private val BUTTON_MIN_SIZE = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    deviceId: Long,
    onOpenTouchpad: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenIrFallback: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TapOnceApplication
    val viewModel: RemoteControlViewModel = viewModel(
        key = "remote-$deviceId",
        factory = viewModelFactory {
            initializer {
                RemoteControlViewModel(deviceId, app, app.database.savedDeviceDao())
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.reviewRequested) {
        if (uiState.reviewRequested) {
            (context as? Activity)?.let { ReviewHelper.maybeRequestReview(it) }
            viewModel.onReviewRequestHandled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.device?.displayName ?: "Remote") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        androidx.compose.material3.Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ConnectionStatusChip(uiState.phase)

            when (uiState.phase) {
                ConnectionPhase.DEVICE_NOT_FOUND -> Text("This device is no longer saved.")
                ConnectionPhase.DEVICE_OFFLINE -> {
                    Text("Device offline or unreachable.")
                    OutlinedButton(onClick = onOpenIrFallback) { Text("Try IR fallback") }
                }
                else -> {
                    RemoteButtonsGrid(
                        protocol = uiState.device?.protocol,
                        onSend = viewModel::sendCommand,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onOpenTouchpad) { Text("Touchpad") }
                        OutlinedButton(onClick = onOpenTouchpad) { Text("Keyboard") }
                    }

                    uiState.lastResult?.let { result ->
                        Text(
                            when (result) {
                                is CommandResult.Success -> ""
                                is CommandResult.Unsupported -> "This TV doesn't support that action."
                                is CommandResult.TextUnsupported -> "Text entry isn't supported for this TV."
                                is CommandResult.Failure -> "Command failed: ${result.reason}"
                            },
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusChip(phase: ConnectionPhase) {
    val label = when (phase) {
        ConnectionPhase.LOADING_DEVICE -> "Loading…"
        ConnectionPhase.CONNECTING -> "Connecting…"
        ConnectionPhase.CONNECTED -> "Connected"
        ConnectionPhase.DEVICE_OFFLINE -> "Offline"
        ConnectionPhase.DEVICE_NOT_FOUND -> "Not found"
    }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
private fun RemoteButtonsGrid(protocol: RemoteProtocol?, onSend: (RemoteCommand) -> Unit) {
    val supportsNav = protocol == RemoteProtocol.ROKU_ECP || protocol == RemoteProtocol.MANUAL_ROKU_ECP
    val supportsChannel = supportsNav
    val supportsPower = supportsNav

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (supportsPower) {
                RemoteButton("Power") { onSend(RemoteCommand.Power) }
            }
            RemoteButton("Mute") { onSend(RemoteCommand.VolumeMute) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton("Vol -") { onSend(RemoteCommand.VolumeDown) }
            RemoteButton("Vol +") { onSend(RemoteCommand.VolumeUp) }
            if (supportsChannel) {
                RemoteButton("Ch -") { onSend(RemoteCommand.ChannelDown) }
                RemoteButton("Ch +") { onSend(RemoteCommand.ChannelUp) }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text("Navigation", style = MaterialTheme.typography.labelLarge)
        RemoteButton("Up") { onSend(RemoteCommand.Up) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton("Left") { onSend(RemoteCommand.Left) }
            RemoteButton("OK") { onSend(RemoteCommand.Select) }
            RemoteButton("Right") { onSend(RemoteCommand.Right) }
        }
        RemoteButton("Down") { onSend(RemoteCommand.Down) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton("Home") { onSend(RemoteCommand.Home) }
            RemoteButton("Back") { onSend(RemoteCommand.Back) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RemoteButton("Rev") { onSend(RemoteCommand.Rev) }
            RemoteButton("Play") { onSend(RemoteCommand.Play) }
            RemoteButton("Fwd") { onSend(RemoteCommand.Fwd) }
        }
    }
}

@Composable
private fun RemoteButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = androidx.compose.ui.Modifier.size(BUTTON_MIN_SIZE)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
