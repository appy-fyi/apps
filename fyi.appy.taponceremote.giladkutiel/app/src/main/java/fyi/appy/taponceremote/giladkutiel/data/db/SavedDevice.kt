package fyi.appy.taponceremote.giladkutiel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RemoteProtocol {
    ROKU_ECP,
    GOOGLE_CAST,
    SSDP_DIAL,
    IR_PROFILE,
    MANUAL_ROKU_ECP,
}

@Entity(tableName = "saved_devices")
data class SavedDevice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val protocol: RemoteProtocol,
    val ipAddress: String? = null,
    val port: Int? = null,
    val castDeviceId: String? = null,
    val irProfileName: String? = null,
    val lastSeenAtEpochMillis: Long,
    val lastUsed: Boolean = false,
)
