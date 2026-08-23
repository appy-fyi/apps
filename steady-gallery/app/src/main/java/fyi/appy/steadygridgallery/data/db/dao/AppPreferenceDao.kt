package fyi.appy.steadygridgallery.data.db.dao

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Insert
import fyi.appy.steadygridgallery.data.db.entity.AppPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPreferenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: AppPreferenceEntity)

    @Query("SELECT * FROM app_preference")
    fun observeAll(): Flow<List<AppPreferenceEntity>>

    @Query("SELECT * FROM app_preference")
    suspend fun getAllOnce(): List<AppPreferenceEntity>

    @Query("SELECT * FROM app_preference WHERE key = :key")
    suspend fun getByKey(key: String): AppPreferenceEntity?
}
