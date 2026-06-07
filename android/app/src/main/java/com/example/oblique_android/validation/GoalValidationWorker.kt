package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.NetworkUtils
import com.example.oblique_android.utils.PrefsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that validates all active LeetCode goals
 * (only if internet is available).
 */
class GoalValidationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!NetworkUtils.hasInternet(applicationContext)) {
            Log.w("GoalValidationWorker", "Skipping: no internet connection")
            return@withContext Result.success()
        }

        val username = PrefsUtils.getPlatformUsername(applicationContext, "leetcode")
        if (username.isNullOrBlank()) {
            Log.w("GoalValidationWorker", "No username set for LeetCode platform")
            return@withContext Result.success()
        }

        val repo = GoalsRepository(applicationContext)
        val goals = repo.listGoals()
        val validator = GoalValidator(applicationContext)

        for (goal in goals) {
            if (goal.platform.lowercase() == "leetcode" && goal.status == "active") {
                validator.validate(goal, username)
            }
        }

        Result.success()
    }
}
