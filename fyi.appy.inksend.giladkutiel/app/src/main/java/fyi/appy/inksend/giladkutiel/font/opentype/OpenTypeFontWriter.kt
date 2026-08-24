package fyi.appy.inksend.giladkutiel.font.opentype

/** Assembles named table byte blobs into a valid sfnt (TrueType) binary. */
object OpenTypeFontWriter {
    fun assemble(tables: Map<String, ByteArray>): ByteArray {
        val sortedTags = tables.keys.sorted()
        val numTables = sortedTags.size

        var entrySelector = 0
        while ((1 shl (entrySelector + 1)) <= numTables) entrySelector++
        val searchRange = (1 shl entrySelector) * 16
        val rangeShift = numTables * 16 - searchRange

        val header = ByteWriter()
        header.u32(0x00010000L) // sfnt version: TrueType
        header.u16(numTables)
        header.u16(searchRange)
        header.u16(entrySelector)
        header.u16(rangeShift)

        val directoryEntrySize = 16
        val dataStart = 12 + numTables * directoryEntrySize

        val directory = ByteWriter()
        val dataSection = ByteWriter()
        var offset = dataStart

        val paddedTables = sortedTags.associateWith { tag ->
            val raw = tables.getValue(tag)
            val padded = raw.copyOf(raw.size + ((4 - raw.size % 4) % 4))
            padded
        }

        for (tag in sortedTags) {
            val data = paddedTables.getValue(tag)
            val checksum = checksum(data)
            directory.tag(tag)
            directory.u32(checksum)
            directory.u32(offset.toLong())
            directory.u32(tables.getValue(tag).size.toLong())
            dataSection.bytes(data)
            offset += data.size
        }

        val fontBytes = header.toByteArray() + directory.toByteArray() + dataSection.toByteArray()

        // Patch head.checkSumAdjustment: 0xB1B0AFBA minus the checksum of the whole font
        // (computed with checkSumAdjustment itself treated as zero, which it already is).
        val headOffset = findTableOffset(fontBytes, "head", sortedTags, dataStart, paddedTables)
        if (headOffset != null) {
            val fullChecksum = checksum(fontBytes)
            val adjustment = (0xB1B0AFBAL - fullChecksum) and 0xFFFFFFFFL
            writeU32(fontBytes, headOffset + 8, adjustment)
        }

        return fontBytes
    }

    private fun findTableOffset(
        fontBytes: ByteArray,
        target: String,
        sortedTags: List<String>,
        dataStart: Int,
        paddedTables: Map<String, ByteArray>,
    ): Int? {
        var offset = dataStart
        for (tag in sortedTags) {
            if (tag == target) return offset
            offset += paddedTables.getValue(tag).size
        }
        return null
    }

    private fun writeU32(bytes: ByteArray, at: Int, value: Long) {
        bytes[at] = ((value ushr 24) and 0xFF).toByte()
        bytes[at + 1] = ((value ushr 16) and 0xFF).toByte()
        bytes[at + 2] = ((value ushr 8) and 0xFF).toByte()
        bytes[at + 3] = (value and 0xFF).toByte()
    }

    /** Sum of the table's bytes as big-endian uint32 words (per the OpenType spec's checksum algorithm). */
    private fun checksum(data: ByteArray): Long {
        var sum = 0L
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toLong() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toLong() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toLong() and 0xFF else 0
            val b3 = if (i + 3 < data.size) data[i + 3].toLong() and 0xFF else 0
            sum += (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            i += 4
        }
        return sum and 0xFFFFFFFFL
    }
}
