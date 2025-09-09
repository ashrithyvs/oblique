package com.example.oblique_android.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity used by DAOs and AppDatabase.
 * Keep this in package: com.example.oblique_android.entities
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // platform name like "LeetCode", "Duolingo", "Meditation"
    val platform: String = "",

    // unit like "problems", "minutes", "sessions"
    val unit: String = "",

    // target value (e.g., 2 problems / 30 minutes)
    val targetValue: Int = 0,

    // progress so far
    val progress: Int = 0,

    // createdAt as millis since epoch (Long). Room supports Long directly.
    val createdAt: Long = System.currentTimeMillis(),

    // optional deadline: millis or 0
    val deadline: Long = 0L
)
