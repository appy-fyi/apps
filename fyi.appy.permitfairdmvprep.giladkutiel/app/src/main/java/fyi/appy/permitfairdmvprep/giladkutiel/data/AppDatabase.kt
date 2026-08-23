package fyi.appy.permitfairdmvprep.giladkutiel.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StatePackEntity::class,
        LessonEntity::class,
        QuestionEntity::class,
        AnswerOptionEntity::class,
        UserPreferenceEntity::class,
        LessonProgressEntity::class,
        QuizAttemptEntity::class,
        QuizAnswerEntity::class,
        EntitlementCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statePackDao(): StatePackDao
    abstract fun lessonDao(): LessonDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerOptionDao(): AnswerOptionDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun quizAttemptDao(): QuizAttemptDao
    abstract fun quizAnswerDao(): QuizAnswerDao
    abstract fun entitlementCacheDao(): EntitlementCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "permitfair.db",
                ).build().also { instance = it }
            }
    }
}
