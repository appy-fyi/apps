package fyi.appy.inksend.giladkutiel.font.opentype

/** A contiguous run of unicode code points mapped to contiguous glyph indices. */
private data class CmapSegment(val startCode: Int, val endCode: Int, val idDelta: Int)

/** Builds a `cmap` table with a single format-4 subtable for platform 3 (Windows), encoding 1 (Unicode BMP). */
object CmapTableBuilder {
    /** [unicodeToGlyphIndex] must map contiguous unicode ranges to contiguous, ascending glyph indices. */
    fun build(unicodeToGlyphIndex: Map<Int, Int>): ByteArray {
        val sorted = unicodeToGlyphIndex.entries.sortedBy { it.key }
        val segments = ArrayList<CmapSegment>()
        var i = 0
        while (i < sorted.size) {
            val startCode = sorted[i].key
            val startGlyph = sorted[i].value
            var j = i
            while (
                j + 1 < sorted.size &&
                sorted[j + 1].key == sorted[j].key + 1 &&
                sorted[j + 1].value == sorted[j].value + 1
            ) {
                j++
            }
            val endCode = sorted[j].key
            segments.add(CmapSegment(startCode, endCode, startGlyph - startCode))
            i = j + 1
        }
        segments.add(CmapSegment(0xFFFF, 0xFFFF, 1)) // required terminator segment

        val segCount = segments.size
        val subtable = buildFormat4Subtable(segments, segCount)

        val writer = ByteWriter()
        writer.u16(0) // version
        writer.u16(1) // numTables
        writer.u16(3) // platformID: Windows
        writer.u16(1) // encodingID: Unicode BMP
        writer.u32(12L) // offset to subtable (right after this one record)
        writer.bytes(subtable)
        return writer.toByteArray()
    }

    private fun buildFormat4Subtable(segments: List<CmapSegment>, segCount: Int): ByteArray {
        val segCountX2 = segCount * 2
        val entrySelector = (Math.log(segCount.toDouble()) / Math.log(2.0)).toInt().coerceAtLeast(0)
        val searchRange = (1 shl entrySelector) * 2
        val rangeShift = segCountX2 - searchRange

        val length = 14 + segCountX2 * 4 + 2 // header + 4 parallel arrays + reservedPad

        val writer = ByteWriter()
        writer.u16(4) // format
        writer.u16(length)
        writer.u16(0) // language
        writer.u16(segCountX2)
        writer.u16(searchRange)
        writer.u16(entrySelector)
        writer.u16(rangeShift)
        segments.forEach { writer.u16(it.endCode) }
        writer.u16(0) // reservedPad
        segments.forEach { writer.u16(it.startCode) }
        segments.forEach { writer.i16(it.idDelta) }
        segments.forEach { writer.u16(0) } // idRangeOffset: 0 everywhere, glyphIdArray unused
        return writer.toByteArray()
    }
}
