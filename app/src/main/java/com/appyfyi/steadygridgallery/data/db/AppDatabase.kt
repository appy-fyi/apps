package com.appyfyi.steadygridgallery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.appyfyi.steadygridgallery.data.db.dao.AppPreferenceDao
import com.appyfyi.steadygridgallery.data.db.dao.FolderStateDao
import com.appyfyi.steadygridgallery.data.db.dao.RecycleItemDao
import com.appyfyi.steadygridgallery.data.db.entity.AppPreferenceEntity
import com.appyfyi.steadygridgallery.data.db.entity.FolderStateEntity
import com.appyfyi.steadygridgallery.data.db.entity.RecycleItemEntity

@Database(
    entities = [FolderStateEntity::class, RecycleItemEntity::class, AppPreferenceEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderStateDao(): FolderStateDao
    abstract fun recycleItemDao(): RecycleItemDao
    abstract fun appPreferenceDao(): AppPreferenceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "steady-gallery.db",
                ).build().also { instance = it }
            }
        }
    }
}
