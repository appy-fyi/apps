package fyi.appy.inksend.giladkutiel.data.model

/**
 * Everything [fyi.appy.inksend.giladkutiel.engine.ImageRenderer] needs to draw one image,
 * fully resolved by [AutoStyle.planFor] from the typed text: which bundled font to load, the
 * two gradient stops, an auto-chosen text colour with enough contrast against that gradient,
 * and the ≤3 emojis for the bottom strip. No enums or lookups survive into the renderer —
 * it just paints what this says.
 */
data class RenderPlan(
    val fontAssetPath: String,
    val fontName: String,
    val gradientStartHex: String,
    val gradientEndHex: String,
    val textColorHex: String,
    val emojis: List<String>,
)
