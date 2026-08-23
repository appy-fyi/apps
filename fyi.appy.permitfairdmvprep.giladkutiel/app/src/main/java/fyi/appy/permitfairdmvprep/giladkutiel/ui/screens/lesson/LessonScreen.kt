package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.FreeAccessState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LessonUiState {
    data object Loading : LessonUiState
    data object NotFound : LessonUiState
    data class Populated(
        val lesson: LessonEntity,
        val readCompleted: Boolean,
        val locked: Boolean,
    ) : LessonUiState
}

class LessonViewModel(
    private val lessonId: String,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lesson = contentRepository.getLesson(lessonId)
            if (lesson == null) {
                _uiState.value = LessonUiState.NotFound
                return@launch
            }
            progressRepository.touchLessonOpened(lessonId)
            val readCompleted = progressRepository.getLessonProgress(lessonId)?.readCompleted == true
            val locked = progressRepository.freeAccessState() == FreeAccessState.AFTER_FREE_TEST_COMPLETED
            _uiState.value = LessonUiState.Populated(lesson, readCompleted, locked)
        }
    }

    fun markRead() {
        val current = _uiState.value
        if (current !is LessonUiState.Populated) return
        viewModelScope.launch {
            progressRepository.markLessonRead(lessonId)
            _uiState.value = current.copy(readCompleted = true)
        }
    }

    fun onStartLessonQuizClicked(onNavigateToQuiz: (String) -> Unit, onNavigateToUnlock: () -> Unit) {
        viewModelScope.launch {
            if (progressRepository.freeAccessState() == FreeAccessState.AFTER_FREE_TEST_COMPLETED) {
                onNavigateToUnlock()
                return@launch
            }
            progressRepository.markLessonRead(lessonId)
            onNavigateToQuiz(lessonId)
        }
    }

    companion object {
        fun factory(lessonId: String, contentRepository: ContentRepository, progressRepository: ProgressRepository) = viewModelFactory {
            initializer { LessonViewModel(lessonId, contentRepository, progressRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: String,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    onBack: () -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToUnlock: () -> Unit,
) {
    val viewModel: LessonViewModel = viewModel(
        factory = LessonViewModel.factory(lessonId, contentRepository, progressRepository),
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            LessonUiState.Loading -> LoadingBox(padding)
            LessonUiState.NotFound -> NotFoundBox(padding, onBack)
            is LessonUiState.Populated -> PopulatedLesson(
                padding = padding,
                state = state,
                onMarkRead = viewModel::markRead,
                onStartQuiz = { viewModel.onStartLessonQuizClicked(onNavigateToQuiz, onNavigateToUnlock) },
            )
        }
    }
}

@Composable
private fun LoadingBox(padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun NotFoundBox(padding: PaddingValues, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("This lesson could not be found.")
        Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back to Home") }
    }
}

@Composable
private fun PopulatedLesson(
    padding: PaddingValues,
    state: LessonUiState.Populated,
    onMarkRead: () -> Unit,
    onStartQuiz: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.lesson.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (state.readCompleted) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Completed")
            }
        }
        Text(state.lesson.handbookSection, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))

        LessonBody(state.lesson.bodyMarkdown)

        if (state.locked) {
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("You've used your free full practice test.")
                    Text("Unlock lifetime access to take this lesson's quiz.")
                }
            }
        }

        if (!state.readCompleted) {
            Button(onClick = onMarkRead, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("mark_read_button")) {
                Text("Mark read")
            }
        }
        Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("start_lesson_quiz_button")) {
            Text("Start lesson quiz")
        }
    }
}

/** No WebView — supports paragraphs and bullet lines starting with "- " only (appy build-spec §4). */
@Composable
private fun LessonBody(bodyMarkdown: String) {
    val blocks = bodyMarkdown.split("\n\n")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            val lines = block.lines().filter { it.isNotBlank() }
            if (lines.all { it.trimStart().startsWith("- ") }) {
                Column {
                    for (line in lines) {
                        Text("•  " + line.trimStart().removePrefix("- "))
                    }
                }
            } else {
                Text(block.trim())
            }
        }
    }
}
