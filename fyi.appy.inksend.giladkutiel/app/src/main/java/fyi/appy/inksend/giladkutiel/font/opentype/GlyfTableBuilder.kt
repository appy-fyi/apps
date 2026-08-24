package fyi.appy.inksend.giladkutiel.font.opentype

/** Builds the `glyf` and `loca` tables (long/uint32 offset format) from ordered simple-glyph outlines. */
object GlyfTableBuilder {
    class Result(val glyf: ByteArray, val loca: ByteArray)

    /** [glyphs] must already be in final glyph-index order, index 0 = .notdef. */
    fun build(glyphs: List<GlyphOutline>): Result {
        val glyfWriter = ByteWriter()
        val locaOffsets = ArrayList<Long>()

        for (glyph in glyphs) {
            locaOffsets.add(glyfWriter.size.toLong())
            writeSimpleGlyph(glyfWriter, glyph)
            glyfWriter.padTo4ByteBoundary()
        }
        locaOffsets.add(glyfWriter.size.toLong())

        val locaWriter = ByteWriter()
        locaOffsets.forEach { locaWriter.u32(it) }

        return Result(glyfWriter.toByteArray(), locaWriter.toByteArray())
    }

    private fun writeSimpleGlyph(writer: ByteWriter, glyph: GlyphOutline) {
        val contours = glyph.contours.filter { it.size >= 3 }
        if (contours.isEmpty()) return // zero-length glyph entry (e.g. space) — valid per spec

        writer.i16(contours.size)
        writer.i16(glyph.xMin)
        writer.i16(glyph.yMin)
        writer.i16(glyph.xMax)
        writer.i16(glyph.yMax)

        var pointCount = 0
        for (contour in contours) {
            pointCount += contour.size
            writer.u16(pointCount - 1)
        }
        writer.u16(0) // instructionLength

        // All points are on-curve simple line vertices (see StrokeToOutline).
        repeat(pointCount) { writer.u8(0x01) }

        var prevX = 0
        for (contour in contours) {
            for (point in contour) {
                writer.i16(point.x - prevX)
                prevX = point.x
            }
        }
        var prevY = 0
        for (contour in contours) {
            for (point in contour) {
                writer.i16(point.y - prevY)
                prevY = point.y
            }
        }
    }
}
