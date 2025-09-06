package com.example.oblique_android

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BlockedAppEntity::class], version = 2, exportSchema = false)
abstract class BlockedAppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: BlockedAppDatabase? = null

        fun getDatabase(context: Context): BlockedAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlockedAppDatabase::class.java,
                    "blocked_apps_db"
                )
                    // TODO: For production, handle migrations properly
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
