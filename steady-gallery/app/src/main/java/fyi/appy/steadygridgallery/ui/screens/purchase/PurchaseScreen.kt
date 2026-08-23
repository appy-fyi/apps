package fyi.appy.steadygridgallery.ui.screens.purchase

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fyi.appy.steadygridgallery.BuildConfig
import fyi.appy.steadygridgallery.R
import fyi.appy.steadygridgallery.data.billing.BillingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    onBackToSettings: () -> Unit,
    viewModel: PurchaseViewModel = viewModel(factory = PurchaseViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.purchase_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackToSettings) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(20.dp).size(40.dp),
                )
            }

            Text(
                stringResource(R.string.purchase_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (val state = uiState) {
                        is BillingUiState.LoadingProducts -> CircularProgressIndicator()

                        is BillingUiState.NotPurchased -> {
                            Text(state.formattedPrice, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.purchase_one_time),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = { activity?.let(viewModel::buy) },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.purchase_buy_button)) }
                            OutlinedButton(
                                onClick = viewModel::restore,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.purchase_restore_button)) }
                        }

                        is BillingUiState.Purchasing -> CircularProgressIndicator()

                        is BillingUiState.Purchased -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(stringResource(R.string.purchase_thank_you), style = MaterialTheme.typography.titleMedium)
                            Button(onClick = onBackToSettings, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.common_continue))
                            }
                        }

                        is BillingUiState.BillingUnavailable -> {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                stringResource(R.string.purchase_billing_unavailable),
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                            OutlinedButton(onClick = viewModel::retryConnection, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }

                        is BillingUiState.Error -> {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            OutlinedButton(onClick = viewModel::retryConnection, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }
            }

            // Debug-only: this whole block is compiled out of release builds (BuildConfig.DEBUG
            // is a compile-time constant), so it can't ship. Lets Editor/Hidden Folders be tested
            // locally before the app exists in Play Console for a real purchase.
            if (BuildConfig.DEBUG) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(R.string.purchase_debug_tools_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = viewModel::debugGrantEntitlement, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.purchase_debug_simulate))
                }
                OutlinedButton(onClick = viewModel::debugRevokeEntitlement, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.purchase_debug_revoke))
                }
            }
        }
    }
}
