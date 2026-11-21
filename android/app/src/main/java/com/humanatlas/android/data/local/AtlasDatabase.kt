package com.humanatlas.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StudyEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AtlasDatabase : RoomDatabase() {

    abstract fun studyEntryDao(): StudyEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AtlasDatabase? = null

        fun getInstance(context: Context): AtlasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtlasDatabase::class.java,
                    "human_atlas_study.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
