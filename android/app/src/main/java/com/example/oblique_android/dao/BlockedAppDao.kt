package com.example.oblique_android.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.oblique_android.entities.BlockedAppEntity

@Dao
interface BlockedAppDao {

    @Query("SELECT * FROM blocked_apps ORDER BY createdAt DESC")
    fun getAllLive(): LiveData<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps")
    suspend fun getAll(): List<BlockedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<BlockedAppEntity>)

    @Delete
    suspend fun deleteApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("DELETE FROM blocked_apps WHERE serverId = :sid")
    suspend fun deleteByServerId(sid: String)

    @Query("DELETE FROM blocked_apps")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(apps: List<BlockedAppEntity>) {
        clearAll()
        insertAll(apps)
    }
}

