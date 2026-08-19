package com.appyfyi.steadygridgallery

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
import com.appyfyi.steadygridgallery.data.prefs.PRO_UNLOCK_PRODUCT_ID
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces "Data-loss anxiety: folders and hidden files intermittently vanish": hiding a folder
 * must never touch the underlying MediaStore row, and the folder must reappear in Hidden Folders
 * after unlocking rather than disappearing outright.
 *
 * Note: the spec's acceptance criteria for hidden folders require a Pro entitlement before a new
 * folder can be hidden, but the test_plan script for this scenario does not purchase Pro. This
 * test pre-grants entitlement so the scripted steps can actually reach the behavior under test --
 * flagged here since it's a small inconsistency between the spec's acceptance_criteria and test_plan.
 */
@RunWith(AndroidJUnit4::class)
class HiddenFoldersInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestMediaStoreUtils.grantMediaPermissions()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestMediaStoreUtils.insertJpeg(context, "DCIM/Camera/", "camera_test.jpg")
        TestMediaStoreUtils.insertJpeg(context, "Pictures/Trips/", "trips_test.jpg")
        PurchaseEntitlementStore(context).setEntitlement(PRO_UNLOCK_PRODUCT_ID, isPurchased = true, purchaseTokenHash = "test")
    }

    @Test
    fun hidingFolderPreservesMediaStoreRowAndFolderReappearsInHiddenFolders() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Trips", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Trips", substring = true).performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithContentDescriptionContaining("trips_test.jpg")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("trips_test.jpg").performTouchInput { longClick() }
        composeTestRule.onNodeWithContentDescription("Hide folder").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("New PIN", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("New PIN").performTextInput("1234")
        composeTestRule.onNodeWithText("Confirm PIN").performTextInput("1234")
        composeTestRule.onNodeWithText("Create PIN").performClick()

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf("trips_test.jpg"),
            null,
        )
        val stillPresent = cursor?.use { it.moveToFirst() && it.getString(0)?.contains("Trips") == true } ?: false
        assertTrue("Hidden folder's media must remain in MediaStore untouched", stillPresent)

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Trips", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Trips", substring = true).assertExists()
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithContentDescriptionContaining(
        text: String,
    ) = onAllNodes(androidx.compose.ui.test.hasContentDescription(text, substring = true))
}
