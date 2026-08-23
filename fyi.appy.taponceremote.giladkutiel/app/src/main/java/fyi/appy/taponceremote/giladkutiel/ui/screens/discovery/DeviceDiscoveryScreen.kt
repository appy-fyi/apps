package fyi.appy.taponceremote.giladkutiel.ui.screens.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.taponceremote.giladkutiel.TapOnceApplication
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    onDeviceReady: (Long) -> Unit,
    onOpenIrFallback: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as TapOnceApplication
    val viewModel: DeviceDiscoveryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DeviceDiscoveryViewModel(app.discoveryRepository, app.database.savedDeviceDao()) }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    var manualIp by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.navigateToRemote.collect { id -> onDeviceReady(id) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Find TV") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        when (uiState.screenState) {
                            DiscoveryScreenState.SCANNING -> "Scanning for TVs…"
                            DiscoveryScreenState.NETWORK_ERROR -> "Couldn't scan the network"
                            else -> "Nearby and saved TVs"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (uiState.isScanning) {
                        CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
                    } else {
                        TextButton(onClick = { viewModel.scan() }) { Text("Refresh") }
                    }
                }
            }

            if (uiState.savedDevices.isNotEmpty()) {
                item { Text("Saved", style = MaterialTheme.typography.labelLarge) }
                items(uiState.savedDevices, key = { "saved-${it.id}" }) { device ->
                    SavedDeviceRow(device) { viewModel.connectSaved(device) }
                }
            }

            if (uiState.discovered.isNotEmpty()) {
                item { Text("Discovered", style = MaterialTheme.typography.labelLarge) }
                items(uiState.discovered, key = { "discovered-${it.key}" }) { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(device.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(device.protocol.name, style = MaterialTheme.typography.labelMedium)
                            }
                            Button(onClick = { viewModel.connectDiscovered(device) }) { Text("Connect") }
                        }
                    }
                }
            }

            if (uiState.screenState == DiscoveryScreenState.EMPTY_NO_SAVED_OR_DISCOVERED) {
                item {
                    Column {
                        Text("No TVs found yet.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.scan() }) { Text("Refresh") }
                            OutlinedButton(onClick = onOpenIrFallback) { Text("Use IR remote") }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text("Enter TV IP address manually", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = {
                            manualIp = it
                            viewModel.clearManualIpError()
                        },
                        label = { Text("IP address") },
                        isError = uiState.manualIpError != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (uiState.manualIpError != null) {
                        Text(uiState.manualIpError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { viewModel.submitManualIp(manualIp) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text("Connect") }
                }
            }

            item {
                OutlinedButton(onClick = onOpenIrFallback, modifier = Modifier.fillMaxWidth()) {
                    Text("IR fallback")
                }
            }
        }
    }
}

@Composable
private fun SavedDeviceRow(device: SavedDevice, onConnect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(device.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(device.protocol.name, style = MaterialTheme.typography.labelMedium)
            }
            Button(onClick = onConnect) { Text("Connect") }
        }
    }
}
