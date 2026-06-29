package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository

sealed class SyncOutcome {
    data object NoChange : SyncOutcome()
    data class ProgressUpdated(val goal: Goal) : SyncOutcome()
    data class PeriodSatisfied(val goal: Goal) : SyncOutcome()
    data class Completed(val goal: Goal) : SyncOutcome()
    data class Error(val message: String) : SyncOutcome()
}

/**
 * Syncs validated period progress to the backend without optimistic local mutation.
 */
class GoalProgressSyncer(
    private val repo: GoalsRepository,
) {
    suspend fun syncPeriod(
        goal: Goal,
        periodDeadlineMs: Long,
        progress: Int,
        evidence: Map<String, Any>? = null,
    ): SyncOutcome {
        val alreadySatisfied = goal.lastSatisfiedPeriodDeadlineMs >= periodDeadlineMs
        if (progress == goal.progress && alreadySatisfied) return SyncOutcome.NoChange
        if (progress == goal.progress && progress < goal.targetValue) return SyncOutcome.NoChange

        return try {
            val updated = repo.recordPeriodProgress(goal.id, periodDeadlineMs, progress, evidence)
            if (progress >= goal.targetValue) {
                SyncOutcome.PeriodSatisfied(updated)
            } else {
                SyncOutcome.ProgressUpdated(updated)
            }
        } catch (e: Exception) {
            SyncOutcome.Error(e.message ?: "Sync failed")
        }
    }

    /** Legacy absolute-count sync for validators without period support. */
    suspend fun sync(
        goal: Goal,
        currentValue: Int,
        evidence: Map<String, Any>? = null,
    ): SyncOutcome {
        val computedProgress = goal.computedProgress(currentValue)
        if (computedProgress == goal.progress) return SyncOutcome.NoChange

        return try {
            if (computedProgress >= goal.targetValue && goal.status != com.example.oblique_android.utils.GoalStatusConstants.COMPLETED) {
                val completed = repo.completeGoal(goal.id, evidence)
                SyncOutcome.Completed(completed)
            } else {
                val updated = repo.updateGoalProgress(goal.id, currentValue, evidence)
                SyncOutcome.ProgressUpdated(updated)
            }
        } catch (e: Exception) {
            SyncOutcome.Error(e.message ?: "Sync failed")
        }
    }
}
