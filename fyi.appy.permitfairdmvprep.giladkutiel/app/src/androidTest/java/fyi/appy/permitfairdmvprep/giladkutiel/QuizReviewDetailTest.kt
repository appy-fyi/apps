package fyi.appy.permitfairdmvprep.giladkutiel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the appy build-spec test_plan scenario "baseline parity": Quiz Results must show the
 * total score plus a review row (selected answer, correct answer, explanation) for every
 * question in the attempt. Assumes a fresh install / cleared app data. The bundled California
 * pack gives each lesson exactly 7 questions, so a lesson quiz (capped at 10) uses all 7.
 */
@RunWith(AndroidJUnit4::class)
class QuizReviewDetailTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val questionsInFirstLesson = 7

    @Test
    fun resultsShowsAReviewRowForEveryQuestionInTheLessonQuiz() {
        composeTestRule.waitForTag("state_card_california")
        composeTestRule.onNodeWithTag("state_card_california").performClick()
        composeTestRule.onNodeWithTag("continue_button").performClick()

        composeTestRule.waitForTag("lesson_card")
        composeTestRule.onAllNodesWithTag("lesson_card")[0].performClick()
        composeTestRule.waitForTag("start_lesson_quiz_button")
        composeTestRule.onNodeWithTag("start_lesson_quiz_button").performClick()
        composeTestRule.waitForTag("quiz_option_0")

        repeat(questionsInFirstLesson) { questionNumber ->
            // Deliberately alternate answer choices so at least one incorrect answer is likely.
            val optionIndex = if (questionNumber % 2 == 0) 0 else 1
            composeTestRule.onNodeWithTag("quiz_option_$optionIndex").performClick()
            composeTestRule.onNodeWithTag("quiz_next_button").performClick()
        }

        composeTestRule.waitForText("Quiz Results")
        composeTestRule.onNodeWithText("Quiz Results").assertIsDisplayed()

        val correctAnswerLabels = composeTestRule.onAllNodesWithText("Correct answer:", substring = true).fetchSemanticsNodes()
        val yourAnswerLabels = composeTestRule.onAllNodesWithText("Your answer:", substring = true).fetchSemanticsNodes()

        assertEquals(questionsInFirstLesson, correctAnswerLabels.size)
        assertEquals(questionsInFirstLesson, yourAnswerLabels.size)
    }
}
