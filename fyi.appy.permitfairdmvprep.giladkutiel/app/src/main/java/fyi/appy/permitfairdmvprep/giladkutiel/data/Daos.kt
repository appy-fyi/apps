package fyi.appy.permitfairdmvprep.giladkutiel.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StatePackDao {
    @Query("SELECT * FROM state_packs ORDER BY displayName ASC")
    fun observeAll(): Flow<List<StatePackEntity>>

    @Query("SELECT * FROM state_packs ORDER BY displayName ASC")
    suspend fun getAllOnce(): List<StatePackEntity>

    @Query("SELECT * FROM state_packs WHERE stateId = :stateId")
    fun observeById(stateId: String): Flow<StatePackEntity?>

    @Query("SELECT * FROM state_packs WHERE stateId = :stateId")
    suspend fun getById(stateId: String): StatePackEntity?

    @Upsert
    suspend fun upsert(statePack: StatePackEntity)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE stateId = :stateId ORDER BY sortOrder ASC")
    fun observeForState(stateId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE stateId = :stateId ORDER BY sortOrder ASC")
    suspend fun getForState(stateId: String): List<LessonEntity>

    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getById(lessonId: String): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("DELETE FROM lessons WHERE stateId = :stateId")
    suspend fun deleteForState(stateId: String)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE lessonId = :lessonId ORDER BY sortOrder ASC")
    suspend fun getForLesson(lessonId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE stateId = :stateId ORDER BY sortOrder ASC")
    suspend fun getForState(stateId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id IN (:questionIds)")
    suspend fun getByIds(questionIds: List<String>): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE stateId = :stateId")
    suspend fun countForState(stateId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE stateId = :stateId")
    suspend fun deleteForState(stateId: String)
}

@Dao
interface AnswerOptionDao {
    @Query("SELECT * FROM answer_options WHERE questionId = :questionId ORDER BY sortOrder ASC")
    suspend fun getForQuestion(questionId: String): List<AnswerOptionEntity>

    @Query("SELECT * FROM answer_options WHERE questionId IN (:questionIds) ORDER BY sortOrder ASC")
    suspend fun getForQuestions(questionIds: List<String>): List<AnswerOptionEntity>

    @Query("SELECT * FROM answer_options WHERE id = :id")
    suspend fun getById(id: String): AnswerOptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(options: List<AnswerOptionEntity>)

    @Query("DELETE FROM answer_options WHERE questionId IN (SELECT id FROM questions WHERE stateId = :stateId)")
    suspend fun deleteForState(stateId: String)
}

@Dao
interface UserPreferenceDao {
    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    fun observe(key: String): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    suspend fun get(key: String): UserPreferenceEntity?

    @Upsert
    suspend fun upsert(preference: UserPreferenceEntity)
}

@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE lessonId IN (:lessonIds)")
    fun observeForLessons(lessonIds: List<String>): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :lessonId")
    suspend fun getById(lessonId: String): LessonProgressEntity?

    @Upsert
    suspend fun upsert(progress: LessonProgressEntity)
}

@Dao
interface QuizAttemptDao {
    @Query("SELECT * FROM quiz_attempts WHERE id = :id")
    suspend fun getById(id: String): QuizAttemptEntity?

    @Query("SELECT * FROM quiz_attempts WHERE id = :id")
    fun observeById(id: String): Flow<QuizAttemptEntity?>

    @Query("SELECT * FROM quiz_attempts WHERE stateId = :stateId ORDER BY startedAtEpochMillis DESC")
    fun observeForState(stateId: String): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts WHERE stateId = :stateId ORDER BY startedAtEpochMillis DESC")
    suspend fun getForState(stateId: String): List<QuizAttemptEntity>

    @Query(
        "SELECT * FROM quiz_attempts WHERE wasFreeFullPracticeTest = 1 AND completedAtEpochMillis > 0 LIMIT 1",
    )
    suspend fun getCompletedFreeFullPracticeTest(): QuizAttemptEntity?

    @Query(
        "SELECT COUNT(*) FROM quiz_attempts WHERE stateId = :stateId AND quizMode = 'PRACTICE_TEST' AND completedAtEpochMillis > 0",
    )
    fun observeCompletedPracticeTestCount(stateId: String): Flow<Int>

    @Upsert
    suspend fun upsert(attempt: QuizAttemptEntity)
}

@Dao
interface QuizAnswerDao {
    @Upsert
    suspend fun upsert(answer: QuizAnswerEntity)

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId")
    suspend fun getForAttempt(attemptId: String): List<QuizAnswerEntity>

    @Query("SELECT * FROM quiz_answers WHERE attemptId = :attemptId")
    fun observeForAttempt(attemptId: String): Flow<List<QuizAnswerEntity>>

    @Query("SELECT COUNT(*) FROM quiz_answers WHERE attemptId = :attemptId")
    suspend fun countForAttempt(attemptId: String): Int
}

@Dao
interface EntitlementCacheDao {
    @Query("SELECT * FROM entitlement_cache WHERE productId = :productId")
    fun observe(productId: String): Flow<EntitlementCacheEntity?>

    @Query("SELECT * FROM entitlement_cache WHERE productId = :productId")
    suspend fun get(productId: String): EntitlementCacheEntity?

    @Upsert
    suspend fun upsert(entitlement: EntitlementCacheEntity)
}
