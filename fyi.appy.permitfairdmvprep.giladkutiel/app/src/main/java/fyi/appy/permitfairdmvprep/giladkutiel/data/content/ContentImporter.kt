package fyi.appy.permitfairdmvprep.giladkutiel.data.content

import androidx.room.withTransaction
import fyi.appy.permitfairdmvprep.giladkutiel.data.AnswerOptionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.data.LessonEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuestionEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.StatePackEntity
import kotlinx.serialization.json.Json

data class ContentImportResult(
    val importedStateIds: List<String>,
    val failedStateIds: List<String>,
    val manifestInvalid: Boolean = false,
)

/**
 * Validates and imports bundled public-handbook content packs into Room. A pack that fails
 * validation contributes zero rows for that state — see appy build-spec feature
 * "Bundled public-handbook launch-state content".
 */
class ContentImporter(private val db: AppDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun importManifest(
        manifestJson: String,
        loadPackJson: suspend (stateId: String) -> String,
    ): ContentImportResult {
        val manifest = try {
            json.decodeFromString(ManifestDto.serializer(), manifestJson)
        } catch (e: Exception) {
            return ContentImportResult(emptyList(), emptyList(), manifestInvalid = true)
        }

        val imported = mutableListOf<String>()
        val failed = mutableListOf<String>()
        for (entry in manifest.statePacks) {
            val pack = try {
                json.decodeFromString(PackDto.serializer(), loadPackJson(entry.stateId))
            } catch (e: Exception) {
                failed += entry.stateId
                continue
            }
            if (isValid(pack)) {
                importPack(pack)
                imported += entry.stateId
            } else {
                failed += entry.stateId
            }
        }
        return ContentImportResult(imported, failed, manifestInvalid = false)
    }

    private fun isValid(pack: PackDto): Boolean {
        if (pack.lessons.isEmpty() || pack.questions.isEmpty()) return false

        val lessonIds = pack.lessons.map { it.id }.toSet()
        if (lessonIds.size != pack.lessons.size) return false
        if (pack.lessons.any { it.title.isBlank() || it.bodyMarkdown.isBlank() }) return false

        val questionIds = pack.questions.map { it.id }.toSet()
        if (questionIds.size != pack.questions.size) return false

        val optionsByQuestion = pack.answerOptions.groupBy { it.questionId }
        for (question in pack.questions) {
            if (question.prompt.isBlank() || question.explanation.isBlank() || question.handbookSection.isBlank()) {
                return false
            }
            if (question.lessonId !in lessonIds) return false
            val options = optionsByQuestion[question.id] ?: return false
            if (options.size != 4) return false
            if (options.count { it.isCorrect } != 1) return false
            if (options.any { it.text.isBlank() }) return false
        }
        return true
    }

    private suspend fun importPack(pack: PackDto) {
        val stateId = pack.statePack.stateId
        db.withTransaction {
            db.statePackDao().upsert(
                StatePackEntity(
                    stateId = stateId,
                    displayName = pack.statePack.displayName,
                    handbookTitle = pack.statePack.handbookTitle,
                    handbookSourceUrl = pack.statePack.handbookSourceUrl,
                    version = pack.statePack.version,
                    importedAtEpochMillis = System.currentTimeMillis(),
                ),
            )

            // Children first, respecting the FK order, while never touching QuizAttempt,
            // QuizAnswer, LessonProgress, UserPreference, or EntitlementCache rows.
            db.answerOptionDao().deleteForState(stateId)
            db.questionDao().deleteForState(stateId)
            db.lessonDao().deleteForState(stateId)

            db.lessonDao().insertAll(
                pack.lessons.map {
                    LessonEntity(
                        id = it.id,
                        stateId = it.stateId,
                        title = it.title,
                        handbookSection = it.handbookSection,
                        bodyMarkdown = it.bodyMarkdown,
                        sortOrder = it.sortOrder,
                    )
                },
            )
            db.questionDao().insertAll(
                pack.questions.map {
                    QuestionEntity(
                        id = it.id,
                        stateId = it.stateId,
                        lessonId = it.lessonId,
                        prompt = it.prompt,
                        explanation = it.explanation,
                        handbookSection = it.handbookSection,
                        sortOrder = it.sortOrder,
                    )
                },
            )
            db.answerOptionDao().insertAll(
                pack.answerOptions.map {
                    AnswerOptionEntity(
                        id = it.id,
                        questionId = it.questionId,
                        text = it.text,
                        isCorrect = it.isCorrect,
                        sortOrder = it.sortOrder,
                    )
                },
            )
        }
    }
}
