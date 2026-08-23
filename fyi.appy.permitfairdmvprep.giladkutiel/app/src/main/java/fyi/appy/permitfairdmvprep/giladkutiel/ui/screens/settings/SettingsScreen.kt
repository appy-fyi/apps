package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingRepository
import fyi.appy.permitfairdmvprep.giladkutiel.billing.BillingUiState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.FreeAccessState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import fyi.appy.permitfairdmvprep.giladkutiel.ui.components.PricingDisclosure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val selectedStateDisplayName: String? = null,
    val accessState: FreeAccessState = FreeAccessState.BEFORE_FREE_TEST_COMPLETED,
)

class SettingsViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val billingRepository: BillingRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val billingUiState: StateFlow<BillingUiState> = billingRepository.uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val stateId = progressRepository.getSelectedStateId()
            val displayName = stateId?.let { contentRepository.getStatePack(it)?.displayName }
            val accessState = progressRepository.freeAccessState()
            _uiState.value = SettingsUiState(loading = false, selectedStateDisplayName = displayName, accessState = accessState)
        }
    }

    fun onRestoreClicked() {
        viewModelScope.launch {
            billingRepository.restorePurchases()
            refresh()
        }
    }

    companion object {
        fun factory(
            contentRepository: ContentRepository,
            progressRepository: ProgressRepository,
            billingRepository: BillingRepository,
        ) = viewModelFactory {
            initializer { SettingsViewModel(contentRepository, progressRepository, billingRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    billingRepository: BillingRepository,
    onChangeState: () -> Unit,
    refreshKey: Any?,
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(contentRepository, progressRepository, billingRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    val billingUiState by viewModel.billingUiState.collectAsState()

    LaunchedEffect(refreshKey) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Selected state", style = MaterialTheme.typography.labelLarge)
                    Text(uiState.selectedStateDisplayName ?: "None")
                }
                OutlinedButton(onClick = onChangeState) { Text("Change") }
            }

            Text(
                text = "Purchase status: " + when (uiState.accessState) {
                    FreeAccessState.LIFETIME_UNLOCKED -> "Lifetime unlocked"
                    FreeAccessState.AFTER_FREE_TEST_COMPLETED -> "Free test used"
                    FreeAccessState.BEFORE_FREE_TEST_COMPLETED -> "Free test available"
                },
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (billingUiState is BillingUiState.Unavailable) {
                Text("Billing is unavailable right now.", modifier = Modifier.padding(bottom = 8.dp))
            }

            Button(onClick = viewModel::onRestoreClicked, modifier = Modifier.fillMaxWidth()) {
                Text("Restore purchase")
            }

            PricingDisclosure(modifier = Modifier.padding(top = 16.dp))

            Text(
                "Privacy: this app has no account and collects no personal data.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )

            Text(
                "PermitFair DMV Prep v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
