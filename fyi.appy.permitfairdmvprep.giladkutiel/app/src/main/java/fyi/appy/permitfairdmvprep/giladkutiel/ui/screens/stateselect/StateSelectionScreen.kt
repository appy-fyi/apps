package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.stateselect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentLoadState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.StatePackSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StateSelectionViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val allowAutoSkip: Boolean,
) : ViewModel() {
    val loadState: StateFlow<ContentLoadState> = contentRepository.loadState

    private val _pendingSelection = MutableStateFlow<String?>(null)
    val pendingSelection: StateFlow<String?> = _pendingSelection.asStateFlow()

    private val _skipToHome = MutableStateFlow(false)
    val skipToHome: StateFlow<Boolean> = _skipToHome.asStateFlow()

    private val _confirmedSelection = MutableStateFlow(false)
    val confirmedSelection: StateFlow<Boolean> = _confirmedSelection.asStateFlow()

    init {
        viewModelScope.launch {
            contentRepository.ensureContentImported()
            if (!allowAutoSkip) return@launch
            val selected = progressRepository.getSelectedStateId()
            val loaded = contentRepository.loadState.value
            if (selected != null && loaded is ContentLoadState.Loaded && loaded.states.any { it.stateId == selected }) {
                _skipToHome.value = true
            }
        }
    }

    fun selectState(stateId: String) {
        _pendingSelection.value = stateId
    }

    fun confirmSelection() {
        val stateId = _pendingSelection.value ?: return
        viewModelScope.launch {
            progressRepository.setSelectedState(stateId)
            _confirmedSelection.value = true
        }
    }

    companion object {
        fun factory(contentRepository: ContentRepository, progressRepository: ProgressRepository, allowAutoSkip: Boolean) = viewModelFactory {
            initializer { StateSelectionViewModel(contentRepository, progressRepository, allowAutoSkip) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateSelectionScreen(
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    allowAutoSkip: Boolean,
    onNavigateToHome: () -> Unit,
) {
    val viewModel: StateSelectionViewModel = viewModel(
        factory = StateSelectionViewModel.factory(contentRepository, progressRepository, allowAutoSkip),
    )
    val loadState by viewModel.loadState.collectAsState()
    val pendingSelection by viewModel.pendingSelection.collectAsState()
    val skipToHome by viewModel.skipToHome.collectAsState()
    val confirmedSelection by viewModel.confirmedSelection.collectAsState()

    LaunchedEffect(skipToHome) {
        if (skipToHome) onNavigateToHome()
    }
    LaunchedEffect(confirmedSelection) {
        if (confirmedSelection) onNavigateToHome()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PermitFair DMV Prep") }) },
    ) { padding ->
        when (val state = loadState) {
            is ContentLoadState.Loading -> LoadingContent(padding)
            is ContentLoadState.Empty -> MessageContent(padding, "No bundled state packs were found in this build.")
            is ContentLoadState.Error -> MessageContent(padding, "This content pack is invalid: ${state.message}")
            is ContentLoadState.Loaded -> PopulatedContent(
                padding = padding,
                states = state.states,
                selectedStateId = pendingSelection,
                onSelect = viewModel::selectState,
                onContinue = viewModel::confirmSelection,
            )
        }
    }
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageContent(padding: PaddingValues, message: String) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message)
    }
}

@Composable
private fun PopulatedContent(
    padding: PaddingValues,
    states: List<StatePackSummary>,
    selectedStateId: String?,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(states, key = { it.stateId }) { state ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("state_card_${state.stateId}")
                        .selectable(selected = state.stateId == selectedStateId, onClick = { onSelect(state.stateId) }),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.stateId == selectedStateId, onClick = { onSelect(state.stateId) })
                            Text(state.displayName, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        }
                        Text("Source: ${state.handbookTitle}")
                        Text("${state.lessonCount} lessons • ${state.questionCount} questions")
                    }
                }
            }
        }
        Text(
            "Content is sourced from public DMV handbooks",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Button(
            onClick = onContinue,
            enabled = selectedStateId != null,
            modifier = Modifier.fillMaxWidth().testTag("continue_button"),
        ) {
            Text("Continue")
        }
    }
}
