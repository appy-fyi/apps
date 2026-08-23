package fyi.appy.permitfairdmvprep.giladkutiel

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the appy build-spec test_plan scenario "Paywall after 3-5 questions": a learner must
 * be able to finish one full 25-question practice test with no paywall.
 *
 * FeatureFlags.ALL_FEATURES_FREE currently bypasses the paywall entirely (see that file), so a
 * second practice test also starts freely rather than routing to Unlock — the billing/entitlement
 * code these tests exercised when the flag is off (BillingProductConfigTest still covers the
 * product config itself) stays in place for when it's turned back on.
 *
 * Assumes a fresh install / cleared app data, exactly as the test_plan's own first step
 * requires (run the instrumentation with `-e clearPackageData true`).
 */
@RunWith(AndroidJUnit4::class)
class PaywallFlowTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstFreePracticeTestReachesResultsWithNoPaywall() {
        selectFirstStateAndContinue()
        composeTestRule.onNodeWithTag("start_practice_test_button").performClick()
        composeTestRule.waitForTag("quiz_option_0")

        repeat(25) { questionNumber ->
            composeTestRule.onNodeWithText("Question ${questionNumber + 1} of 25").assertIsDisplayed()
            composeTestRule.onNodeWithText("One-time lifetime unlock").assertDoesNotExist()
            composeTestRule.onNodeWithTag("quiz_option_0").performClick()
            composeTestRule.onNodeWithTag("quiz_next_button").performClick()
        }

        composeTestRule.waitForText("Quiz Results")
        composeTestRule.onNodeWithText("Quiz Results").assertIsDisplayed()
    }

    @Test
    fun secondPracticeTestAttemptStartsFreelyWhileAllFeaturesAreFree() {
        selectFirstStateAndContinue()
        completeAPracticeTest()

        composeTestRule.onNodeWithText("Back to Home").performClick()
        composeTestRule.waitForTag("start_practice_test_button")
        composeTestRule.onNodeWithTag("start_practice_test_button").performClick()

        composeTestRule.waitForText("Question 1 of 25")
        composeTestRule.onNodeWithText("Question 1 of 25").assertIsDisplayed()
        composeTestRule.onNodeWithText("One-time lifetime unlock").assertDoesNotExist()
    }

    private fun selectFirstStateAndContinue() {
        composeTestRule.waitForTag("state_card_california")
        composeTestRule.onNodeWithTag("state_card_california").performClick()
        composeTestRule.onNodeWithTag("continue_button").performClick()
        composeTestRule.waitForTag("start_practice_test_button")
    }

    private fun completeAPracticeTest() {
        composeTestRule.onNodeWithTag("start_practice_test_button").performClick()
        composeTestRule.waitForTag("quiz_option_0")
        repeat(25) {
            composeTestRule.onNodeWithTag("quiz_option_0").performClick()
            composeTestRule.onNodeWithTag("quiz_next_button").performClick()
        }
        composeTestRule.waitForText("Quiz Results")
        composeTestRule.onNodeWithText("Quiz Results").assertIsDisplayed()
    }
}
