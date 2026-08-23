package fyi.appy.taponceremote.giladkutiel.data.ir

import android.content.Context
import android.hardware.ConsumerIrManager

interface IrTransmitter {
    fun hasIrEmitter(): Boolean
    fun transmit(frequencyHz: Int, pattern: IntArray)
}

class SystemIrTransmitter(context: Context) : IrTransmitter {
    private val manager = context.getSystemService(ConsumerIrManager::class.java)

    override fun hasIrEmitter(): Boolean = manager?.hasIrEmitter() ?: false

    override fun transmit(frequencyHz: Int, pattern: IntArray) {
        manager?.transmit(frequencyHz, pattern)
    }
}
