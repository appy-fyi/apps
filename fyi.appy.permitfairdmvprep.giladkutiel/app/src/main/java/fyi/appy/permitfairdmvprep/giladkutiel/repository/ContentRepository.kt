package fyi.appy.permitfairdmvprep.giladkutiel.repository

import android.content.Context
import fyi.appy.permitfairdmvprep.giladkutiel.data.AnswerOptionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.data.LessonEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuestionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.StatePackEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.content.ContentImporter
import fyi.appy.permitfairdmvprep.giladkutiel.data.content.ManifestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.IOException

data class StatePackSummary(
    val stateId: String,
    val displayName: String,
    val handbookTitle: String,
    val lessonCount: Int,
    val questionCount: Int,
)

sealed interface ContentLoadState {
    data object Loading : ContentLoadState
    data object Empty : ContentLoadState
    data class Error(val message: String) : ContentLoadState
    data class Loaded(val states: List<StatePackSummary>) : ContentLoadState
}

/**
 * Owns bundled public-handbook content: reads assets/content on first launch or whenever a
 * pack's manifest version changes (appy build-spec feature "Bundled public-handbook
 * launch-state content"), then exposes it from Room.
 */
class ContentRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val importer = ContentImporter(db)
    private val json = Json { ignoreUnknownKeys = true }

    private val _loadState = MutableStateFlow<ContentLoadState>(ContentLoadState.Loading)
    val loadState: StateFlow<ContentLoadState> = _loadState.asStateFlow()

    suspend fun ensureContentImported() {
        _loadState.value = ContentLoadState.Loading
        val manifestJson = try {
            readAsset("content/manifest.json")
        } catch (e: IOException) {
            _loadState.value = ContentLoadState.Empty
            return
        }

        val manifest = try {
            json.decodeFromString(ManifestDto.serializer(), manifestJson)
        } catch (e: Exception) {
            _loadState.value = ContentLoadState.Error("invalid content manifest")
            return
        }

        if (manifest.statePacks.isEmpty()) {
            _loadState.value = ContentLoadState.Empty
            return
        }

        val staleEntries = manifest.statePacks.filter { entry ->
            val existing = db.statePackDao().getById(entry.stateId)
            existing == null || existing.version != entry.version
        }

        if (staleEntries.isNotEmpty()) {
            val filteredManifestJson = json.encodeToString(
                ManifestDto.serializer(),
                manifest.copy(statePacks = staleEntries),
            )
            val result = importer.importManifest(filteredManifestJson) { stateId ->
                readAsset("content/$stateId/pack.json")
            }
            if (result.manifestInvalid) {
                _loadState.value = ContentLoadState.Error("invalid content manifest")
                return
            }
        }

        refreshLoadedState()
    }

    private suspend fun refreshLoadedState() {
        val states = db.statePackDao().getAllOnce()
        if (states.isEmpty()) {
            _loadState.value = ContentLoadState.Error("no valid content packs available")
            return
        }
        val summaries = states.map { pack ->
            StatePackSummary(
                stateId = pack.stateId,
                displayName = pack.displayName,
                handbookTitle = pack.handbookTitle,
                lessonCount = db.lessonDao().getForState(pack.stateId).size,
                questionCount = db.questionDao().countForState(pack.stateId),
            )
        }
        _loadState.value = ContentLoadState.Loaded(summaries)
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    fun observeStatePack(stateId: String): Flow<StatePackEntity?> = db.statePackDao().observeById(stateId)

    suspend fun getStatePack(stateId: String): StatePackEntity? = db.statePackDao().getById(stateId)

    fun observeLessons(stateId: String): Flow<List<LessonEntity>> = db.lessonDao().observeForState(stateId)

    suspend fun getLessonsForState(stateId: String): List<LessonEntity> = db.lessonDao().getForState(stateId)

    suspend fun getLesson(lessonId: String): LessonEntity? = db.lessonDao().getById(lessonId)

    suspend fun getQuestionsForLesson(lessonId: String): List<QuestionEntity> = db.questionDao().getForLesson(lessonId)

    suspend fun getQuestionsForState(stateId: String): List<QuestionEntity> = db.questionDao().getForState(stateId)

    suspend fun getQuestionsByIds(questionIds: List<String>): List<QuestionEntity> =
        db.questionDao().getByIds(questionIds)

    suspend fun getOptionsForQuestions(questionIds: List<String>): List<AnswerOptionEntity> =
        db.answerOptionDao().getForQuestions(questionIds)

    suspend fun getOptionsForQuestion(questionId: String): List<AnswerOptionEntity> =
        db.answerOptionDao().getForQuestion(questionId)
}
