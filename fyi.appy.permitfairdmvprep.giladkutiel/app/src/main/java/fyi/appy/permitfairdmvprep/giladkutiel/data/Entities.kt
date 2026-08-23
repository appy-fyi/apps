package fyi.appy.permitfairdmvprep.giladkutiel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "state_packs")
data class StatePackEntity(
    @PrimaryKey val stateId: String,
    val displayName: String,
    val handbookTitle: String,
    val handbookSourceUrl: String,
    val version: String,
    val importedAtEpochMillis: Long,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val stateId: String,
    val title: String,
    val handbookSection: String,
    val bodyMarkdown: String,
    val sortOrder: Int,
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val stateId: String,
    val lessonId: String,
    val prompt: String,
    val explanation: String,
    val handbookSection: String,
    val sortOrder: Int,
)

@Entity(tableName = "answer_options")
data class AnswerOptionEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val text: String,
    val isCorrect: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val readCompleted: Boolean,
    val lastOpenedAtEpochMillis: Long,
)

/** quizMode is one of [fyi.appy.permitfairdmvprep.giladkutiel.data.QuizMode]. */
@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val stateId: String,
    val lessonId: String?,
    val quizMode: String,
    val questionIdsCsv: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val scoreCorrect: Int,
    val scoreTotal: Int,
    val wasFreeFullPracticeTest: Boolean,
)

@Entity(tableName = "quiz_answers", primaryKeys = ["attemptId", "questionId"])
data class QuizAnswerEntity(
    val attemptId: String,
    val questionId: String,
    val selectedAnswerOptionId: String,
    val answeredAtEpochMillis: Long,
    val isCorrect: Boolean,
)

@Entity(tableName = "entitlement_cache")
data class EntitlementCacheEntity(
    @PrimaryKey val productId: String,
    val isOwned: Boolean,
    val purchaseTokenHash: String?,
    val updatedAtEpochMillis: Long,
)

object QuizMode {
    const val PRACTICE_TEST = "PRACTICE_TEST"
    const val LESSON_QUIZ = "LESSON_QUIZ"
}

object PreferenceKeys {
    const val SELECTED_STATE_ID = "selected_state_id"
}

object ProductIds {
    const val LIFETIME_UNLOCK = "lifetime_unlock"
}
