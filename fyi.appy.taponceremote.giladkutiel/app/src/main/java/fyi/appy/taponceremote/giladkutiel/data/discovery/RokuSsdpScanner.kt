package fyi.appy.taponceremote.giladkutiel.data.discovery

import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URI

/**
 * Finds Roku devices via SSDP M-SEARCH on the local network, then confirms
 * ECP support and reads the friendly device name over HTTP.
 */
class RokuSsdpScanner(
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun scan(scanWindowMillis: Long = 3000): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val locations = collectSsdpLocations(scanWindowMillis)
        locations.mapNotNull { location -> probeLocation(location) }
    }

    private fun collectSsdpLocations(scanWindowMillis: Long): Set<String> {
        val locations = mutableSetOf<String>()
        val socket = DatagramSocket().apply { soTimeout = 500 }
        try {
            val group = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
            for (searchTarget in listOf("roku:ecp", "ssdp:all")) {
                val message = buildMSearch(searchTarget)
                val packet = DatagramPacket(message, message.size, group, SSDP_PORT)
                socket.send(packet)
            }

            val deadline = System.currentTimeMillis() + scanWindowMillis
            val buffer = ByteArray(2048)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val response = DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    val text = String(response.data, 0, response.length)
                    parseLocationHeader(text)?.let { locations.add(it) }
                } catch (e: java.net.SocketTimeoutException) {
                    // expected between packets, keep polling until the deadline
                }
            }
        } catch (e: Exception) {
            // network unavailable or blocked; return whatever was collected
        } finally {
            socket.close()
        }
        return locations
    }

    private fun buildMSearch(searchTarget: String): ByteArray {
        val message = "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: $SSDP_MULTICAST_ADDRESS:$SSDP_PORT\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 2\r\n" +
            "ST: $searchTarget\r\n\r\n"
        return message.toByteArray()
    }

    private fun parseLocationHeader(response: String): String? =
        response.lineSequence()
            .firstOrNull { it.startsWith("LOCATION:", ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()

    private fun probeLocation(location: String): DiscoveredDevice? {
        val uri = try {
            URI(location)
        } catch (e: Exception) {
            return null
        }
        val host = uri.host ?: return null
        val port = if (uri.port > 0) uri.port else 8060
        val baseUrl = "http://$host:$port"
        return try {
            val request = Request.Builder().url("$baseUrl/query/device-info").get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                val friendlyName = extractTag(body, "friendly-device-name") ?: "Roku ($host)"
                DiscoveredDevice(
                    key = "roku:$host:$port",
                    displayName = friendlyName,
                    protocol = RemoteProtocol.ROKU_ECP,
                    ipAddress = host,
                    port = port,
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTag(xml: String, tag: String): String? =
        Regex("<$tag>(.*?)</$tag>").find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private const val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900
    }
}
