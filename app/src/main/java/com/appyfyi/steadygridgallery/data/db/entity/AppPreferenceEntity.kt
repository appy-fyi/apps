package com.appyfyi.steadygridgallery.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Supported preference keys, per the spec: default_sort, grid_cell_dp, theme_mode. */
object PreferenceKeys {
    const val DEFAULT_SORT = "default_sort"
    const val GRID_CELL_DP = "grid_cell_dp"
    const val THEME_MODE = "theme_mode"
}

@Entity(tableName = "app_preference")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Instant,
)
