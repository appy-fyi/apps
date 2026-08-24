package fyi.appy.inksend.giladkutiel.font.opentype

import java.nio.charset.StandardCharsets

/** Builds a minimal `name` table (platform 3/Windows, encoding 1/Unicode BMP, language 0x0409/en-US). */
object NameTableBuilder {
    fun build(familyName: String): ByteArray {
        val postScriptName = familyName.replace(Regex("[^A-Za-z0-9]"), "")
        val records = listOf(
            1 to familyName, // Family
            2 to "Regular", // Subfamily
            3 to "InkSend:$postScriptName:2024", // Unique identifier
            4 to familyName, // Full name
            5 to "Version 1.0", // Version string
            6 to postScriptName, // PostScript name
        )

        val stringStorage = ByteWriter()
        val offsets = ArrayList<Pair<Int, Int>>() // length, offset
        records.forEach { (_, value) ->
            val utf16 = value.toByteArray(StandardCharsets.UTF_16BE)
            offsets.add(utf16.size to stringStorage.size)
            stringStorage.bytes(utf16)
        }

        val writer = ByteWriter()
        writer.u16(0) // format
        writer.u16(records.size)
        writer.u16(6 + records.size * 12) // stringOffset: right after this header + records
        records.forEachIndexed { index, (nameId, _) ->
            val (length, offset) = offsets[index]
            writer.u16(3) // platformID: Windows
            writer.u16(1) // encodingID: Unicode BMP
            writer.u16(0x0409) // languageID: en-US
            writer.u16(nameId)
            writer.u16(length)
            writer.u16(offset)
        }
        writer.bytes(stringStorage.toByteArray())
        return writer.toByteArray()
    }
}
