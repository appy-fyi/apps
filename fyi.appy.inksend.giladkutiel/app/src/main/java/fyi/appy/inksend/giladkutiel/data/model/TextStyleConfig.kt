package fyi.appy.inksend.giladkutiel.data.model

import androidx.annotation.StringRes
import fyi.appy.inksend.giladkutiel.R

/** Available font family choices, using Android's generic system typeface names. */
enum class FontChoice(val typefaceName: String, @StringRes val labelRes: Int) {
    SANS_SERIF("sans-serif", R.string.font_sans_serif),
    SERIF("serif", R.string.font_serif),
    MONOSPACE("monospace", R.string.font_monospace),
    CURSIVE("cursive", R.string.font_cursive),
}

data class TextStyleConfig(
    val font: FontChoice = FontChoice.SANS_SERIF,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#1E1E2E",
    val isGradientEnabled: Boolean = true,
    val gradientEndColorHex: String = "#89B4FA",
    val paddingDp: Int = 32,
    val cornerRadiusDp: Float = 24f,
    val minTextLength: Int = 3,
    val maxTextLength: Int = 280,
    /** Small badge drawn in a corner of the rendered image. Blank means no badge. */
    val emoji: String = "✨",
)

/**
 * The visual look of the second overlay button. Only the fields that affect rendered
 * appearance are here — trigger bounds and padding are shared with [TextStyleConfig],
 * since only one overlay pair triggers/lays out for a given typed text.
 */
data class SecondaryStyleConfig(
    val font: FontChoice = FontChoice.SERIF,
    val textColorHex: String = "#1E1E2E",
    val backgroundColorHex: String = "#F5E9DA",
    val isGradientEnabled: Boolean = false,
    val gradientEndColorHex: String = "#F7B267",
    val emoji: String = "🎨",
)

/** Builds the full render config for the second overlay button, borrowing shared layout fields from [base]. */
fun SecondaryStyleConfig.toRenderConfig(base: TextStyleConfig): TextStyleConfig = base.copy(
    font = font,
    textColorHex = textColorHex,
    backgroundColorHex = backgroundColorHex,
    isGradientEnabled = isGradientEnabled,
    gradientEndColorHex = gradientEndColorHex,
    emoji = emoji,
)
