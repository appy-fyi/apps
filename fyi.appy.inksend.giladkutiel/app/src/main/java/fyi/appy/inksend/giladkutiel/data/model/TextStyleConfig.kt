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
    val fontSizeSp: Float = 28f,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#1E1E2E",
    val isGradientEnabled: Boolean = true,
    val gradientEndColorHex: String = "#89B4FA",
    val paddingDp: Int = 32,
    val cornerRadiusDp: Float = 24f,
    val minTextLength: Int = 3,
    val maxTextLength: Int = 280,
)
