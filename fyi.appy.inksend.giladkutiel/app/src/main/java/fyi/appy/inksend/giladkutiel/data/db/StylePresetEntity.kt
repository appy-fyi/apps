package fyi.appy.inksend.giladkutiel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "style_presets")
data class StylePresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Bundled font asset id (e.g. "pacifico"), or a HandwritingFontEntity.filePath for a personal font. */
    val fontFamily: String,
    val textColorHex: String,
    /** "solid" or "gradient". */
    val backgroundType: String,
    val backgroundColorHex: String,
    /** Empty string when backgroundType is "solid". */
    val backgroundColorHex2: String,
    val isDefault: Boolean,
    val isBuiltIn: Boolean,
)
