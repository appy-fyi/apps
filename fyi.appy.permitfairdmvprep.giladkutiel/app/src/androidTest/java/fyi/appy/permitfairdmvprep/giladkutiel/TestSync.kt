package fyi.appy.permitfairdmvprep.giladkutiel

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText

/**
 * Screen state loads asynchronously off the Compose clock (Room I/O), so plain performClick()
 * synchronization isn't enough between navigation steps — wait for the next screen's content.
 */
fun ComposeTestRule.waitForTag(tag: String, timeoutMillis: Long = 15_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
}

fun ComposeTestRule.waitForText(text: String, timeoutMillis: Long = 15_000) {
    waitUntil(timeoutMillis = timeoutMillis) {
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }
}
