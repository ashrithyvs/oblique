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

    suspend fun shouldBlockApps(): Boolean = withContext(Dispatchers.IO) {
        val goals = goalsRepo.listGoals().filter { it.status == GoalStatusConstants.ACTIVE }
        if (goals.isEmpty()) return@withContext false
        val bufferMs = PrefsUtils.getDeadlineBufferMs(appContext)
        val now = AppTime.nowMs()
        goals.any { goal -> shouldBlockGoal(goal, bufferMs, now) }
    }

    fun shouldBlockGoal(goal: Goal, bufferMs: Long, nowMs: Long = AppTime.nowMs()): Boolean {
        val satisfied = periodPolicy.isCurrentPeriodSatisfied(goal, bufferMs, nowMs)
        if (!satisfied) return true
        val until = periodPolicy.unblockUntilMs(goal, bufferMs, nowMs)
        return until <= 0L || nowMs >= until
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
