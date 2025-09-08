package com.example.oblique_android.utils

import com.example.oblique_android.models.Goal

object GoalUtils {

    fun defaultGoalForPlatform(platform: String, unit: String, target: Int): Goal {
        return Goal(
            platform = platform,
            unit = unit,
            targetValue = target,
            progress = 0
        )
    }
}
