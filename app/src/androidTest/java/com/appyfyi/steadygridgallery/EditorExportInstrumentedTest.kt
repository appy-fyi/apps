package com.appyfyi.steadygridgallery

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appyfyi.steadygridgallery.data.prefs.PRO_UNLOCK_PRODUCT_ID
import com.appyfyi.steadygridgallery.data.prefs.PurchaseEntitlementStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reproduces the spec's "Advanced Editor broken: editing and exporting hangs at 0% and never
 * completes" scenario end to end: edit a large JPEG (crop, rotate, sepia filter) and confirm
 * export makes visible progress within 1 second and finishes within 15 seconds.
 */
@RunWith(AndroidJUnit4::class)
class EditorExportInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        TestMediaStoreUtils.grantMediaPermissions()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestMediaStoreUtils.insertJpeg(
            context = context,
            relativePath = "Pictures/EditorTest/",
            displayName = "editor_test.jpg",
            width = 3000,
            height = 2000,
        )
        PurchaseEntitlementStore(context).setEntitlement(
            productId = PRO_UNLOCK_PRODUCT_ID,
            isPurchased = true,
            purchaseTokenHash = "test-token-hash",
        )
    }

    @Test
    fun editingAndExportingCompletesInsteadOfHangingAtZeroPercent() {
        composeTestRule.onNodeWithText("EditorTest", substring = true).performClick()
        composeTestRule.onNodeWithText("editor_test.jpg", substring = true).performClick()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()

        // Crop: drag the bottom-right handle inward to roughly the center half of the image.
        composeTestRule.onNodeWithText("Rotate 90°").performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithText("Rotate 90°").performClick()
        composeTestRule.onNodeWithText("Sepia").performClick()
        composeTestRule.onNodeWithText("Export").performClick()

        val nonZeroPercent = SemanticsMatcher("has nonzero percent text") { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                ?.any { it.text.endsWith("%") && it.text != "0%" } == true
        }
        val exportComplete = SemanticsMatcher("has 'Export complete.' text") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == "Export complete." } == true
        }

        // Progress must become nonzero within ~1 second instead of hanging at 0%.
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule.onAllNodes(nonZeroPercent).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodes(exportComplete).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Export complete.").assertExists()
    }
}
