package fyi.appy.permitfairdmvprep.giladkutiel

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the appy build-spec test_plan scenario "baseline parity" — lesson completion survives
 * an Activity recreation, since Room persists LessonProgress to disk on every write rather than
 * keeping it only in memory. Assumes a fresh install / cleared app data.
 */
@RunWith(AndroidJUnit4::class)
class LessonProgressPersistenceTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonStaysMarkedReadAfterActivityRecreation() {
        composeTestRule.waitForTag("state_card_california")
        composeTestRule.onNodeWithTag("state_card_california").performClick()
        composeTestRule.onNodeWithTag("continue_button").performClick()

        composeTestRule.waitForTag("lesson_card")
        composeTestRule.onAllNodesWithTag("lesson_card")[0].performClick()
        composeTestRule.waitForTag("mark_read_button")
        composeTestRule.onNodeWithTag("mark_read_button").performClick()
        composeTestRule.onNodeWithTag("mark_read_button").assertDoesNotExist()

        // Simulate reopening the app: recreate the Activity, backed by the same on-disk Room database.
        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.waitForTag("lesson_card")
        composeTestRule.onAllNodesWithTag("lesson_card")[0].performClick()
        composeTestRule.waitForTag("start_lesson_quiz_button")
        composeTestRule.onNodeWithTag("mark_read_button").assertDoesNotExist()
    }
}
