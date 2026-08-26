package fyi.appy.inksend.giladkutiel.engine

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import fyi.appy.inksend.giladkutiel.data.model.RenderPlan
import fyi.appy.inksend.giladkutiel.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android 10+ only lets the app holding window focus read the primary clip back,
 * so this drives the copy from inside a resumed, focused Activity rather than
 * the bare instrumentation process (which has no focused window of its own).
 */
@RunWith(AndroidJUnit4::class)
class ClipboardManagerHelperTest {

    @Test
    fun copyingAnImageUriPutsAnImagePngClipOnTheSystemClipboard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val plan = RenderPlan(
                    fontAssetPath = "fonts/Comfortaa.ttf",
                    fontName = "sample",
                    gradientStartHex = "#1E1E2E",
                    gradientEndHex = "#89B4FA",
                    textColorHex = "#FFFFFF",
                    emojis = listOf("✨"),
                )
                val uri = ImageRenderer.generateStyledImageUri(activity, "Paste me", plan)

                ClipboardManagerHelper.copyImageToClipboard(activity, uri)

                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val primaryClip = clipboard.primaryClip
                assertNotNull("clipboard should hold the copied clip", primaryClip)
                assertEquals(uri, primaryClip!!.getItemAt(0).uri)
                assertEquals("image/png", primaryClip.description.getMimeType(0))
            }
        }
    }
}
