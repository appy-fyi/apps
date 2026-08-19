package com.appyfyi.steadygridgallery.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class DeleteState {
    COPIED_PENDING_SYSTEM_DELETE,
    RECYCLED,
    RESTORED,
    PERMANENTLY_DELETED,
    ERROR,
}

@Entity(tableName = "recycle_item")
data class RecycleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalMediaStoreId: Long,
    val originalUri: String,
    val displayName: String,
    val mimeType: String,
    val relativePath: String,
    val dateTakenMillis: Long,
    val trashedCopyPath: String,
    val sha256: String,
    val sizeBytes: Long,
    val deletedAt: Instant,
    val deleteState: String,
)
