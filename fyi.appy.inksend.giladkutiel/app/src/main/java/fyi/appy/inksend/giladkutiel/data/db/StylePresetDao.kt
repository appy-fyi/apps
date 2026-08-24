package fyi.appy.inksend.giladkutiel.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StylePresetDao {
    @Query("SELECT * FROM style_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observeAll(): Flow<List<StylePresetEntity>>

    @Query("SELECT * FROM style_presets WHERE id = :id")
    suspend fun getById(id: Long): StylePresetEntity?

    @Query("SELECT * FROM style_presets WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): StylePresetEntity?

    @Query("SELECT COUNT(*) FROM style_presets")
    suspend fun count(): Int

    @Insert
    suspend fun insert(preset: StylePresetEntity): Long

    @Update
    suspend fun update(preset: StylePresetEntity)

    @Delete
    suspend fun delete(preset: StylePresetEntity)

    @Query("UPDATE style_presets SET isDefault = 0")
    suspend fun clearDefaults()

    @Query("UPDATE style_presets SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultFlag(id: Long)

    /** Exactly one row is true at a time: clear every default, then set the target row. */
    @Transaction
    suspend fun setDefault(id: Long) {
        clearDefaults()
        setDefaultFlag(id)
    }
}
