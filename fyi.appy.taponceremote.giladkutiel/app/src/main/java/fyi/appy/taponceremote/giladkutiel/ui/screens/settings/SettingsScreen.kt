package fyi.appy.taponceremote.giladkutiel.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.taponceremote.giladkutiel.TapOnceApplication

const val PRIVACY_POLICY_URL = "https://appy.fyi/app/fyi.appy.taponceremote.giladkutiel/privacy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as TapOnceApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(app.database.savedDeviceDao()) }
        },
    )
    val savedDevices by viewModel.savedDevices.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Free, no ads", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "TapOnce Remote is completely free. No ads, no purchases, no subscription.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item { Text("Saved devices", style = MaterialTheme.typography.titleMedium) }

            if (savedDevices.isEmpty()) {
                item { Text("No saved devices yet.") }
            } else {
                items(savedDevices, key = { it.id }) { device ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(device.displayName)
                            TextButton(onClick = { viewModel.deleteDevice(device) }) { Text("Delete") }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                }) { Text("Privacy policy") }
            }
        }
    }
}
