package fyi.appy.permitfairdmvprep.giladkutiel.repository

import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.data.LessonProgressEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.PreferenceKeys
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizAnswerEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizAttemptEntity
import fyi.appy.permitfairdmvprep.giladkutiel.data.QuizMode
import fyi.appy.permitfairdmvprep.giladkutiel.data.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed interface AttemptStartResult {
    data class Started(val attempt: QuizAttemptEntity) : AttemptStartResult
    data object RequiresUnlock : AttemptStartResult
}

/**
 * Tracks selected state, lesson completion, quiz attempts, and free-test eligibility, all
 * on-device (appy build-spec feature "Local progress tracking").
 */
class ProgressRepository(private val db: AppDatabase) {
    private val freeAccessPolicy = FreeAccessPolicy(db)
    private val quizAttemptFactory = QuizAttemptFactory(db)

    fun observeSelectedStateId(): Flow<String?> =
        db.userPreferenceDao().observe(PreferenceKeys.SELECTED_STATE_ID).map { it?.value }

    suspend fun getSelectedStateId(): String? = db.userPreferenceDao().get(PreferenceKeys.SELECTED_STATE_ID)?.value

    suspend fun setSelectedState(stateId: String) {
        db.userPreferenceDao().upsert(UserPreferenceEntity(PreferenceKeys.SELECTED_STATE_ID, stateId))
    }

    fun observeLessonProgress(lessonIds: List<String>): Flow<List<LessonProgressEntity>> =
        db.lessonProgressDao().observeForLessons(lessonIds)

    suspend fun getLessonProgress(lessonId: String): LessonProgressEntity? = db.lessonProgressDao().getById(lessonId)

    suspend fun markLessonRead(lessonId: String) {
        db.lessonProgressDao().upsert(LessonProgressEntity(lessonId, readCompleted = true, lastOpenedAtEpochMillis = System.currentTimeMillis()))
    }

    suspend fun touchLessonOpened(lessonId: String) {
        val existing = db.lessonProgressDao().getById(lessonId)
        db.lessonProgressDao().upsert(
            existing?.copy(lastOpenedAtEpochMillis = System.currentTimeMillis())
                ?: LessonProgressEntity(lessonId, readCompleted = false, lastOpenedAtEpochMillis = System.currentTimeMillis()),
        )
    }

    fun observeAttemptsForState(stateId: String): Flow<List<QuizAttemptEntity>> = db.quizAttemptDao().observeForState(stateId)

    suspend fun getAttemptsForState(stateId: String): List<QuizAttemptEntity> = db.quizAttemptDao().getForState(stateId)

    fun observeCompletedPracticeTestCount(stateId: String): Flow<Int> = db.quizAttemptDao().observeCompletedPracticeTestCount(stateId)

    fun observeAttempt(attemptId: String): Flow<QuizAttemptEntity?> = db.quizAttemptDao().observeById(attemptId)

    suspend fun getAttempt(attemptId: String): QuizAttemptEntity? = db.quizAttemptDao().getById(attemptId)

    fun observeAnswersForAttempt(attemptId: String): Flow<List<QuizAnswerEntity>> = db.quizAnswerDao().observeForAttempt(attemptId)

    suspend fun getAnswersForAttempt(attemptId: String): List<QuizAnswerEntity> = db.quizAnswerDao().getForAttempt(attemptId)

    suspend fun freeAccessState() = freeAccessPolicy.currentState()

    suspend fun startPracticeTest(stateId: String): AttemptStartResult {
        val accessState = freeAccessPolicy.currentState()
        if (accessState == FreeAccessState.AFTER_FREE_TEST_COMPLETED) return AttemptStartResult.RequiresUnlock

        val attemptId = UUID.randomUUID().toString()
        val questionIds = quizAttemptFactory.buildPracticeTestQuestionIds(stateId, attemptId)
        val attempt = QuizAttemptEntity(
            id = attemptId,
            stateId = stateId,
            lessonId = null,
            quizMode = QuizMode.PRACTICE_TEST,
            questionIdsCsv = questionIds.joinToString(","),
            startedAtEpochMillis = System.currentTimeMillis(),
            completedAtEpochMillis = 0,
            scoreCorrect = 0,
            scoreTotal = questionIds.size,
            wasFreeFullPracticeTest = accessState == FreeAccessState.BEFORE_FREE_TEST_COMPLETED,
        )
        db.quizAttemptDao().upsert(attempt)
        return AttemptStartResult.Started(attempt)
    }

    suspend fun startLessonQuiz(stateId: String, lessonId: String): AttemptStartResult {
        val accessState = freeAccessPolicy.currentState()
        if (accessState == FreeAccessState.AFTER_FREE_TEST_COMPLETED) return AttemptStartResult.RequiresUnlock

        val attemptId = UUID.randomUUID().toString()
        val questionIds = quizAttemptFactory.buildLessonQuizQuestionIds(stateId, lessonId, attemptId)
        val attempt = QuizAttemptEntity(
            id = attemptId,
            stateId = stateId,
            lessonId = lessonId,
            quizMode = QuizMode.LESSON_QUIZ,
            questionIdsCsv = questionIds.joinToString(","),
            startedAtEpochMillis = System.currentTimeMillis(),
            completedAtEpochMillis = 0,
            scoreCorrect = 0,
            scoreTotal = questionIds.size,
            wasFreeFullPracticeTest = false,
        )
        db.quizAttemptDao().upsert(attempt)
        return AttemptStartResult.Started(attempt)
    }

    suspend fun recordAnswer(attemptId: String, questionId: String, selectedAnswerOptionId: String, isCorrect: Boolean) {
        db.quizAnswerDao().upsert(
            QuizAnswerEntity(
                attemptId = attemptId,
                questionId = questionId,
                selectedAnswerOptionId = selectedAnswerOptionId,
                answeredAtEpochMillis = System.currentTimeMillis(),
                isCorrect = isCorrect,
            ),
        )
    }

    suspend fun completeAttempt(attemptId: String) {
        val attempt = db.quizAttemptDao().getById(attemptId) ?: return
        val answers = db.quizAnswerDao().getForAttempt(attemptId)
        db.quizAttemptDao().upsert(
            attempt.copy(
                completedAtEpochMillis = System.currentTimeMillis(),
                scoreCorrect = answers.count { it.isCorrect },
            ),
        )
    }
}
