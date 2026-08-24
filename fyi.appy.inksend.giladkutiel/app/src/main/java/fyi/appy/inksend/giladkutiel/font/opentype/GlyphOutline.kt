package fyi.appy.inksend.giladkutiel.font.opentype

/** One compiled glyph: its unicode code point, contours (all on-curve points), and advance width. */
data class GlyphOutline(
    val unicode: Int,
    val contours: List<List<DesignPoint>>,
    val advanceWidth: Int,
) {
    val xMin: Int get() = allPoints().minOfOrNull { it.x } ?: 0
    val yMin: Int get() = allPoints().minOfOrNull { it.y } ?: 0
    val xMax: Int get() = allPoints().maxOfOrNull { it.x } ?: 0
    val yMax: Int get() = allPoints().maxOfOrNull { it.y } ?: 0

    private fun allPoints(): List<DesignPoint> = contours.flatten()
}
