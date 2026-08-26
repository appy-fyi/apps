package fyi.appy.inksend.giladkutiel.data.model

import androidx.annotation.StringRes
import fyi.appy.inksend.giladkutiel.R
import java.util.UUID

/** Available font family choices, using Android's generic system typeface names. */
enum class FontChoice(val typefaceName: String, @StringRes val labelRes: Int) {
    SANS_SERIF("sans-serif", R.string.font_sans_serif),
    SERIF("serif", R.string.font_serif),
    MONOSPACE("monospace", R.string.font_monospace),
    CURSIVE("cursive", R.string.font_cursive),
}

/**
 * The full look of one overlay style/button: font, colors, gradient, padding, and its
 * emoji badge. [id] is stable across edits so a style can be found and updated or removed
 * from the saved list without relying on its position in it.
 */
data class StyleConfig(
    val id: String = UUID.randomUUID().toString(),
    val font: FontChoice = FontChoice.SANS_SERIF,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#1E1E2E",
    val isGradientEnabled: Boolean = true,
    val gradientEndColorHex: String = "#89B4FA",
    val paddingDp: Int = 32,
    val cornerRadiusDp: Float = 24f,
    /** Small badge drawn in a corner of the rendered image. Blank means no badge. */
    val emoji: String = "✨",
)

/** Text-length bounds shared by every style, since one overlay set triggers for a given typed text. */
data class TriggerConfig(
    val minTextLength: Int = 3,
    val maxTextLength: Int = 280,
)

/** Seeded the first time the app runs, and whenever the user removes every style back down to zero. */
val DEFAULT_STYLES = listOf(
    StyleConfig(
        id = "default-1",
        font = FontChoice.SANS_SERIF,
        textColorHex = "#FFFFFF",
        backgroundColorHex = "#1E1E2E",
        isGradientEnabled = true,
        gradientEndColorHex = "#89B4FA",
        emoji = "✨",
    ),
    StyleConfig(
        id = "default-2",
        font = FontChoice.SERIF,
        textColorHex = "#1E1E2E",
        backgroundColorHex = "#F5E9DA",
        isGradientEnabled = false,
        gradientEndColorHex = "#F7B267",
        emoji = "🎨",
    ),
)
