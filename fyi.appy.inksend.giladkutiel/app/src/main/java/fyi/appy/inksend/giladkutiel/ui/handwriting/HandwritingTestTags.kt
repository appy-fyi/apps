package fyi.appy.inksend.giladkutiel.ui.handwriting

/** Compose test tags shared with the instrumented test suite (see androidTest). */
object HandwritingTestTags {
    const val GLYPH_CANVAS = "glyph_canvas"
    const val SAVE_GLYPH_BUTTON = "save_glyph_button"
    const val SAVE_FONT_BUTTON = "save_font_button"

    /** Tag of a single glyph's tile in the overview grid. */
    fun glyphTile(char: Char) = "glyph_tile_$char"
}
