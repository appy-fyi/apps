package fyi.appy.inksend.giladkutiel.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fyi.appy.inksend.giladkutiel.BuildConfig
import fyi.appy.inksend.giladkutiel.ui.localAppContainer
import kotlinx.coroutines.launch

private const val PRIVACY_POLICY_URL = "https://appy.fyi/app/fyi.appy.inksend.giladkutiel/privacy"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = localAppContainer()
    val coroutineScope = rememberCoroutineScope()

    val purchased by container.preferencesRepository.purchased.collectAsState(initial = false)
    val presets by container.styleRepository.observeStyles().collectAsState(initial = emptyList())
    val defaultPreset = presets.firstOrNull { it.isDefault }
    var restoring by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Default style") },
                supportingContent = { Text(defaultPreset?.name ?: "None selected") },
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Purchase status") },
                supportingContent = { Text(if (purchased) "Unlocked — all styles and handwriting fonts" else "Free tier — 3 built-in styles") },
                trailingContent = {
                    if (restoring) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    } else {
                        TextButton(onClick = {
                            restoring = true
                            coroutineScope.launch {
                                container.billingRepository.refreshPurchaseState()
                                restoring = false
                            }
                        }) { Text("Restore purchase") }
                    }
                },
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Privacy policy") },
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                },
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text(BuildConfig.VERSION_NAME) },
            )
        }
    }
}
