package fyi.appy.taponceremote.giladkutiel.data.ir

data class IrProfile(
    val name: String,
    val protocol: String,
    val carrierFrequencyHz: Int,
    val commands: Map<String, Long>,
)
