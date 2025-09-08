package com.example.oblique_android.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platform: String,       // e.g. "LeetCode", "Duolingo"
    val unit: String,           // e.g. "problems", "minutes"
    val targetValue: Int,       // e.g. 5 problems or 15 minutes
    val progress: Int = 0,      // how much user has completed
    val createdAt: Long = System.currentTimeMillis()
)
