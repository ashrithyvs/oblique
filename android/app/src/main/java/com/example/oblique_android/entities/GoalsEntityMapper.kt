package com.example.oblique_android.entities

import com.example.oblique_android.models.Goal

fun GoalEntity.toDomain(): Goal {
    return Goal(
        id = id,
        platform = platform,
        unit = unit,
        targetValue = targetValue,
        progress = progress,
        createdAt = createdAt,
        deadline = deadline
    )
}

fun Goal.toEntity(): GoalEntity {
    return GoalEntity(
        id = if (id.isBlank()) java.util.UUID.randomUUID().toString() else id,
        title = "",
        platform = platform,
        platformUsername = null,
        unit = unit,
        baselineValue = 0,
        targetValue = targetValue,
        progress = progress,
        status = "active",
        checkIntervalMs = 3600000L,
        deadline = deadline,
        evidenceJson = null,
        completedAt = null,
        createdAt = createdAt,
        updatedAt = createdAt,
        pendingSync = true
    )
}
