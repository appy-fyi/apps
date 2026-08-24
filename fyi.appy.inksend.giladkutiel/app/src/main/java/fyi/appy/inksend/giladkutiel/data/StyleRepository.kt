package fyi.appy.inksend.giladkutiel.data

import fyi.appy.inksend.giladkutiel.data.db.HandwritingFontDao
import fyi.appy.inksend.giladkutiel.data.db.HandwritingFontEntity
import fyi.appy.inksend.giladkutiel.data.db.StylePresetDao
import fyi.appy.inksend.giladkutiel.data.db.StylePresetEntity
import fyi.appy.inksend.giladkutiel.font.BundledFont
import kotlinx.coroutines.flow.Flow

class StyleRepository(
    private val styleDao: StylePresetDao,
    private val handwritingFontDao: HandwritingFontDao,
    private val prefs: PreferencesRepository,
) {
    fun observeStyles(): Flow<List<StylePresetEntity>> = styleDao.observeAll()

    fun observeHandwritingFonts(): Flow<List<HandwritingFontEntity>> = handwritingFontDao.observeAll()

    suspend fun getStyle(id: Long): StylePresetEntity? = styleDao.getById(id)

    suspend fun getDefaultStyle(): StylePresetEntity? = styleDao.getDefault()

    /** Idempotent: inserts the 3 free built-in presets only if the table is empty. */
    suspend fun ensureBuiltInPresetsSeeded() {
        if (styleDao.count() > 0) return
        val builtIns = listOf(
            StylePresetEntity(
                name = "Cursive Blue",
                fontFamily = BundledFont.DANCING_SCRIPT.id,
                textColorHex = "#FFFFFF",
                backgroundType = "solid",
                backgroundColorHex = "#2D6CDF",
                backgroundColorHex2 = "",
                isDefault = true,
                isBuiltIn = true,
            ),
            StylePresetEntity(
                name = "Bold Sunset",
                fontFamily = BundledFont.BEBAS_NEUE.id,
                textColorHex = "#FFFFFF",
                backgroundType = "gradient",
                backgroundColorHex = "#FF7A45",
                backgroundColorHex2 = "#FF4D8D",
                isDefault = false,
                isBuiltIn = true,
            ),
            StylePresetEntity(
                name = "Ink Classic",
                fontFamily = BundledFont.COURIER_PRIME.id,
                textColorHex = "#1C1B1F",
                backgroundType = "solid",
                backgroundColorHex = "#F2EFE9",
                backgroundColorHex2 = "",
                isDefault = false,
                isBuiltIn = true,
            ),
        )
        var defaultId = -1L
        builtIns.forEach { preset ->
            val id = styleDao.insert(preset)
            if (preset.isDefault) defaultId = id
        }
        if (defaultId >= 0) prefs.setDefaultStyleId(defaultId)
    }

    suspend fun saveStyle(preset: StylePresetEntity): Long =
        if (preset.id == 0L) styleDao.insert(preset) else {
            styleDao.update(preset)
            preset.id
        }

    suspend fun deleteStyle(preset: StylePresetEntity) = styleDao.delete(preset)

    suspend fun setDefault(id: Long) {
        styleDao.setDefault(id)
        prefs.setDefaultStyleId(id)
    }

    suspend fun saveHandwritingFont(font: HandwritingFontEntity): Long =
        if (font.id == 0L) handwritingFontDao.insert(font) else {
            handwritingFontDao.update(font)
            font.id
        }

    suspend fun getHandwritingFont(id: Long): HandwritingFontEntity? = handwritingFontDao.getById(id)
}
