package fyi.appy.taponceremote.giladkutiel.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDeviceDao {
    @Query("SELECT * FROM saved_devices ORDER BY lastSeenAtEpochMillis DESC")
    fun observeAll(): Flow<List<SavedDevice>>

    @Query("SELECT * FROM saved_devices WHERE lastUsed = 1 LIMIT 1")
    suspend fun getLastUsed(): SavedDevice?

    @Query("SELECT * FROM saved_devices WHERE id = :id")
    suspend fun getById(id: Long): SavedDevice?

    @Insert
    suspend fun insert(device: SavedDevice): Long

    @Query("UPDATE saved_devices SET lastUsed = 0")
    suspend fun clearLastUsed()

    @Query("UPDATE saved_devices SET lastUsed = 1, lastSeenAtEpochMillis = :timestamp WHERE id = :id")
    suspend fun setLastUsed(id: Long, timestamp: Long)

    @Transaction
    suspend fun markAsLastUsed(id: Long, timestamp: Long) {
        clearLastUsed()
        setLastUsed(id, timestamp)
    }

    @Delete
    suspend fun delete(device: SavedDevice)
}
