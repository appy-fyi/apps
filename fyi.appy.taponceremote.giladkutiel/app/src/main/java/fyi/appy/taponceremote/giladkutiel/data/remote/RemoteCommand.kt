package fyi.appy.taponceremote.giladkutiel.data.remote

enum class RemoteCommand {
    Power,
    VolumeUp,
    VolumeDown,
    VolumeMute,
    ChannelUp,
    ChannelDown,
    Up,
    Down,
    Left,
    Right,
    Select,
    Home,
    Back,
    Play,
    Rev,
    Fwd,
}

sealed interface CommandResult {
    data object Success : CommandResult
    data object Unsupported : CommandResult
    data object TextUnsupported : CommandResult
    data class Failure(val reason: String) : CommandResult
}

interface RemoteProtocolAdapter {
    suspend fun send(command: RemoteCommand): CommandResult
    suspend fun sendText(text: String): CommandResult
}
