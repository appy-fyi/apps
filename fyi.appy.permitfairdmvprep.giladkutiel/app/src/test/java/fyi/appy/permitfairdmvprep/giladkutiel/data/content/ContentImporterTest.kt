package fyi.appy.permitfairdmvprep.giladkutiel.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fyi.appy.permitfairdmvprep.giladkutiel.data.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Pinned to SDK 34: the bundled Robolectric 4.13 doesn't yet know SDK 35 (this app's targetSdk).
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ContentImporterTest {
    private lateinit var db: AppDatabase
    private lateinit var importer: ContentImporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        importer = ContentImporter(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun packJson(
        stateId: String,
        lessonId: String,
        secondCorrectOption: Boolean = false,
    ): String = """
        {
          "statePack": {
            "stateId": "$stateId",
            "displayName": "State $stateId",
            "handbookTitle": "Handbook $stateId",
            "handbookSourceUrl": "https://example.gov/$stateId-handbook",
            "version": "1.0.0"
          },
          "lessons": [
            { "id": "$lessonId", "stateId": "$stateId", "title": "Lesson", "handbookSection": "Ch 1", "bodyMarkdown": "Body text.", "sortOrder": 0 }
          ],
          "questions": [
            { "id": "${lessonId}_q1", "stateId": "$stateId", "lessonId": "$lessonId", "prompt": "Prompt?", "explanation": "Because.", "handbookSection": "Ch 1", "sortOrder": 0 }
          ],
          "answerOptions": [
            { "id": "${lessonId}_q1_opt1", "questionId": "${lessonId}_q1", "text": "A", "isCorrect": true, "sortOrder": 0 },
            { "id": "${lessonId}_q1_opt2", "questionId": "${lessonId}_q1", "text": "B", "isCorrect": $secondCorrectOption, "sortOrder": 1 },
            { "id": "${lessonId}_q1_opt3", "questionId": "${lessonId}_q1", "text": "C", "isCorrect": false, "sortOrder": 2 },
            { "id": "${lessonId}_q1_opt4", "questionId": "${lessonId}_q1", "text": "D", "isCorrect": false, "sortOrder": 3 }
          ]
        }
    """.trimIndent()

    @Test
    fun `valid packs import while a pack with two correct options is rejected without partial rows`() = runTest {
        val manifestJson = """
            {
              "manifestVersion": "1.0.0",
              "statePacks": [
                { "stateId": "state_a", "displayName": "State A", "handbookTitle": "Handbook A", "handbookSourceUrl": "https://example.gov/a", "version": "1.0.0" },
                { "stateId": "state_b", "displayName": "State B", "handbookTitle": "Handbook B", "handbookSourceUrl": "https://example.gov/b", "version": "1.0.0" },
                { "stateId": "state_c", "displayName": "State C", "handbookTitle": "Handbook C", "handbookSourceUrl": "https://example.gov/c", "version": "1.0.0" }
              ]
            }
        """.trimIndent()

        val packs = mapOf(
            "state_a" to packJson("state_a", "lesson_a"),
            "state_b" to packJson("state_b", "lesson_b"),
            "state_c" to packJson("state_c", "lesson_c", secondCorrectOption = true),
        )

        val result = importer.importManifest(manifestJson) { stateId -> requireNotNull(packs[stateId]) }

        assertEquals(setOf("state_a", "state_b"), result.importedStateIds.toSet())
        assertEquals(setOf("state_c"), result.failedStateIds.toSet())

        val stateAPack = db.statePackDao().getById("state_a")
        assertEquals("https://example.gov/state_a-handbook", stateAPack?.handbookSourceUrl)
        val stateBPack = db.statePackDao().getById("state_b")
        assertEquals("https://example.gov/state_b-handbook", stateBPack?.handbookSourceUrl)

        // The invalid pack contributes zero rows.
        assertTrue(db.lessonDao().getForState("state_c").isEmpty())
        assertTrue(db.questionDao().getForState("state_c").isEmpty())
        assertEquals(null, db.statePackDao().getById("state_c"))

        for (stateId in listOf("state_a", "state_b")) {
            val questions = db.questionDao().getForState(stateId)
            assertEquals(1, questions.size)
            for (question in questions) {
                val options = db.answerOptionDao().getForQuestion(question.id)
                assertEquals(4, options.size)
                assertEquals(1, options.count { it.isCorrect })
            }
        }
    }
}
