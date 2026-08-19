package com.appyfyi.steadygridgallery

import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces "Make the recycle bin and folder model bulletproof": recycling and then restoring an
 * item must round-trip back to the original relative path with a nonzero size.
 */
@RunWith(AndroidJUnit4::class)
class RecycleBinRestoreInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestMediaStoreUtils.grantMediaPermissions()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestMediaStoreUtils.insertJpeg(context, "Pictures/RecycleSource/", "recycle_test.jpg")
    }

    @Test
    fun recycleThenRestoreRoundTripsToOriginalPath() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("RecycleSource", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("RecycleSource", substring = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("recycle_test.jpg", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("recycle_test.jpg").performClick()

        composeTestRule.onNodeWithContentDescription("Move to Recycle Bin").performClick()

        // The system delete-confirmation dialog is outside the app's Compose tree.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.textContains("Allow")), 5_000)
        val allowButton = device.findObject(By.textContains("Allow")) ?: device.findObject(By.textContains("Delete"))
        allowButton?.click()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("recycle_test.jpg", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Navigate to Recycle Bin from Folders' overflow menu: Viewer -> MediaGrid -> Folders.
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Recycle Bin").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("recycle_test.jpg", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("recycle_test.jpg", substring = true)
            .performClick() // toggles the row's checkbox via its click target in RecycleRow
        composeTestRule.onNodeWithText("Restore selected").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Recycle Bin is empty.", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("recycle_test.jpg"),
            null,
        )
        var restored = false
        cursor?.use {
            if (it.moveToFirst()) {
                val relativePath = it.getString(0)
                val size = it.getLong(1)
                restored = relativePath?.contains("RecycleSource") == true && size > 0
            }
        }
        assertTrue("Restored item must be back at its original relative path with nonzero size", restored)
    }
}
