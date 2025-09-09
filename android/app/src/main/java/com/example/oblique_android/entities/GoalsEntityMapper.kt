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
        id = id,
        platform = platform,
        unit = unit,
        targetValue = targetValue,
        progress = progress,
        createdAt = createdAt,
        deadline = deadline
    )
}
