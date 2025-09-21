package com.example.oblique_android.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,                       // server id or generated uuid
    val title: String = "",
    val platform: String = "leetcode",
    val platformUsername: String? = null,
    val unit: String = "",
    val baselineValue: Int = 0,
    val targetValue: Int = 0,
    val progress: Int = 0,
    val status: String = "active",
    val checkIntervalMs: Long = 3600000L,
    val deadline: Long = 0L,
    val evidenceJson: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pendingSync: Boolean = false
)
