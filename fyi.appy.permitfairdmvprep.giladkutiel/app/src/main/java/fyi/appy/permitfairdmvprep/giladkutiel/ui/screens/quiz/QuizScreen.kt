package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.permitfairdmvprep.giladkutiel.data.AnswerOptionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuestionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizMode
import fyi.appy.permitfairdmvprep.giladkutiel.repository.AttemptStartResult
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object RequiresUnlock : QuizUiState
    data object NotEnoughQuestions : QuizUiState
    data class Active(
        val attemptId: String,
        val questionIndex: Int,
        val totalQuestions: Int,
        val question: QuestionEntity,
        val options: List<AnswerOptionEntity>,
        val selectedOptionId: String?,
    ) : QuizUiState

    data class Completed(val attemptId: String) : QuizUiState
}

class QuizViewModel(
    private val quizMode: String,
    private val lessonId: String?,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var questions: List<QuestionEntity> = emptyList()
    private var optionsByQuestion: Map<String, List<AnswerOptionEntity>> = emptyMap()
    private val selectedOptionByQuestion = mutableMapOf<String, String>()
    private var attemptId: String = ""
    private var questionIndex: Int = 0

    init {
        viewModelScope.launch {
            val stateId = progressRepository.getSelectedStateId()
            if (stateId == null) {
                _uiState.value = QuizUiState.NotEnoughQuestions
                return@launch
            }
            val result = if (quizMode == QuizMode.PRACTICE_TEST) {
                progressRepository.startPracticeTest(stateId)
            } else {
                progressRepository.startLessonQuiz(stateId, requireNotNull(lessonId))
            }
            when (result) {
                AttemptStartResult.RequiresUnlock -> _uiState.value = QuizUiState.RequiresUnlock
                is AttemptStartResult.Started -> {
                    val attempt = result.attempt
                    val questionIds = attempt.questionIdsCsv.split(",").filter { it.isNotBlank() }
                    if (questionIds.isEmpty()) {
                        _uiState.value = QuizUiState.NotEnoughQuestions
                        return@launch
                    }
                    val loadedQuestions = contentRepository.getQuestionsByIds(questionIds)
                    val byId = loadedQuestions.associateBy { it.id }
                    questions = questionIds.mapNotNull { byId[it] }
                    optionsByQuestion = questions.associate { it.id to contentRepository.getOptionsForQuestion(it.id) }
                    attemptId = attempt.id
                    questionIndex = 0
                    showCurrentQuestion()
                }
            }
        }
    }

    private fun showCurrentQuestion() {
        val question = questions[questionIndex]
        _uiState.value = QuizUiState.Active(
            attemptId = attemptId,
            questionIndex = questionIndex,
            totalQuestions = questions.size,
            question = question,
            options = optionsByQuestion[question.id].orEmpty(),
            selectedOptionId = selectedOptionByQuestion[question.id],
        )
    }

    fun selectOption(optionId: String) {
        val question = questions.getOrNull(questionIndex) ?: return
        val option = optionsByQuestion[question.id]?.firstOrNull { it.id == optionId } ?: return
        selectedOptionByQuestion[question.id] = optionId
        viewModelScope.launch {
            progressRepository.recordAnswer(attemptId, question.id, optionId, option.isCorrect)
        }
        showCurrentQuestion()
    }

    fun onNext() {
        if (questionIndex >= questions.lastIndex) {
            viewModelScope.launch {
                progressRepository.completeAttempt(attemptId)
                _uiState.value = QuizUiState.Completed(attemptId)
            }
        } else {
            questionIndex += 1
            showCurrentQuestion()
        }
    }

    companion object {
        fun factory(
            quizMode: String,
            lessonId: String?,
            contentRepository: ContentRepository,
            progressRepository: ProgressRepository,
        ) = viewModelFactory {
            initializer { QuizViewModel(quizMode, lessonId, contentRepository, progressRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    quizMode: String,
    lessonId: String?,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    onNavigateToUnlock: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onExit: () -> Unit,
) {
    val viewModel: QuizViewModel = viewModel(
        factory = QuizViewModel.factory(quizMode, lessonId, contentRepository, progressRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is QuizUiState.Completed) onNavigateToResults(state.attemptId)
        if (state is QuizUiState.RequiresUnlock) onNavigateToUnlock()
    }

    BackHandler(enabled = uiState is QuizUiState.Active) { showExitDialog = true }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit quiz?") },
            text = { Text("Your progress on this question will be kept, but the quiz will end.") },
            confirmButton = { TextButton(onClick = { showExitDialog = false; onExit() }) { Text("Exit") } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Keep studying") } },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (quizMode == QuizMode.PRACTICE_TEST) "Practice test" else "Lesson quiz") }) },
    ) { padding ->
        when (val state = uiState) {
            QuizUiState.Loading, QuizUiState.RequiresUnlock -> CenteredBox(padding) { CircularProgressIndicator() }
            QuizUiState.NotEnoughQuestions -> CenteredBox(padding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("There aren't enough valid questions for this quiz yet.")
                    Button(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) { Text("Back to Home") }
                }
            }
            is QuizUiState.Active -> ActiveQuiz(padding, state, viewModel::selectOption, viewModel::onNext)
            is QuizUiState.Completed -> CenteredBox(padding) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun CenteredBox(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ActiveQuiz(
    padding: PaddingValues,
    state: QuizUiState.Active,
    onSelectOption: (String) -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Question ${state.questionIndex + 1} of ${state.totalQuestions}", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { (state.questionIndex + 1) / state.totalQuestions.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(state.question.prompt, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))

        for ((index, option) in state.options.withIndex()) {
            val selected = option.id == state.selectedOptionId
            val modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("quiz_option_$index")
            if (selected) {
                Button(onClick = { onSelectOption(option.id) }, modifier = modifier) {
                    Text(option.text)
                }
            } else {
                OutlinedButton(onClick = { onSelectOption(option.id) }, modifier = modifier) {
                    Text(option.text)
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = state.selectedOptionId != null,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).testTag("quiz_next_button"),
        ) {
            Text("Next")
        }
    }
}
