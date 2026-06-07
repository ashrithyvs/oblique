package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Schedules goal validations periodically leading up to deadlines.
 * - Every 1h starting 6h before deadline
 * - +10m, +5m, +1m, and exact deadline runs
 */
class GoalValidationScheduler(private val context: Context) {

    private val wm = WorkManager.getInstance(context)

    fun schedule(goalId: String, deadline: Long) {
        val now = System.currentTimeMillis()
        val start = deadline - TimeUnit.HOURS.toMillis(6)

        var next = max(now, start)
        while (next < deadline - TimeUnit.MINUTES.toMillis(10)) {
            enqueue(next - now)
            next += TimeUnit.HOURS.toMillis(1)
        }

        val finalTimes = listOf(10L, 5L, 1L, 0L)
        for (m in finalTimes) {
            val delay = (deadline - TimeUnit.MINUTES.toMillis(m)) - now
            if (delay > 0) enqueue(delay)
        }

        Log.i("GoalValidationScheduler", "Scheduled validations for goal=$goalId until deadline=$deadline")
    }

    private fun enqueue(delayMs: Long) {
        val req = OneTimeWorkRequestBuilder<GoalValidationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("goal_validation")
            .build()
        wm.enqueue(req)
    }

    fun cancelAll() {
        wm.cancelAllWorkByTag("goal_validation")
    }
}
