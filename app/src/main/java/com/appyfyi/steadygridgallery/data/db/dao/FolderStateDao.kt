package com.appyfyi.steadygridgallery.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.appyfyi.steadygridgallery.data.db.entity.FolderStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderStateDao {

    @Upsert
    suspend fun upsert(folder: FolderStateEntity)

    @Upsert
    suspend fun upsertAll(folders: List<FolderStateEntity>)

    @Query("SELECT * FROM folder_state WHERE folderKey = :folderKey")
    suspend fun getByKey(folderKey: String): FolderStateEntity?

    @Query("SELECT * FROM folder_state WHERE folderKey = :folderKey")
    fun observeByKey(folderKey: String): Flow<FolderStateEntity?>

    @Query("UPDATE folder_state SET sortMode = :sortMode WHERE folderKey = :folderKey")
    suspend fun setSortMode(folderKey: String, sortMode: String)

    @Query("SELECT folderKey FROM folder_state")
    suspend fun allKeys(): List<String>
}
