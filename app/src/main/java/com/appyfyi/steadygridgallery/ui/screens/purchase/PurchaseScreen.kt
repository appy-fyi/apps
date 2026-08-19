package com.appyfyi.steadygridgallery.ui.screens.purchase

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appyfyi.steadygridgallery.data.billing.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onBackToSettings: () -> Unit,
    viewModel: PurchaseViewModel = viewModel(factory = PurchaseViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    Scaffold(topBar = { TopAppBar(title = { Text("Unlock Pro") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Unlock the editor and hidden folders forever with a single, one-time purchase. " +
                    "No subscription, no account required.",
                style = MaterialTheme.typography.bodyMedium,
            )

            when (val state = uiState) {
                is BillingUiState.LoadingProducts -> CircularProgressIndicator()

                is BillingUiState.NotPurchased -> {
                    Text(state.formattedPrice, style = MaterialTheme.typography.headlineMedium)
                    Button(onClick = { activity?.let(viewModel::buy) }) { Text("Buy unlock") }
                    OutlinedButton(onClick = viewModel::restore) { Text("Restore purchase") }
                }

                is BillingUiState.Purchasing -> CircularProgressIndicator()

                is BillingUiState.Purchased -> {
                    Text("Pro unlocked. Thank you!")
                    Button(onClick = onBackToSettings) { Text("Back to Settings") }
                }

                is BillingUiState.BillingUnavailable -> {
                    Text(
                        "Play Billing is unavailable on this device right now.",
                        color = MaterialTheme.colorScheme.error,
                    )
                    OutlinedButton(onClick = viewModel::retryConnection) { Text("Retry") }
                }

                is BillingUiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = viewModel::retryConnection) { Text("Retry") }
                }
            }
        }
    }
}
