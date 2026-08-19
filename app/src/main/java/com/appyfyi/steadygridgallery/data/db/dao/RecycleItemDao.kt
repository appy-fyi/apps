package com.appyfyi.steadygridgallery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.appyfyi.steadygridgallery.data.db.entity.RecycleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecycleItemDao {

    @Insert
    suspend fun insert(item: RecycleItemEntity): Long

    @Update
    suspend fun update(item: RecycleItemEntity)

    @Query(
        "SELECT * FROM recycle_item WHERE deleteState IN ('COPIED_PENDING_SYSTEM_DELETE', 'RECYCLED') " +
            "ORDER BY deletedAt DESC"
    )
    fun observeActive(): Flow<List<RecycleItemEntity>>

    @Query("SELECT * FROM recycle_item WHERE id = :id")
    suspend fun getById(id: Long): RecycleItemEntity?

    @Query(
        "SELECT originalMediaStoreId FROM recycle_item WHERE deleteState IN " +
            "('COPIED_PENDING_SYSTEM_DELETE', 'RECYCLED')"
    )
    suspend fun activeOriginalMediaStoreIds(): List<Long>

    @Query("UPDATE recycle_item SET deleteState = :deleteState WHERE id = :id")
    suspend fun setState(id: Long, deleteState: String)

    @Query("DELETE FROM recycle_item WHERE id = :id")
    suspend fun deleteRow(id: Long)
}
