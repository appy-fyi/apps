package fyi.appy.permitfairdmvprep.giladkutiel.ui.screens.results

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ContentRepository
import fyi.appy.permitfairdmvprep.giladkutiel.repository.FreeAccessState
import fyi.appy.permitfairdmvprep.giladkutiel.repository.ProgressRepository
import fyi.appy.permitfairdmvprep.giladkutiel.ui.util.requestInAppReviewIfPossible
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewRow(
    val prompt: String,
    val handbookSection: String,
    val explanation: String,
    val selectedText: String,
    val correctText: String,
    val isCorrect: Boolean,
)

sealed interface QuizResultsUiState {
    data object Loading : QuizResultsUiState
    data object NotFound : QuizResultsUiState
    data class Populated(
        val scoreCorrect: Int,
        val scoreTotal: Int,
        val reviewRows: List<ReviewRow>,
        val showUnlockCard: Boolean,
    ) : QuizResultsUiState
}

class QuizResultsViewModel(
    private val attemptId: String,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<QuizResultsUiState>(QuizResultsUiState.Loading)
    val uiState: StateFlow<QuizResultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val attempt = progressRepository.getAttempt(attemptId)
            if (attempt == null) {
                _uiState.value = QuizResultsUiState.NotFound
                return@launch
            }
            val answers = progressRepository.getAnswersForAttempt(attemptId)
            val questionIds = attempt.questionIdsCsv.split(",").filter { it.isNotBlank() }
            val questions = contentRepository.getQuestionsByIds(questionIds).associateBy { it.id }
            val answersByQuestion = answers.associateBy { it.questionId }

            val rows = questionIds.mapNotNull { questionId ->
                val question = questions[questionId] ?: return@mapNotNull null
                val options = contentRepository.getOptionsForQuestion(questionId)
                val answer = answersByQuestion[questionId]
                val selected = options.firstOrNull { it.id == answer?.selectedAnswerOptionId }
                val correct = options.firstOrNull { it.isCorrect }
                ReviewRow(
                    prompt = question.prompt,
                    handbookSection = question.handbookSection,
                    explanation = question.explanation,
                    selectedText = selected?.text ?: "Not answered",
                    correctText = correct?.text ?: "",
                    isCorrect = answer?.isCorrect == true,
                )
            }

            val showUnlockCard = attempt.wasFreeFullPracticeTest &&
                attempt.completedAtEpochMillis > 0 &&
                progressRepository.freeAccessState() != FreeAccessState.LIFETIME_UNLOCKED

            _uiState.value = QuizResultsUiState.Populated(
                scoreCorrect = attempt.scoreCorrect,
                scoreTotal = attempt.scoreTotal,
                reviewRows = rows,
                showUnlockCard = showUnlockCard,
            )
        }
    }

    companion object {
        fun factory(attemptId: String, contentRepository: ContentRepository, progressRepository: ProgressRepository) = viewModelFactory {
            initializer { QuizResultsViewModel(attemptId, contentRepository, progressRepository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultsScreen(
    attemptId: String,
    contentRepository: ContentRepository,
    progressRepository: ProgressRepository,
    onNavigateToUnlock: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val viewModel: QuizResultsViewModel = viewModel(
        factory = QuizResultsViewModel.factory(attemptId, contentRepository, progressRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        if (uiState is QuizResultsUiState.Populated) {
            (context as? Activity)?.let { requestInAppReviewIfPossible(it) }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Quiz Results") }) }) { padding ->
        when (val state = uiState) {
            QuizResultsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            QuizResultsUiState.NotFound -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("This attempt could not be found.")
                Button(onClick = onNavigateHome, modifier = Modifier.padding(top = 16.dp)) { Text("Back to Home") }
            }
            is QuizResultsUiState.Populated -> PopulatedResults(padding, state, onNavigateToUnlock, onNavigateHome)
        }
    }
}

@Composable
private fun PopulatedResults(
    padding: PaddingValues,
    state: QuizResultsUiState.Populated,
    onNavigateToUnlock: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    val percent = if (state.scoreTotal > 0) (state.scoreCorrect * 100 / state.scoreTotal) else 0
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("$percent%", style = MaterialTheme.typography.displayMedium)
        Text("${state.scoreCorrect} of ${state.scoreTotal} correct")

        if (state.showUnlockCard) {
            Card(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("You've completed your free full practice test.", style = MaterialTheme.typography.titleMedium)
                    Text("Unlock lifetime access for one $4.99 one-time purchase — no subscription.")
                    Button(onClick = onNavigateToUnlock, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Unlock lifetime access")
                    }
                }
            }
        }

        Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("Back to Home")
        }

        Text("Review", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.reviewRows) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(row.prompt, style = MaterialTheme.typography.bodyLarge)
                        Text(row.handbookSection, style = MaterialTheme.typography.bodySmall)
                        Text("Your answer: ${row.selectedText}")
                        Text("Correct answer: ${row.correctText}")
                        Text(row.explanation, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
