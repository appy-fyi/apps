package fyi.appy.taponceremote.giladkutiel.data.remote

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.db.SavedDevice

object ProtocolAdapterFactory {
    fun create(context: Context, device: SavedDevice): RemoteProtocolAdapter = when (device.protocol) {
        RemoteProtocol.ROKU_ECP, RemoteProtocol.MANUAL_ROKU_ECP -> {
            val port = device.port ?: 8060
            RokuProtocolAdapter("http://${device.ipAddress}:$port")
        }
        RemoteProtocol.GOOGLE_CAST -> CastProtocolAdapter {
            try {
                CastContext.getSharedInstance(context).sessionManager.currentCastSession
            } catch (e: Exception) {
                null
            }
        }
        RemoteProtocol.SSDP_DIAL, RemoteProtocol.IR_PROFILE -> NoOpProtocolAdapter()
    }
}
