package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class GoalPeriod(
    val deadlineMs: Long,
    val periodStartMs: Long,
    val creditEndMs: Long,
    val forfeited: Boolean,
    val satisfied: Boolean,
)

class GoalPeriodPolicy {

    fun deadlineTimeOfDayMs(goal: Goal): Long {
        if (goal.deadlineTimeOfDayMs > 0L) return goal.deadlineTimeOfDayMs
        if (goal.deadline <= 0L) return 0L
        val cal = Calendar.getInstance().apply { timeInMillis = goal.deadline }
        return TimeUnit.HOURS.toMillis(cal.get(Calendar.HOUR_OF_DAY).toLong()) +
            TimeUnit.MINUTES.toMillis(cal.get(Calendar.MINUTE).toLong()) +
            TimeUnit.SECONDS.toMillis(cal.get(Calendar.SECOND).toLong()) +
            cal.get(Calendar.MILLISECOND).toLong()
    }

    fun deadlineInstantForDay(dayStartMs: Long, timeOfDayMs: Long): Long =
        dayStartMs + timeOfDayMs

    fun startOfLocalDay(timeMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Deadline instant for the period currently in progress at [nowMs]. */
    fun currentPeriodDeadlineMs(goal: Goal, nowMs: Long, bufferMs: Long = 0L): Long {
        val timeOfDay = deadlineTimeOfDayMs(goal)
        if (timeOfDay <= 0L) return nowMs
        val todayStart = startOfLocalDay(nowMs)
        val todayDeadline = deadlineInstantForDay(todayStart, timeOfDay)
        return if (nowMs > todayDeadline + bufferMs) {
            todayDeadline + DAY_MS
        } else {
            todayDeadline
        }
    }

    fun nextPeriodDeadlineMs(deadlineMs: Long): Long = deadlineMs + DAY_MS

    fun buildPeriod(
        goal: Goal,
        deadlineMs: Long,
        bufferMs: Long,
        nowMs: Long,
    ): GoalPeriod {
        val periodStartMs = deadlineMs - DAY_MS
        val creditEndMs = deadlineMs + bufferMs
        val satisfied = goal.lastSatisfiedPeriodDeadlineMs >= deadlineMs
        val forfeited = !satisfied && nowMs > creditEndMs
        return GoalPeriod(
            deadlineMs = deadlineMs,
            periodStartMs = periodStartMs,
            creditEndMs = creditEndMs,
            forfeited = forfeited,
            satisfied = satisfied,
        )
    }

    /** Open periods oldest-first (max lookback 14 days). */
    fun openPeriods(goal: Goal, bufferMs: Long, nowMs: Long): List<GoalPeriod> {
        val timeOfDay = deadlineTimeOfDayMs(goal)
        if (timeOfDay <= 0L) return emptyList()

        val currentDeadline = currentPeriodDeadlineMs(goal, nowMs, bufferMs)
        val periods = mutableListOf<GoalPeriod>()
        var deadline = currentDeadline
        repeat(MAX_LOOKBACK_DAYS) {
            val period = buildPeriod(goal, deadline, bufferMs, nowMs)
            if (!period.forfeited && !period.satisfied) {
                periods.add(0, period)
            }
            deadline -= DAY_MS
        }
        return periods
    }

    fun currentPeriod(goal: Goal, bufferMs: Long, nowMs: Long): GoalPeriod {
        val deadline = currentPeriodDeadlineMs(goal, nowMs, bufferMs)
        return buildPeriod(goal, deadline, bufferMs, nowMs)
    }

    fun isCurrentPeriodSatisfied(goal: Goal, bufferMs: Long, nowMs: Long): Boolean {
        val period = currentPeriod(goal, bufferMs, nowMs)
        if (goal.lastSatisfiedPeriodDeadlineMs >= period.deadlineMs) return true
        return goal.progress >= goal.targetValue
    }

    /** After satisfying period at [satisfiedDeadlineMs], apps unblock until next deadline + buffer. */
    fun unblockUntilMs(satisfiedDeadlineMs: Long, bufferMs: Long): Long =
        nextPeriodDeadlineMs(satisfiedDeadlineMs) + bufferMs

    fun unblockUntilMs(goal: Goal, bufferMs: Long, nowMs: Long): Long {
        val last = goal.lastSatisfiedPeriodDeadlineMs
        if (last <= 0L) return 0L
        return unblockUntilMs(last, bufferMs)
    }

    fun creditWindowForPeriod(period: GoalPeriod, nowMs: Long): Pair<Long, Long> {
        val end = minOf(nowMs, period.creditEndMs)
        return period.periodStartMs to end
    }

    companion object {
        const val DAY_MS = 86_400_000L
        const val MAX_BUFFER_MS = 3 * 60 * 60 * 1000L
        private const val MAX_LOOKBACK_DAYS = 14
    }
}
