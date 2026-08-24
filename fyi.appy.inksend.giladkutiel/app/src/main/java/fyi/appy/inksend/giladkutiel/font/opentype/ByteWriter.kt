package fyi.appy.inksend.giladkutiel.font.opentype

import java.io.ByteArrayOutputStream

/** Big-endian byte buffer builder — OpenType/TrueType tables are always big-endian. */
class ByteWriter {
    private val buffer = ByteArrayOutputStream()

    val size: Int get() = buffer.size()

    fun u8(value: Int): ByteWriter {
        buffer.write(value and 0xFF)
        return this
    }

    fun i8(value: Int): ByteWriter = u8(value and 0xFF)

    fun u16(value: Int): ByteWriter {
        buffer.write((value ushr 8) and 0xFF)
        buffer.write(value and 0xFF)
        return this
    }

    fun i16(value: Int): ByteWriter = u16(value and 0xFFFF)

    fun u32(value: Long): ByteWriter {
        buffer.write(((value ushr 24) and 0xFF).toInt())
        buffer.write(((value ushr 16) and 0xFF).toInt())
        buffer.write(((value ushr 8) and 0xFF).toInt())
        buffer.write((value and 0xFF).toInt())
        return this
    }

    fun tag(fourCc: String): ByteWriter {
        require(fourCc.length == 4)
        fourCc.forEach { buffer.write(it.code and 0xFF) }
        return this
    }

    fun bytes(data: ByteArray): ByteWriter {
        buffer.write(data)
        return this
    }

    fun padTo4ByteBoundary(): ByteWriter {
        while (buffer.size() % 4 != 0) buffer.write(0)
        return this
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
