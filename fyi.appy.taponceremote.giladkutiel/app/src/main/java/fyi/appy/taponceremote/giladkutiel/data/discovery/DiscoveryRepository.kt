package fyi.appy.taponceremote.giladkutiel.data.discovery

import android.content.Context
import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import fyi.appy.taponceremote.giladkutiel.data.remote.RokuProtocolAdapter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

interface DiscoveryRepository {
    /** Runs the Roku SSDP and Cast route scans in parallel and merges results. */
    suspend fun scan(): List<DiscoveredDevice>

    /** Validates a manually entered IP by requesting Roku's device-info endpoint over HTTP 200. */
    suspend fun probeManualIp(ip: String, port: Int = 8060): DiscoveredDevice?
}

class DefaultDiscoveryRepository(
    context: Context,
    private val rokuScanner: RokuSsdpScanner = RokuSsdpScanner(),
    private val castScanner: CastDiscoveryScanner = CastDiscoveryScanner(context),
) : DiscoveryRepository {

    override suspend fun scan(): List<DiscoveredDevice> = coroutineScope {
        val results = mutableListOf<DiscoveredDevice>()
        val rokuJob = launch { results.addAll(rokuScanner.scan()) }
        val castJob = launch { results.addAll(castScanner.scan()) }
        rokuJob.join()
        castJob.join()
        results
    }

    override suspend fun probeManualIp(ip: String, port: Int): DiscoveredDevice? {
        val baseUrl = "http://$ip:$port"
        val reachable = RokuProtocolAdapter.probeDeviceInfo(baseUrl)
        if (!reachable) return null
        return DiscoveredDevice(
            key = "roku:manual:$ip:$port",
            displayName = "Roku ($ip)",
            protocol = RemoteProtocol.MANUAL_ROKU_ECP,
            ipAddress = ip,
            port = port,
        )
    }
}
