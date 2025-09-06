package com.example.oblique_android

import androidx.room.*

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Delete
    suspend fun deleteApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps")
    suspend fun clearAll()

    @Query("SELECT packageName FROM blocked_apps WHERE isBlocked = 1")
    suspend fun getBlockedPackageNames(): List<String>
}
