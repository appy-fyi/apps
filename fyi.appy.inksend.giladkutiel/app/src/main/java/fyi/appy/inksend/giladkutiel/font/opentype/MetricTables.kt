package fyi.appy.inksend.giladkutiel.font.opentype

private const val UNITS_PER_EM = 1000
private const val ASCENDER = 800
private const val DESCENDER = -200
private const val LINE_GAP = 90

object MetricTables {
    fun buildHead(glyphs: List<GlyphOutline>, indexToLocFormat: Int): ByteArray {
        val xMin = glyphs.minOfOrNull { it.xMin } ?: 0
        val yMin = glyphs.minOfOrNull { it.yMin } ?: 0
        val xMax = glyphs.maxOfOrNull { it.xMax } ?: UNITS_PER_EM
        val yMax = glyphs.maxOfOrNull { it.yMax } ?: UNITS_PER_EM

        return ByteWriter()
            .u32(0x00010000L) // version
            .u32(0x00010000L) // fontRevision
            .u32(0L) // checkSumAdjustment (patched by OpenTypeFontWriter after assembly)
            .u32(0x5F0F3CF5L) // magicNumber
            .u16(0x0003) // flags: baseline at y=0, left sidebearing at x=0
            .u16(UNITS_PER_EM)
            .u32(0L).u32(0L) // created (LONGDATETIME, 8 bytes)
            .u32(0L).u32(0L) // modified (LONGDATETIME, 8 bytes)
            .i16(xMin)
            .i16(yMin)
            .i16(xMax)
            .i16(yMax)
            .u16(0) // macStyle
            .u16(8) // lowestRecPPEM
            .i16(2) // fontDirectionHint (deprecated, fully mixed)
            .i16(indexToLocFormat)
            .i16(0) // glyphDataFormat
            .toByteArray()
    }

    fun buildHhea(glyphs: List<GlyphOutline>): ByteArray {
        val advanceWidthMax = glyphs.maxOfOrNull { it.advanceWidth } ?: UNITS_PER_EM
        val minLsb = glyphs.minOfOrNull { it.xMin } ?: 0
        val minRsb = glyphs.minOfOrNull { it.advanceWidth - it.xMax } ?: 0
        val xMaxExtent = glyphs.maxOfOrNull { it.xMax } ?: UNITS_PER_EM

        return ByteWriter()
            .u32(0x00010000L) // version
            .i16(ASCENDER)
            .i16(DESCENDER)
            .i16(LINE_GAP)
            .u16(advanceWidthMax)
            .i16(minLsb)
            .i16(minRsb)
            .i16(xMaxExtent)
            .i16(1) // caretSlopeRise
            .i16(0) // caretSlopeRun
            .i16(0) // caretOffset
            .i16(0).i16(0).i16(0).i16(0) // reserved x4
            .i16(0) // metricDataFormat
            .u16(glyphs.size) // numberOfHMetrics — every glyph gets its own entry
            .toByteArray()
    }

    fun buildMaxp(glyphs: List<GlyphOutline>): ByteArray {
        val maxPoints = glyphs.maxOfOrNull { g -> g.contours.sumOf { it.size } } ?: 0
        val maxContours = glyphs.maxOfOrNull { it.contours.size } ?: 0

        return ByteWriter()
            .u32(0x00010000L) // version 1.0 (TrueType)
            .u16(glyphs.size)
            .u16(maxPoints)
            .u16(maxContours)
            .u16(0) // maxCompositePoints
            .u16(0) // maxCompositeContours
            .u16(1) // maxZones
            .u16(0) // maxTwilightPoints
            .u16(0) // maxStorage
            .u16(0) // maxFunctionDefs
            .u16(0) // maxInstructionDefs
            .u16(0) // maxStackElements
            .u16(0) // maxSizeOfInstructions
            .u16(0) // maxComponentElements
            .u16(0) // maxComponentDepth
            .toByteArray()
    }

    fun buildHmtx(glyphs: List<GlyphOutline>): ByteArray {
        val writer = ByteWriter()
        glyphs.forEach { glyph ->
            writer.u16(glyph.advanceWidth)
            writer.i16(glyph.xMin)
        }
        return writer.toByteArray()
    }

    fun buildOs2(glyphs: List<GlyphOutline>): ByteArray {
        val avgWidth = if (glyphs.isEmpty()) UNITS_PER_EM else glyphs.sumOf { it.advanceWidth } / glyphs.size
        val firstChar = glyphs.filter { it.unicode > 0 }.minOfOrNull { it.unicode } ?: 0x20
        val lastChar = glyphs.filter { it.unicode > 0 }.maxOfOrNull { it.unicode } ?: 0x7E

        return ByteWriter()
            .u16(0) // version 0
            .i16(avgWidth) // xAvgCharWidth
            .u16(400) // usWeightClass (Regular)
            .u16(5) // usWidthClass (Medium)
            .u16(0) // fsType (installable embedding)
            .i16(0).i16(0).i16(0).i16(0) // subscript size/offset x/y (unused)
            .i16(0).i16(0).i16(0).i16(0) // superscript size/offset x/y (unused)
            .i16(50) // yStrikeoutSize
            .i16(ASCENDER / 3) // yStrikeoutPosition
            .i16(0) // sFamilyClass
            .bytes(ByteArray(10)) // panose (unclassified)
            .u32(0L).u32(0L).u32(0L).u32(0L) // ulUnicodeRange1-4 (Basic Latin covers our glyph set)
            .tag("INKS") // achVendID
            .u16(0x0040) // fsSelection: REGULAR
            .u16(firstChar)
            .u16(lastChar)
            .i16(ASCENDER) // sTypoAscender
            .i16(DESCENDER) // sTypoDescender
            .i16(LINE_GAP) // sTypoLineGap
            .u16(ASCENDER) // usWinAscent
            .u16(-DESCENDER) // usWinDescent
            .toByteArray()
    }

    fun buildPost(): ByteArray =
        ByteWriter()
            .u32(0x00030000L) // version 3.0: no glyph names table
            .u32(0L) // italicAngle (Fixed)
            .i16(-100) // underlinePosition
            .i16(50) // underlineThickness
            .u32(0L) // isFixedPitch
            .u32(0L).u32(0L).u32(0L).u32(0L) // min/max MemType42/Type1
            .toByteArray()
}
