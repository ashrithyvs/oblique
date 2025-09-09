package com.example.oblique_android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.oblique_android.BlockedAppDao
import com.example.oblique_android.BlockedAppEntity
import com.example.oblique_android.data.GoalDao
import com.example.oblique_android.entities.GoalEntity

/**
 * Single app-wide RoomDatabase. Make sure GoalEntity is listed here.
 * For dev speed we use fallbackToDestructiveMigration(); replace with proper migrations later.
 */
@Database(
    entities = [BlockedAppEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    // Use destructive for dev to unblock builds. Replace with migrations in prod.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
