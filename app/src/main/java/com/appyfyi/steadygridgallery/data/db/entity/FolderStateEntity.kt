package com.appyfyi.steadygridgallery.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

enum class SortMode {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    NAME_DESC,
}

@Entity(tableName = "folder_state")
data class FolderStateEntity(
    @PrimaryKey val folderKey: String,
    val displayName: String,
    val relativePath: String,
    val isHidden: Boolean,
    val sortMode: String,
    val updatedAt: Instant,
)
