package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.GoalsApi
import com.example.oblique_android.repository.GoalsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Validates a goal using LeetCode API and updates backend if:
 * - progress changed (always update)
 * - goal completed (marks complete)
 */
class GoalValidator(private val context: Context) {

    private val repo = GoalsRepository(context)
    private val leetCodeValidator = LeetCodeValidator(context)

    suspend fun validate(goal: Goal, username: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val from = goal.deadline - 24 * 60 * 60 * 1000L // last 24h window
            Log.i("GoalValidator"," from${from}, deadline${goal.deadline}")
            val result = leetCodeValidator.validate(username, from, now)
            Log.i("GoalValidator","result = ${result.toString()}");
            val newProgress = result.totalSolved
            Log.i("GoalValidator", "Goal=${goal.id} progress=$newProgress / target=${goal.targetValue}")

            // If progress changed, update backend (progress field)
            if (newProgress != goal.progress) {
                goal.progress = newProgress
                // Reuse completeGoal() only for final completion
                if (newProgress >= goal.targetValue && goal.status != "completed") {
                    repo.completeGoal(goal.id)
                    Log.i("GoalValidator", "✅ Goal ${goal.id} completed and updated on backend")
                    return@withContext true
                } else {
                    // Send lightweight progress update call (use same createGoal schema)
                    try {
                        val body = mutableMapOf<String, Integer>()
                        body["progress"] = Integer(newProgress)
                        val api = ApiClient
                            .getClient(context)
                            .create(GoalsApi::class.java)
                        api.updateGoalProgress(goal.id, body)
                        Log.i("GoalValidator", "Progress updated (${goal.id}) = $newProgress")
                        true
                    } catch (e: Exception) {
                        Log.w("GoalValidator", "Failed to update progress: ${e.message}")
                        false
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e("GoalValidator", "validate() failed: ${e.message}")
            false
        }
    }
}
