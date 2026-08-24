package fyi.appy.inksend.giladkutiel.font

import fyi.appy.inksend.giladkutiel.font.opentype.CmapTableBuilder
import fyi.appy.inksend.giladkutiel.font.opentype.DesignPoint
import fyi.appy.inksend.giladkutiel.font.opentype.GlyfTableBuilder
import fyi.appy.inksend.giladkutiel.font.opentype.GlyphOutline
import fyi.appy.inksend.giladkutiel.font.opentype.MetricTables
import fyi.appy.inksend.giladkutiel.font.opentype.NameTableBuilder
import fyi.appy.inksend.giladkutiel.font.opentype.OpenTypeFontWriter
import fyi.appy.inksend.giladkutiel.font.opentype.StrokeToOutline
import java.io.File

/** All 62 characters the Handwriting Font Creator prompts for, in cmap-friendly ascending unicode order. */
val REQUIRED_GLYPH_CHARACTERS: List<Char> = ('0'..'9') + ('A'..'Z') + ('a'..'z')

private const val UNITS_PER_EM = 1000
private const val ADVANCE_WIDTH = 1000
private const val STROKE_HALF_WIDTH = 28f

/**
 * Compiles a completed set of 62 hand-drawn glyphs into a real, installable
 * TrueType font file, entirely on-device — no third-party font-compiler
 * library, since the glyph set and encoding are fixed and small (see
 * `font/opentype/`).
 */
object HandwritingFontCompiler {
    /**
     * [strokesByChar] maps each of the 62 required characters to the strokes drawn for it
     * (design-unit points, already normalized against the drawing canvas — see
     * [fyi.appy.inksend.giladkutiel.ui.handwriting.GlyphCanvasMapper]). All 62 must be present.
     */
    fun compile(strokesByChar: Map<Char, List<List<DesignPoint>>>, familyName: String, outputFile: File) {
        require(REQUIRED_GLYPH_CHARACTERS.all { it in strokesByChar }) {
            "All 62 required glyphs must be drawn before compiling."
        }

        val notdef = GlyphOutline(
            unicode = 0,
            contours = listOf(notdefBoxContour()),
            advanceWidth = ADVANCE_WIDTH,
        )

        val drawnGlyphs = REQUIRED_GLYPH_CHARACTERS.map { char ->
            val strokes = strokesByChar.getValue(char)
            val contours = strokes.map { StrokeToOutline.expand(it, STROKE_HALF_WIDTH) }
                .filter { it.size >= 3 }
            GlyphOutline(unicode = char.code, contours = contours, advanceWidth = ADVANCE_WIDTH)
        }

        val glyphs = listOf(notdef) + drawnGlyphs
        val unicodeToGlyphIndex = glyphs.mapIndexedNotNull { index, glyph ->
            if (glyph.unicode > 0) glyph.unicode to index else null
        }.toMap()

        val indexToLocFormat = 1 // long (uint32) offsets — simplest, robust for our small glyph count
        val glyf = GlyfTableBuilder.build(glyphs)

        val tables = mapOf(
            "glyf" to glyf.glyf,
            "loca" to glyf.loca,
            "head" to MetricTables.buildHead(glyphs, indexToLocFormat),
            "hhea" to MetricTables.buildHhea(glyphs),
            "hmtx" to MetricTables.buildHmtx(glyphs),
            "maxp" to MetricTables.buildMaxp(glyphs),
            "OS/2" to MetricTables.buildOs2(glyphs),
            "post" to MetricTables.buildPost(),
            "cmap" to CmapTableBuilder.build(unicodeToGlyphIndex),
            "name" to NameTableBuilder.build(familyName),
        )

        val fontBytes = OpenTypeFontWriter.assemble(tables)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(fontBytes)
    }

    private fun notdefBoxContour(): List<DesignPoint> {
        val margin = UNITS_PER_EM / 10
        return listOf(
            DesignPoint(margin, 0),
            DesignPoint(UNITS_PER_EM - margin, 0),
            DesignPoint(UNITS_PER_EM - margin, UNITS_PER_EM * 7 / 10),
            DesignPoint(margin, UNITS_PER_EM * 7 / 10),
        )
    }
}
