package com.appyfyi.steadygridgallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import com.appyfyi.steadygridgallery.ui.screens.viewer.pinchZoomAndSwipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the todo.txt request "add an option to zoom in and out when pinching the image" by
 * exercising ViewerScreen's actual gesture-detection Modifier (pinchZoomAndSwipe) directly,
 * through Compose's real touch-input pipeline (performTouchInput), rather than through the full
 * app + MediaStore + navigation stack, which is unrelated flaky infrastructure in this
 * environment (the pre-existing EditorExportInstrumentedTest hits the same MediaStore-loading
 * race). This still validates the exact code path ViewerScreen wires up, just mounted directly.
 */
class ViewerZoomAndSwipeInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pinchOutIncreasesZoomScale() {
        val zoomScale = mutableStateOf(1f)
        val zoomOffset = mutableStateOf(Offset.Zero)

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("zoomTarget")
                    .pinchZoomAndSwipe(
                        key = Unit,
                        zoomScale = zoomScale,
                        zoomOffset = zoomOffset,
                        onSwipeDirectionChanged = {},
                        onSwipeNext = {},
                        onSwipePrevious = {},
                    ),
            )
        }

        composeTestRule.onNodeWithTag("zoomTarget").performTouchInput {
            pinch(
                start0 = center - Offset(80f, 0f),
                end0 = center - Offset(280f, 0f),
                start1 = center + Offset(80f, 0f),
                end1 = center + Offset(280f, 0f),
                durationMillis = 300,
            )
        }
        composeTestRule.waitForIdle()

        assertTrue("expected zoomScale > 1f after pinch-out, was ${zoomScale.value}", zoomScale.value > 1f)
    }

    @Test
    fun pinchInThenOutStaysWithinOneToFiveXBounds() {
        val zoomScale = mutableStateOf(1f)
        val zoomOffset = mutableStateOf(Offset.Zero)

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("zoomTarget")
                    .pinchZoomAndSwipe(
                        key = Unit,
                        zoomScale = zoomScale,
                        zoomOffset = zoomOffset,
                        onSwipeDirectionChanged = {},
                        onSwipeNext = {},
                        onSwipePrevious = {},
                    ),
            )
        }

        // Pinch in (fingers start far apart, end close together) -- should clamp at the 1f floor,
        // never go below it.
        composeTestRule.onNodeWithTag("zoomTarget").performTouchInput {
            pinch(
                start0 = center - Offset(280f, 0f),
                end0 = center - Offset(20f, 0f),
                start1 = center + Offset(280f, 0f),
                end1 = center + Offset(20f, 0f),
                durationMillis = 300,
            )
        }
        composeTestRule.waitForIdle()
        assertEquals(1f, zoomScale.value)

        // Repeated aggressive pinch-outs should clamp at the 5f ceiling, never exceed it.
        repeat(5) {
            composeTestRule.onNodeWithTag("zoomTarget").performTouchInput {
                pinch(
                    start0 = center - Offset(20f, 0f),
                    end0 = center - Offset(400f, 0f),
                    start1 = center + Offset(20f, 0f),
                    end1 = center + Offset(400f, 0f),
                    durationMillis = 300,
                )
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("expected zoomScale clamped to <= 5f, was ${zoomScale.value}", zoomScale.value <= 5f)
        assertTrue("expected pinch-out to have raised scale above 1f", zoomScale.value > 1f)
    }

    @Test
    fun singleFingerSwipeStillNavigatesWhenNotZoomedIn() {
        val zoomScale = mutableStateOf(1f)
        val zoomOffset = mutableStateOf(Offset.Zero)
        var nextCalled = false
        var previousCalled = false

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("swipeTarget")
                    .pinchZoomAndSwipe(
                        key = Unit,
                        zoomScale = zoomScale,
                        zoomOffset = zoomOffset,
                        onSwipeDirectionChanged = {},
                        onSwipeNext = { nextCalled = true },
                        onSwipePrevious = { previousCalled = true },
                    ),
            )
        }

        composeTestRule.onNodeWithTag("swipeTarget").performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertTrue("expected showNext() to fire on a left swipe while not zoomed in", nextCalled)
        assertTrue("showPrevious() should not fire from a left swipe", !previousCalled)
        assertEquals("a plain single-finger swipe must not change zoom", 1f, zoomScale.value)
    }
}
