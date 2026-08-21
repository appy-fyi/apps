package fyi.appy.steadygridgallery

import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import fyi.appy.steadygridgallery.data.prefs.PRO_UNLOCK_PRODUCT_ID
import fyi.appy.steadygridgallery.data.prefs.PurchaseEntitlementStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces the "Hidden Photos" spec: hiding a selected photo must actually move it out of
 * MediaStore into the app-private hidden store (unlike the old whole-folder metadata hide), the
 * hidden vault must sit behind a PIN, and un-hiding must round-trip the photo back to its
 * original relative path.
 *
 * Note: the spec's acceptance criteria require a Pro entitlement before a photo can be hidden,
 * but the test_plan script for this scenario does not purchase Pro. This test pre-grants
 * entitlement so the scripted steps can actually reach the behavior under test.
 */
@RunWith(AndroidJUnit4::class)
class HiddenPhotosInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestMediaStoreUtils.grantMediaPermissions()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestMediaStoreUtils.insertJpeg(context, "Pictures/Trips/", "trips_test.jpg")
        PurchaseEntitlementStore(context).setEntitlement(PRO_UNLOCK_PRODUCT_ID, isPurchased = true, purchaseTokenHash = "test")
    }

    @Test
    fun hidingPhotoRemovesItFromMediaStoreAndUnhideRestoresOriginalPath() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Trips", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Trips", substring = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("trips_test.jpg", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("trips_test.jpg").performTouchInput { longClick() }
        composeTestRule.onNodeWithContentDescription("Hide selected").performClick()

        // No PIN exists yet: set one up. Hiding resumes automatically once the PIN is created.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("New PIN", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("New PIN").performTextInput("1234")
        composeTestRule.onNodeWithText("Confirm PIN").performTextInput("1234")
        composeTestRule.onNodeWithText("Create PIN").performClick()

        // The system delete-confirmation dialog is outside the app's Compose tree.
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.textContains("Allow")), 5_000)
        val allowButton = device.findObject(By.textContains("Allow")) ?: device.findObject(By.textContains("Delete"))
        allowButton?.click()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf("trips_test.jpg"),
                null,
            )
            val stillInMediaStore = cursor?.use { it.moveToFirst() } ?: false
            !stillInMediaStore
        }

        val cursorAfterHide = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("trips_test.jpg"),
            null,
        )
        val stillInMediaStore = cursorAfterHide?.use { it.moveToFirst() } ?: false
        assertFalse("Hidden photo must be removed from MediaStore, not just flagged", stillInMediaStore)

        // Hidden Photos, reached via Folders' overflow menu, must already be unlocked (PIN was
        // just created/verified) and show the hidden item.
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Hidden Photos").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("trips_test.jpg", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Unhide photo").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("No photos are hidden.", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val cursorAfterUnhide = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.SIZE),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("trips_test.jpg"),
            null,
        )
        var restored = false
        cursorAfterUnhide?.use {
            if (it.moveToFirst()) {
                val relativePath = it.getString(0)
                val size = it.getLong(1)
                restored = relativePath?.contains("Trips") == true && size > 0
            }
        }
        assertTrue("Unhidden photo must be restored to its original relative path with nonzero size", restored)

        // Leaving Hidden Photos must require unlocking again to come back in.
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithContentDescription("More options").performClick()
        composeTestRule.onNodeWithText("Hidden Photos").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Enter PIN", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Enter PIN").assertExists()
    }
}
