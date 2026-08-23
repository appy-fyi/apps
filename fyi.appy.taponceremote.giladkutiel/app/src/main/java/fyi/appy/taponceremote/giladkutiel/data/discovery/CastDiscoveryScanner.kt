package fyi.appy.taponceremote.giladkutiel.data.discovery

import android.content.Context
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads currently visible Cast routes from the framework's [MediaRouter] selector. */
class CastDiscoveryScanner(private val context: Context) {
    suspend fun scan(): List<DiscoveredDevice> = withContext(Dispatchers.Main) {
        try {
            val castContext = CastContext.getSharedInstance(context)
            val selector = castContext.mergedSelector ?: MediaRouteSelector.EMPTY
            val router = MediaRouter.getInstance(context)
            router.routes
                .filter { it.matchesSelector(selector) && !it.isDefault }
                .map { route ->
                    DiscoveredDevice(
                        key = "cast:${route.id}",
                        displayName = route.name,
                        protocol = RemoteProtocol.GOOGLE_CAST,
                        castDeviceId = route.id,
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
