package fyi.appy.inksend.giladkutiel.ui.handwriting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import fyi.appy.inksend.giladkutiel.font.REQUIRED_GLYPH_CHARACTERS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Covers the "Unskippable ads while creating the font" scenario from the build spec's test plan:
 * drawing and saving all 62 glyphs back-to-back must never show a forced-wait or ad screen, and
 * must complete in well under 60 seconds — unlike the incumbent's reported 90+ second ad every
 * 3-5 letters. There is no ad SDK integrated in this app at all (see the final build report), so
 * this test also asserts zero Activities/Intents are launched during the whole sequence, which is
 * the strongest available proof that nothing ad-like can interrupt it.
 */
@RunWith(AndroidJUnit4::class)
class HandwritingFontAdFreeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun drawingAndSavingAll62Glyphs_showsNoAdOrForcedWaitScreen_andFinishesQuickly() {
        val done = AtomicBoolean(false)
        val startMillis = System.currentTimeMillis()

        composeTestRule.setContent {
            HandwritingFontCreatorScreen(onDone = { done.set(true) })
        }

        repeat(REQUIRED_GLYPH_CHARACTERS.size) {
            composeTestRule.onNodeWithTag(HandwritingTestTags.GLYPH_CANVAS).performTouchInput {
                swipe(start = Offset(center.x - 40f, center.y), end = Offset(center.x + 40f, center.y))
            }
            composeTestRule.onNodeWithTag(HandwritingTestTags.SAVE_GLYPH_BUTTON).performClick()
            composeTestRule.onNodeWithTag(HandwritingTestTags.CONTINUE_BUTTON).performClick()
        }

        composeTestRule.onNodeWithTag(HandwritingTestTags.SAVE_FONT_BUTTON).performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) { done.get() }

        val elapsedMillis = System.currentTimeMillis() - startMillis
        assertTrue("expected well under 60s, took ${elapsedMillis}ms", elapsedMillis < 60_000)
        assertEquals(0, Intents.getIntents().size)
    }
}
