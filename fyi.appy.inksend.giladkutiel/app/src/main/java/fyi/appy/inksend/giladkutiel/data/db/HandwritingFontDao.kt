package fyi.appy.inksend.giladkutiel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HandwritingFontDao {
    @Query("SELECT * FROM handwriting_fonts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HandwritingFontEntity>>

    @Query("SELECT * FROM handwriting_fonts WHERE id = :id")
    suspend fun getById(id: Long): HandwritingFontEntity?

    @Insert
    suspend fun insert(font: HandwritingFontEntity): Long

    @Update
    suspend fun update(font: HandwritingFontEntity)
}
