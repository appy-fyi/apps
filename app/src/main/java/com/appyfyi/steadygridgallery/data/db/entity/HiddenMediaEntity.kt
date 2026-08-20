package com.appyfyi.steadygridgallery.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class HiddenMediaState {
    COPIED_PENDING_SYSTEM_DELETE,
    HIDDEN,
    ERROR,
}

/**
 * A photo/video that was moved into the app-private "Hidden Photos" store: [hiddenCopyPath] is a
 * byte-verified copy, kept only after the original MediaStore row is deleted (see
 * HiddenMediaRepository.copyAndVerify), mirroring RecycleItemEntity's copy-then-delete safety model.
 */
@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalMediaStoreId: Long,
    val kind: String,
    val displayName: String,
    val mimeType: String,
    val relativePath: String,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long,
    val hiddenCopyPath: String,
    val sha256: String,
    val sizeBytes: Long,
    val hiddenAt: Instant,
    val state: String,
)
