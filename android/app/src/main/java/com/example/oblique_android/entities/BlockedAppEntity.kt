package com.example.oblique_android.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,   // we keep packageName as the unique local key
    val appName: String,
    val isBlocked: Boolean = true,
    val icon: ByteArray? = null,

    // optional server id (if created on server)
    val serverId: String? = null,

    val pendingSync: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
