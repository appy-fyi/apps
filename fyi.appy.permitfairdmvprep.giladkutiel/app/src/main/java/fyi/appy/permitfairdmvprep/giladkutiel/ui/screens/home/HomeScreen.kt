package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import fyi.appy.permitfairdmvprep.giladkutiel.data.LessonEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizMode
import fyi.appy.permitfairdmvprep.giladkutiel.data.StatePackEntity
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.FreeAccessState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NoStateSelected : HomeUiState
    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Populated(
        val statePack: StatePackEntity,
        val lessons: List<LessonEntity>,
        val lessonReadById: Map<String, Boolean>,
        val completedLessons: Int,
        val totalLessons: Int,
        val completedPracticeTests: Int,
        val accessState: FreeAccessState,
    ) : HomeUiState
}

class HomeViewModel(
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val stateId = progressRepository.getSelectedStateId()
            if (stateId == null) {
                _uiState.value = HomeUiState.NoStateSelected
                return@launch
            }
            val statePack = contentRepository.getStatePack(stateId)
            if (statePack == null) {
                _uiState.value = HomeUiState.Error("database/content import failed")
                return@launch
            }
            val lessons = contentRepository.getLessonsForState(stateId)
            if (lessons.isEmpty()) {
                _uiState.value = HomeUiState.Empty
                return@launch
            }
            val lessonReadById = lessons.associate { it.id to (progressRepository.getLessonProgress(it.id)?.readCompleted == true) }
            val completedLessons = lessonReadById.values.count { it }
            val attempts = progressRepository.getAttemptsForState(stateId)
            val completedPracticeTests = attempts.count { it.quizMode == QuizMode.PRACTICE_TEST && it.completedAtEpochMillis > 0 }
            val accessState = progressRepository.freeAccessState()
            _uiState.value = HomeUiState.Populated(
                statePack = statePack,
                lessons = lessons,
                lessonReadById = lessonReadById,
                completedLessons = completedLessons,
                totalLessons = lessons.size,
                completedPracticeTests = completedPracticeTests,
                accessState = accessState,
            )
        }
    }

    /** Only decides where to navigate; the QuizViewModel is the one that actually creates the attempt. */
    fun onStartPracticeTestClicked(onNavigateToQuiz: () -> Unit, onNavigateToUnlock: () -> Unit) {
        viewModelScope.launch {
            if (progressRepository.freeAccessState() == FreeAccessState.AFTER_FREE_TEST_COMPLETED) {
                onNavigateToUnlock()
            } else {
                onNavigateToQuiz()
            }
        }
    }

    companion object {
        fun factory(contentRepository: ContentRepository, progressRepository: ProgressRepository) = viewModelFactory {
            initializer { HomeViewModel(contentRepository, progressRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    onNavigateToStateSelect: () -> Unit,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToPracticeQuiz: () -> Unit,
    onNavigateToUnlock: () -> Unit,
    onNavigateToSettings: () -> Unit,
    refreshKey: Any?,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(contentRepository, progressRepository))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshKey) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            HomeUiState.Loading -> CenteredBox(padding) { CircularProgressIndicator() }
            HomeUiState.NoStateSelected -> {
                LaunchedEffect(Unit) { onNavigateToStateSelect() }
                CenteredBox(padding) { CircularProgressIndicator() }
            }
            HomeUiState.Empty -> CenteredBox(padding) { Text("The selected state has no lessons or questions yet.") }
            is HomeUiState.Error -> CenteredBox(padding) { Text(state.message) }
            is HomeUiState.Populated -> PopulatedHome(
                padding = padding,
                state = state,
                onNavigateToLesson = onNavigateToLesson,
                onStartPracticeTest = {
                    viewModel.onStartPracticeTestClicked(onNavigateToPracticeQuiz, onNavigateToUnlock)
                },
            )
        }
    }
}

@Composable
private fun CenteredBox(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PopulatedHome(
    padding: PaddingValues,
    state: HomeUiState.Populated,
    onNavigateToLesson: (String) -> Unit,
    onStartPracticeTest: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text(state.statePack.displayName, style = MaterialTheme.typography.headlineSmall)

        val chipLabel = if (state.accessState == FreeAccessState.LIFETIME_UNLOCKED) "Lifetime unlocked" else "Free mode"
        AssistChip(onClick = {}, label = { Text(chipLabel) }, modifier = Modifier.padding(vertical = 8.dp))

        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Progress", style = MaterialTheme.typography.titleMedium)
                Text("${state.completedLessons} of ${state.totalLessons} lessons completed")
                Text("${state.completedPracticeTests} practice test(s) completed")
            }
        }

        Button(onClick = onStartPracticeTest, modifier = Modifier.fillMaxWidth().testTag("start_practice_test_button")) {
            Text("Start practice test")
        }

        Text("Lessons", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.lessons.sortedBy { it.sortOrder }, key = { it.id }) { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lesson_card")
                        .clickable { onNavigateToLesson(lesson.id) },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
                            Text(lesson.handbookSection, style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.lessonReadById[lesson.id] == true) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Completed")
                        }
                    }
                }
            }
        }
    }
}
