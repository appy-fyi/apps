package fyi.appy.permitfairdmvprep.giladkutiel.repository

import fyi.appy.permitfairdmvprep.giladkutiel.FeatureFlags
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import fyi.appy.permitfairdmvprep.giladkutiel.data.ProductIds
import kotlin.random.Random

enum class FreeAccessState {
    BEFORE_FREE_TEST_COMPLETED,
    AFTER_FREE_TEST_COMPLETED,
    LIFETIME_UNLOCKED,
}

/**
 * Implements the appy build-spec feature "First complete practice test before paywall":
 * a learner without the lifetime entitlement gets exactly one full free practice test before
 * Unlock is ever shown.
 */
class FreeAccessPolicy(private val db: AppDatabase) {
    suspend fun currentState(): FreeAccessState {
        if (FeatureFlags.ALL_FEATURES_FREE) return FreeAccessState.LIFETIME_UNLOCKED
        val entitlement = db.entitlementCacheDao().get(ProductIds.LIFETIME_UNLOCK)
        if (entitlement?.isOwned == true) return FreeAccessState.LIFETIME_UNLOCKED
        val completedFree = db.quizAttemptDao().getCompletedFreeFullPracticeTest()
        return if (completedFree != null) {
            FreeAccessState.AFTER_FREE_TEST_COMPLETED
        } else {
            FreeAccessState.BEFORE_FREE_TEST_COMPLETED
        }
    }

    suspend fun canStartWithoutUnlock(): Boolean = currentState() != FreeAccessState.AFTER_FREE_TEST_COMPLETED
}

/** Builds the deterministic, per-attempt question selection for practice tests and lesson quizzes. */
class QuizAttemptFactory(private val db: AppDatabase) {
    suspend fun buildPracticeTestQuestionIds(stateId: String, attemptId: String): List<String> {
        val questionIds = db.questionDao().getForState(stateId).map { it.id }
        return seededShuffle(seedFor(stateId, attemptId), questionIds).take(PRACTICE_TEST_SIZE)
    }

    suspend fun buildLessonQuizQuestionIds(stateId: String, lessonId: String, attemptId: String): List<String> {
        val questionIds = db.questionDao().getForLesson(lessonId).map { it.id }
        return seededShuffle(seedFor(stateId, attemptId), questionIds).take(LESSON_QUIZ_SIZE)
    }

    private fun seedFor(stateId: String, attemptId: String): String =
        stateId + currentEpochDay() + attemptId

    companion object {
        const val PRACTICE_TEST_SIZE = 25
        const val LESSON_QUIZ_SIZE = 10
    }
}

fun currentEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

fun seededShuffle(seed: String, items: List<String>): List<String> =
    items.shuffled(Random(seed.hashCode().toLong()))
