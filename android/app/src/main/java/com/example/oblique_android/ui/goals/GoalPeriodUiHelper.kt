package com.example.oblique_android.ui.goals

import android.content.Context
import com.example.oblique_android.R
import com.example.oblique_android.models.Goal
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object GoalPeriodUiHelper {
    private val periodPolicy = GoalPeriodPolicy()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun formatPeriodStatus(context: Context, goal: Goal, bufferMs: Long, nowMs: Long): String {
        val timeOfDay = periodPolicy.deadlineTimeOfDayMs(goal)
        if (timeOfDay <= 0L) {
            return if (goal.progress >= goal.targetValue) {
                context.getString(R.string.period_complete)
            } else {
                ""
            }
        }

        val current = periodPolicy.currentPeriod(goal, bufferMs, nowMs)
        val deadlineLabel = timeFormat.format(current.deadlineMs)
        val satisfied = periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, nowMs)

        if (satisfied) {
            return context.getString(R.string.period_complete)
        }

        if (periodPolicy.isInUnblockWindow(goal, bufferMs, nowMs)) {
            val until = periodPolicy.unblockUntilMs(goal, bufferMs, nowMs)
            return context.getString(R.string.period_unlocked_until, formatDateTime(until, nowMs))
        }

        if (nowMs > current.deadlineMs && nowMs <= current.creditEndMs) {
            return context.getString(
                R.string.period_grace_until,
                timeFormat.format(current.creditEndMs),
            )
        }

        return context.getString(R.string.period_due_by, deadlineLabel)
    }

    private fun formatDateTime(timeMs: Long, nowMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val today = Calendar.getInstance().apply { timeInMillis = nowMs }
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val time = timeFormat.format(timeMs)
        return when {
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> time
            cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "tomorrow $time"
            else -> SimpleDateFormat("MMM d h:mm a", Locale.getDefault()).format(timeMs)
        }
    }
}
