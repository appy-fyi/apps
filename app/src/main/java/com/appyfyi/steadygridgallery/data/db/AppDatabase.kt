package com.appyfyi.steadygridgallery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.appyfyi.steadygridgallery.data.db.dao.AppPreferenceDao
import com.appyfyi.steadygridgallery.data.db.dao.FolderStateDao
import com.appyfyi.steadygridgallery.data.db.dao.HiddenMediaDao
import com.appyfyi.steadygridgallery.data.db.dao.RecycleItemDao
import com.appyfyi.steadygridgallery.data.db.entity.AppPreferenceEntity
import com.appyfyi.steadygridgallery.data.db.entity.FolderStateEntity
import com.appyfyi.steadygridgallery.data.db.entity.HiddenMediaEntity
import com.appyfyi.steadygridgallery.data.db.entity.RecycleItemEntity

@Database(
    entities = [
        FolderStateEntity::class,
        RecycleItemEntity::class,
        AppPreferenceEntity::class,
        HiddenMediaEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderStateDao(): FolderStateDao
    abstract fun recycleItemDao(): RecycleItemDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun hiddenMediaDao(): HiddenMediaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "steady-gallery.db",
                )
                    // No production installs to migrate yet; the "Hidden Folders" -> "Hidden
                    // Photos" rework changed the folder_state/hidden_media schema outright.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
        }
    }
}
