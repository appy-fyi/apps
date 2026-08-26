package fyi.appy.inksend.giladkutiel.data.model

/** Available font family choices, using Android's generic system typeface names. */
enum class FontChoice(val typefaceName: String, val label: String) {
    SANS_SERIF("sans-serif", "Sans Serif"),
    SERIF("serif", "Serif"),
    MONOSPACE("monospace", "Monospace"),
    CURSIVE("cursive", "Cursive"),
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
)

/** Builds the full render config for the second overlay button, borrowing shared layout fields from [base]. */
fun SecondaryStyleConfig.toRenderConfig(base: TextStyleConfig): TextStyleConfig = base.copy(
    font = font,
    textColorHex = textColorHex,
    backgroundColorHex = backgroundColorHex,
    isGradientEnabled = isGradientEnabled,
    gradientEndColorHex = gradientEndColorHex,
)
