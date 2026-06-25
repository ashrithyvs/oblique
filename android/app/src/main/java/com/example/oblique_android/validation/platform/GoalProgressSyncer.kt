package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.GoalStatusConstants

sealed class SyncOutcome {
    data object NoChange : SyncOutcome()
    data class ProgressUpdated(val goal: Goal) : SyncOutcome()
    data class Completed(val goal: Goal) : SyncOutcome()
    data class Error(val message: String) : SyncOutcome()
}

/**
 * Syncs validated progress to the backend without optimistic local mutation.
 */
class GoalProgressSyncer(
    private val repo: GoalsRepository,
) {
    suspend fun sync(
        goal: Goal,
        currentValue: Int,
        evidence: Map<String, Any>? = null,
    ): SyncOutcome {
        if (currentValue == goal.progress) return SyncOutcome.NoChange

        return try {
            if (currentValue >= goal.targetValue && goal.status != GoalStatusConstants.COMPLETED) {
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
