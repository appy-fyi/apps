package fyi.appy.taponceremote.giladkutiel.data.discovery

import fyi.appy.taponceremote.giladkutiel.data.db.RemoteProtocol

data class DiscoveredDevice(
    val key: String,
    val displayName: String,
    val protocol: RemoteProtocol,
    val ipAddress: String? = null,
    val port: Int? = null,
    val castDeviceId: String? = null,
)
