package fyi.appy.inksend.giladkutiel.font.opentype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.DataInputStream

class OpenTypeFontWriterTest {

    private fun triangleGlyph(unicode: Int, offsetX: Int = 0): GlyphOutline =
        GlyphOutline(
            unicode = unicode,
            contours = listOf(
                listOf(
                    DesignPoint(offsetX + 0, 0),
                    DesignPoint(offsetX + 400, 0),
                    DesignPoint(offsetX + 200, 600),
                ),
            ),
            advanceWidth = 1000,
        )

    @Test
    fun `assembled font has a valid sfnt header and checksum-correct head table`() {
        val glyphs = listOf(triangleGlyph(0), triangleGlyph('A'.code))
        val glyf = GlyfTableBuilder.build(glyphs)
        val tables = mapOf(
            "glyf" to glyf.glyf,
            "loca" to glyf.loca,
            "head" to MetricTables.buildHead(glyphs, 1),
            "hhea" to MetricTables.buildHhea(glyphs),
            "hmtx" to MetricTables.buildHmtx(glyphs),
            "maxp" to MetricTables.buildMaxp(glyphs),
            "OS/2" to MetricTables.buildOs2(glyphs),
            "post" to MetricTables.buildPost(),
            "cmap" to CmapTableBuilder.build(mapOf('A'.code to 1)),
            "name" to NameTableBuilder.build("Test Font"),
        )

        val fontBytes = OpenTypeFontWriter.assemble(tables)
        val input = DataInputStream(ByteArrayInputStream(fontBytes))

        val sfntVersion = input.readInt()
        assertEquals(0x00010000, sfntVersion)

        val numTables = input.readUnsignedShort()
        assertEquals(tables.size, numTables)
        input.readUnsignedShort() // searchRange
        input.readUnsignedShort() // entrySelector
        input.readUnsignedShort() // rangeShift

        val sortedTags = tables.keys.sorted()
        var headOffset = -1
        var headLength = -1
        repeat(numTables) {
            val tagBytes = ByteArray(4)
            input.readFully(tagBytes)
            val tag = String(tagBytes, Charsets.US_ASCII)
            input.readInt() // checksum
            val offset = input.readInt()
            val length = input.readInt()
            assertTrue("unexpected table tag $tag", tag in sortedTags)
            if (tag == "head") {
                headOffset = offset
                headLength = length
            }
        }
        assertTrue(headOffset > 0)
        assertEquals(54, headLength) // head table is always 54 bytes

        // magicNumber is at byte offset 12 within the head table.
        val magicNumber = ((fontBytes[headOffset + 12].toInt() and 0xFF) shl 24) or
            ((fontBytes[headOffset + 13].toInt() and 0xFF) shl 16) or
            ((fontBytes[headOffset + 14].toInt() and 0xFF) shl 8) or
            (fontBytes[headOffset + 15].toInt() and 0xFF)
        assertEquals(0x5F0F3CF5, magicNumber)
    }

    @Test
    fun `a single contiguous unicode range collapses into one segment plus the terminator`() {
        val unicodeToGlyphIndex = ('A'..'Z').mapIndexed { i, c -> c.code to (i + 1) }.toMap()
        val cmap = CmapTableBuilder.build(unicodeToGlyphIndex)
        val input = DataInputStream(ByteArrayInputStream(cmap))
        assertEquals(0, input.readUnsignedShort()) // version
        assertEquals(1, input.readUnsignedShort()) // numTables
        assertEquals(3, input.readUnsignedShort()) // platformID
        assertEquals(1, input.readUnsignedShort()) // encodingID
        input.readInt() // subtable offset

        assertEquals(4, input.readUnsignedShort()) // format
        input.readUnsignedShort() // length
        input.readUnsignedShort() // language
        val segCountX2 = input.readUnsignedShort()
        assertEquals(2, segCountX2 / 2) // one contiguous A-Z segment + terminator
    }

    @Test
    fun `three disjoint unicode ranges produce three segments plus the terminator`() {
        // Mirrors REQUIRED_GLYPH_CHARACTERS' ordering: digits, A-Z, a-z — each internally
        // contiguous in both unicode and glyph index, but not contiguous with each other.
        val unicodeToGlyphIndex = (('0'..'9') + ('A'..'Z') + ('a'..'z'))
            .mapIndexed { i, c -> c.code to (i + 1) }.toMap()
        val cmap = CmapTableBuilder.build(unicodeToGlyphIndex)
        val input = DataInputStream(ByteArrayInputStream(cmap))
        repeat(4) { input.readUnsignedShort() } // version, numTables, platformID, encodingID
        input.readInt() // subtable offset
        input.readUnsignedShort() // format
        input.readUnsignedShort() // length
        input.readUnsignedShort() // language
        val segCountX2 = input.readUnsignedShort()
        assertEquals(4, segCountX2 / 2) // 3 disjoint segments + terminator
    }
}
