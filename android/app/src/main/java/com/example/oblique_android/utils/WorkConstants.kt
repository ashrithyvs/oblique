package com.example.oblique_android.utils

import java.util.concurrent.TimeUnit

object WorkConstants {
    const val UNIQUE_WORK_PREFIX = "goal_validation_"
    const val SCHEDULE_LEAD_HOURS = 6L
    val SCHEDULE_LEAD_MS: Long = TimeUnit.HOURS.toMillis(SCHEDULE_LEAD_HOURS)
    val SCHEDULE_HOURLY_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(1)
    val SCHEDULE_FINAL_OFFSETS_MINUTES = listOf(10L, 5L, 1L, 0L)

    fun uniqueWorkName(goalId: String, slotIndex: Int): String =
        "$UNIQUE_WORK_PREFIX${goalId}_$slotIndex"
}
