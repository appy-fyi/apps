package fyi.appy.taponceremote.giladkutiel.data.remote

import com.google.android.gms.cast.framework.CastSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Cast adapter. Volume/mute/play are supported through the active
 * [CastSession]; power/channel/navigation have no Cast equivalent and report
 * [CommandResult.Unsupported] rather than pretending to send anything.
 */
class CastProtocolAdapter(
    private val sessionProvider: () -> CastSession?,
) : RemoteProtocolAdapter {

    override suspend fun send(command: RemoteCommand): CommandResult = withContext(Dispatchers.IO) {
        val session = sessionProvider() ?: return@withContext CommandResult.Failure("no active cast session")
        try {
            when (command) {
                RemoteCommand.VolumeUp -> {
                    session.volume = (session.volume + VOLUME_STEP).coerceIn(0.0, 1.0)
                    CommandResult.Success
                }
                RemoteCommand.VolumeDown -> {
                    session.volume = (session.volume - VOLUME_STEP).coerceIn(0.0, 1.0)
                    CommandResult.Success
                }
                RemoteCommand.VolumeMute -> {
                    session.isMute = !session.isMute
                    CommandResult.Success
                }
                RemoteCommand.Play -> {
                    session.remoteMediaClient?.play()
                    CommandResult.Success
                }
                else -> CommandResult.Unsupported
            }
        } catch (e: Exception) {
            CommandResult.Failure(e.message ?: "cast command failed")
        }
    }

    override suspend fun sendText(text: String): CommandResult = CommandResult.TextUnsupported

    companion object {
        private const val VOLUME_STEP = 0.05
    }
}
