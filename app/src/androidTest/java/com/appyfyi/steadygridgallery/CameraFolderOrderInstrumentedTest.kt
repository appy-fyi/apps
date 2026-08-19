package com.appyfyi.steadygridgallery

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces "Images keep on disappearing, you cannot find the camera folder easily": the Camera
 * folder tile must be present and must appear before other folders in the Folders grid.
 */
@RunWith(AndroidJUnit4::class)
class CameraFolderOrderInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestMediaStoreUtils.grantMediaPermissions()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestMediaStoreUtils.insertJpeg(context, "DCIM/Camera/", "camera_test.jpg")
        TestMediaStoreUtils.insertJpeg(context, "Pictures/Other/", "other_test.jpg")
    }

    @Test
    fun cameraFolderTileIsPresentAndAppearsBeforeOtherFolders() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithTextContaining("Camera").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithTextContaining("Other").fetchSemanticsNodes().isNotEmpty()
        }

        val cameraBounds = composeTestRule.onNodeWithText("Camera").fetchSemanticsNode().boundsInRoot
        val otherBounds = composeTestRule.onNodeWithText("Other").fetchSemanticsNode().boundsInRoot

        val cameraComesFirst = cameraBounds.top < otherBounds.top ||
            (cameraBounds.top == otherBounds.top && cameraBounds.left < otherBounds.left)
        assertTrue("Camera folder tile should appear before Pictures/Other", cameraComesFirst)
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextContaining(text: String) =
        onAllNodes(androidx.compose.ui.test.hasText(text, substring = true))
}
