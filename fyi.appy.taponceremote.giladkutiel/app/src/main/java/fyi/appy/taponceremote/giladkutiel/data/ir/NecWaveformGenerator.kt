package fyi.appy.taponceremote.giladkutiel.data.ir

/**
 * Generates a NEC IR waveform: 9000us header on, 4500us header off, each bit
 * as 560us on plus 560us off (0) or 1690us off (1), and a final 560us on
 * pulse — the pattern shape [ConsumerIrManager.transmit] expects, in
 * microseconds, alternating on/off starting with "on".
 */
object NecWaveformGenerator {
    fun generate(code: Long, bitCount: Int = 32): IntArray {
        val pattern = mutableListOf<Int>()
        pattern.add(9000)
        pattern.add(4500)
        for (bitIndex in bitCount - 1 downTo 0) {
            val bit = (code shr bitIndex) and 1L
            pattern.add(560)
            pattern.add(if (bit == 1L) 1690 else 560)
        }
        pattern.add(560)
        return pattern.toIntArray()
    }
}
