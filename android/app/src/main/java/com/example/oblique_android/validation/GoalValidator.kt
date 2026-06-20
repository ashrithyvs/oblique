package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalsApi
import com.example.oblique_android.repository.GoalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Validates a goal using LeetCode API and updates backend if progress changed or goal completed.
 * Validation window: start of local calendar day containing the goal deadline → min(deadline, now).
 */
class GoalValidator(private val context: Context) {

    private val repo = GoalsRepository(context)
    private val leetCodeValidator = LeetCodeValidator(context)

    private fun validationWindow(deadlineMs: Long, now: Long): Pair<Long, Long> {
        if (deadlineMs <= 0L) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis to now
        }
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = deadlineMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val windowEnd = minOf(deadlineMs, now)
        return dayStart to windowEnd
    }

    suspend fun validate(goal: Goal, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val (from, to) = validationWindow(goal.deadline, now)
            if (from >= to) return@withContext false

            val result = leetCodeValidator.validate(username, from, to)
            val newProgress = result.totalSolved
            Log.i("GoalValidator", "Goal=${goal.id} progress=$newProgress / target=${goal.targetValue}")

            if (newProgress != goal.progress) {
                goal.progress = newProgress
                if (newProgress >= goal.targetValue && goal.status != "completed") {
                    repo.completeGoal(goal.id)
                    Log.i("GoalValidator", "Goal ${goal.id} completed and updated on backend")
                    return@withContext true
                }
                try {
                    val api = ApiClient.getClient(context).create(GoalsApi::class.java)
                    api.updateGoalProgress(goal.id, mapOf("progress" to newProgress))
                    Log.i("GoalValidator", "Progress updated (${goal.id}) = $newProgress")
                    true
                } catch (e: Exception) {
                    Log.w("GoalValidator", "Failed to update progress: ${e.message}")
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("GoalValidator", "validate() failed: ${e.message}")
            false
        }
    }
}
