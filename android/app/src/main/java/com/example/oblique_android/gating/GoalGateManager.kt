package com.example.oblique_android.gating

import android.content.Context
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.AppTime
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoalGateManager(
    private val context: Context,
    private val goalsRepo: GoalsRepository = GoalsRepository(context),
    private val periodPolicy: GoalPeriodPolicy = GoalPeriodPolicy(),
) {
    private val appContext = context.applicationContext

    /**
     * True when blocked apps should be overlay-blocked: current period not met and past deadline+buffer,
     * or past the post-satisfaction unblock window with the new period not met after buffer.
     */
    suspend fun needsForegroundMonitoring(): Boolean = withContext(Dispatchers.IO) {
        val goals = goalsRepo.listGoals().filter { it.status == GoalStatusConstants.ACTIVE }
        if (goals.isEmpty()) return@withContext false
        val bufferMs = PrefsUtils.getDeadlineBufferMs(appContext)
        val now = AppTime.nowMs()
        goals.any { goal -> needsForegroundMonitoring(goal, bufferMs, now) }
    }

    fun needsForegroundMonitoring(goal: Goal, bufferMs: Long, nowMs: Long = AppTime.nowMs()): Boolean {
        if (periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, nowMs)) {
            val until = periodPolicy.unblockUntilMs(goal, bufferMs, nowMs)
            if (until > nowMs) return false
        }

        val todayDeadline = todayDeadlineMs(goal, nowMs) ?: return false
        if (nowMs < todayDeadline) return false

        val creditEnd = todayDeadline + bufferMs
        if (nowMs <= creditEnd) return false
        return !periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, nowMs)
    }

    /** @deprecated Use [needsForegroundMonitoring] — kept as alias for callers. */
    suspend fun shouldBlockApps(): Boolean = needsForegroundMonitoring()

    fun shouldBlockGoal(goal: Goal, bufferMs: Long, nowMs: Long = AppTime.nowMs()): Boolean =
        needsForegroundMonitoring(goal, bufferMs, nowMs)

    suspend fun nextMonitoringTransitionMs(): Long? = withContext(Dispatchers.IO) {
        val goals = goalsRepo.listGoals().filter { it.status == GoalStatusConstants.ACTIVE }
        if (goals.isEmpty()) return@withContext null
        val bufferMs = PrefsUtils.getDeadlineBufferMs(appContext)
        val now = AppTime.nowMs()
        if (goals.any { needsForegroundMonitoring(it, bufferMs, now) }) return@withContext null

        goals.mapNotNull { nextTransitionForGoal(it, bufferMs, now) }
            .minOrNull()
            ?.takeIf { it > now }
    }

    private fun nextTransitionForGoal(goal: Goal, bufferMs: Long, nowMs: Long): Long? {
        if (periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, nowMs)) {
            val until = periodPolicy.unblockUntilMs(goal, bufferMs, nowMs)
            if (until > nowMs) return until
        }

        val todayDeadline = todayDeadlineMs(goal, nowMs) ?: return null
        val creditEnd = todayDeadline + bufferMs
        if (nowMs <= creditEnd) return creditEnd
        return null
    }

    private fun todayDeadlineMs(goal: Goal, nowMs: Long): Long? {
        val timeOfDay = periodPolicy.deadlineTimeOfDayMs(goal)
        if (timeOfDay <= 0L) return null
        val dayStart = periodPolicy.startOfLocalDay(nowMs)
        return periodPolicy.deadlineInstantForDay(dayStart, timeOfDay)
    }

    suspend fun unlockedUntilMs(): Long? = withContext(Dispatchers.IO) {
        val goals = goalsRepo.listGoals().filter { it.status == GoalStatusConstants.ACTIVE }
        if (goals.isEmpty()) return@withContext null
        val bufferMs = PrefsUtils.getDeadlineBufferMs(appContext)
        val now = AppTime.nowMs()
        val times = goals.mapNotNull { goal ->
            if (periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, now)) {
                periodPolicy.unblockUntilMs(goal, bufferMs, now).takeIf { it > now }
            } else null
        }
        times.minOrNull()
    }
}
