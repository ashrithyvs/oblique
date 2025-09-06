package com.example.oblique_android

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    val icon: ByteArray? = null // store app icon as PNG byte array
)
