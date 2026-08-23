package fyi.appy.taponceremote.giladkutiel.data.remote

/** Used for SSDP/DIAL entries and other devices with no known command API — shown, never pretends to send. */
class NoOpProtocolAdapter : RemoteProtocolAdapter {
    override suspend fun send(command: RemoteCommand): CommandResult = CommandResult.Unsupported
    override suspend fun sendText(text: String): CommandResult = CommandResult.TextUnsupported
}
