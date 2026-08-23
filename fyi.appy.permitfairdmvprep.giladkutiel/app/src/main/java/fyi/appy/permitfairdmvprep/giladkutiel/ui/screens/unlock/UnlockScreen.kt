package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.unlock

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingRepository
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingUiState
import fyi.appy.permitfairdmvprep.giladkutiel.billing.PurchaseEvent
import fyi.appy.permitfairdmvprep.giladkutiel.ui.components.PricingDisclosure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UnlockViewModel(private val billingRepository: BillingRepository) : ViewModel() {
    val billingUiState: StateFlow<BillingUiState> = billingRepository.uiState

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _purchaseCompleted = MutableStateFlow(false)
    val purchaseCompleted: StateFlow<Boolean> = _purchaseCompleted.asStateFlow()

    init {
        billingRepository.startConnection()
        viewModelScope.launch {
            billingRepository.purchaseEvents.collect { event ->
                when (event) {
                    PurchaseEvent.Completed -> {
                        _statusMessage.value = "Purchase completed. Lifetime unlock is active."
                        _purchaseCompleted.value = true
                    }
                    PurchaseEvent.Pending -> _statusMessage.value = "Purchase pending — this will update once it clears."
                    PurchaseEvent.Canceled -> _statusMessage.value = "Purchase canceled."
                    PurchaseEvent.AlreadyOwned -> {
                        _statusMessage.value = "Purchase restored."
                        _purchaseCompleted.value = true
                    }
                    is PurchaseEvent.Error -> _statusMessage.value = "Billing error: ${event.message}"
                }
            }
        }
    }

    fun onBuyClicked(activity: Activity) {
        billingRepository.launchPurchaseFlow(activity)
    }

    fun onRestoreClicked() {
        viewModelScope.launch { billingRepository.restorePurchases() }
    }

    companion object {
        fun factory(billingRepository: BillingRepository) = viewModelFactory {
            initializer { UnlockViewModel(billingRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    billingRepository: BillingRepository,
    onUnlocked: () -> Unit,
) {
    val viewModel: UnlockViewModel = viewModel(factory = UnlockViewModel.factory(billingRepository))
    val billingUiState by viewModel.billingUiState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val purchaseCompleted by viewModel.purchaseCompleted.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(purchaseCompleted) {
        if (purchaseCompleted) onUnlocked()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Unlock") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("One-time lifetime unlock", style = MaterialTheme.typography.headlineSmall)

            when (val state = billingUiState) {
                BillingUiState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is BillingUiState.Ready -> {
                    Text(state.formattedPrice, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(vertical = 12.dp))
                    Text("• No weekly subscription. No trial.")
                    Text("• No account needed")
                    Text("• Unlocks lifetime access to every practice test and lesson quiz")

                    Button(
                        onClick = { (context as? Activity)?.let { viewModel.onBuyClicked(it) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Text("Buy lifetime unlock")
                    }
                }
                BillingUiState.Unavailable -> Text(
                    "Billing is unavailable right now. Your free practice test is still usable if you haven't used it yet.",
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                BillingUiState.ProductNotFound -> Text(
                    "This product could not be found. Please try again later.",
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }

            OutlinedButton(onClick = viewModel::onRestoreClicked, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Restore purchase")
            }

            statusMessage?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }

            PricingDisclosure(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
