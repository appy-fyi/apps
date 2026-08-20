package com.appyfyi.steadygridgallery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenMediaDao {

    @Insert
    suspend fun insert(item: HiddenMediaEntity): Long

    @Query(
        "SELECT * FROM hidden_media WHERE state IN ('COPIED_PENDING_SYSTEM_DELETE', 'HIDDEN') " +
            "ORDER BY hiddenAt DESC",
    )
    fun observeActive(): Flow<List<HiddenMediaEntity>>

    @Query("SELECT * FROM hidden_media WHERE id = :id")
    suspend fun getById(id: Long): HiddenMediaEntity?

    @Query(
        "SELECT originalMediaStoreId FROM hidden_media WHERE state IN " +
            "('COPIED_PENDING_SYSTEM_DELETE', 'HIDDEN')",
    )
    suspend fun activeOriginalMediaStoreIds(): List<Long>

    @Query("UPDATE hidden_media SET state = :state WHERE id = :id")
    suspend fun setState(id: Long, state: String)

    @Query("DELETE FROM hidden_media WHERE id = :id")
    suspend fun deleteRow(id: Long)
}
