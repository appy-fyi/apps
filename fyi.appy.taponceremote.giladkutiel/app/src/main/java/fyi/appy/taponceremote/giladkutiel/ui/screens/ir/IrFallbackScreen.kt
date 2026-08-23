package fyi.appy.taponceremote.giladkutiel.ui.screens.ir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import fyi.appy.taponceremote.giladkutiel.data.ir.IrProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrFallbackScreen() {
    val app = LocalContext.current.applicationContext as TapOnceApplication
    val viewModel: IrFallbackViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                IrFallbackViewModel(app.irTransmitter, app.irProfileRepository, app.database.savedDeviceDao())
            }
        },
    )
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("IR Remote") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val current = state) {
                IrState.CheckingHardware -> Text("Checking for infrared hardware…")
                IrState.NoHardware -> Text(
                    "This device doesn't have an IR blaster, so IR remote control isn't available.",
                    color = MaterialTheme.colorScheme.error,
                )
                is IrState.Ready -> {
                    Text("Choose your TV brand", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        current.profiles.forEach { profile ->
                            OutlinedButton(onClick = { viewModel.selectProfile(profile) }) {
                                Text(profile.name)
                            }
                        }
                    }

                    if (current.selected != null) {
                        Card {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("Selected: ${current.selected.name}")
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IrCommandButton("Power", viewModel)
                                    IrCommandButton("VolumeMute", viewModel, label = "Mute")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IrCommandButton("VolumeDown", viewModel, label = "Vol -")
                                    IrCommandButton("VolumeUp", viewModel, label = "Vol +")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IrCommandButton("ChannelDown", viewModel, label = "Ch -")
                                    IrCommandButton("ChannelUp", viewModel, label = "Ch +")
                                }
                                Button(onClick = { viewModel.sendCommand("Power") }) { Text("Test command") }
                                Button(onClick = { viewModel.saveProfile() }) { Text("Save IR profile") }
                                if (current.saved) Text("Saved.")
                                if (current.error != null) {
                                    Text(
                                        "Couldn't send command: ${current.error}",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IrCommandButton(commandKey: String, viewModel: IrFallbackViewModel, label: String = commandKey) {
    Button(
        onClick = { viewModel.sendCommand(commandKey) },
        modifier = Modifier.size(56.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
