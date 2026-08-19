package com.appyfyi.steadygridgallery.data.prefs

import com.appyfyi.steadygridgallery.data.db.dao.AppPreferenceDao
import com.appyfyi.steadygridgallery.data.db.entity.AppPreferenceEntity
import com.appyfyi.steadygridgallery.data.db.entity.PreferenceKeys
import com.appyfyi.steadygridgallery.data.db.entity.SortMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.time.Instant

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gridCellDp: Int = 128,
    val defaultSort: SortMode = SortMode.DATE_DESC,
)

/**
 * Settings live only in Room (never in Compose `remember` state) so they survive process death,
 * per the "persistent options" feature.
 */
class SettingsRepository(
    private val dao: AppPreferenceDao,
    externalScope: CoroutineScope,
) {
    val settings: StateFlow<AppSettings> = dao.observeAll()
        .map { rows -> rows.associate { it.key to it.value }.toAppSettings() }
        .stateIn(externalScope, SharingStarted.Eagerly, AppSettings())

    private fun Map<String, String>.toAppSettings(): AppSettings = AppSettings(
        themeMode = this[PreferenceKeys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        gridCellDp = this[PreferenceKeys.GRID_CELL_DP]?.toIntOrNull() ?: 128,
        defaultSort = this[PreferenceKeys.DEFAULT_SORT]?.let { runCatching { SortMode.valueOf(it) }.getOrNull() }
            ?: SortMode.DATE_DESC,
    )

    suspend fun setThemeMode(mode: ThemeMode) = write(PreferenceKeys.THEME_MODE, mode.name)

    suspend fun setGridCellDp(dp: Int) = write(PreferenceKeys.GRID_CELL_DP, dp.toString())

    suspend fun setDefaultSort(sortMode: SortMode) = write(PreferenceKeys.DEFAULT_SORT, sortMode.name)

    private suspend fun write(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.upsertPreference(AppPreferenceEntity(key = key, value = value, updatedAt = Instant.now()))
    }
}
