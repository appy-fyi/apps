package fyi.appy.inksend.giladkutiel.font

import fyi.appy.inksend.giladkutiel.font.opentype.DesignPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataInputStream
import java.io.FileInputStream
import java.nio.file.Files

class HandwritingFontCompilerTest {
    /** A trivial diagonal-line stroke, distinct per character so every glyph has real geometry. */
    private fun strokeFor(char: Char): List<List<DesignPoint>> {
        val seed = char.code % 200
        return listOf(listOf(DesignPoint(100 + seed, 0), DesignPoint(300, 700), DesignPoint(500 - seed, 0)))
    }

    @Test
    fun `compiling all 62 required glyphs produces a valid sfnt file`() {
        val strokes = REQUIRED_GLYPH_CHARACTERS.associateWith { strokeFor(it) }
        val outputFile = Files.createTempDirectory("inksend-font-test").resolve("test.ttf").toFile()

        HandwritingFontCompiler.compile(strokes, "Test Handwriting", outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        DataInputStream(FileInputStream(outputFile)).use { input ->
            assertEquals(0x00010000, input.readInt()) // sfnt version
            val numTables = input.readUnsignedShort()
            // glyf, loca, head, hhea, hmtx, maxp, OS/2, post, cmap, name
            assertEquals(10, numTables)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compiling with a missing glyph throws`() {
        val incomplete = REQUIRED_GLYPH_CHARACTERS.drop(1).associateWith { strokeFor(it) }
        val outputFile = Files.createTempDirectory("inksend-font-test").resolve("incomplete.ttf").toFile()
        HandwritingFontCompiler.compile(incomplete, "Broken", outputFile)
    }
}
