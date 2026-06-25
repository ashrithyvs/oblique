package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.oblique_android.utils.NetworkUtils
import com.example.oblique_android.utils.ValidationConstants

/**
 * Validates a single goal identified by [ValidationConstants.EXTRA_GOAL_ID] in input data.
 */
class GoalValidationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!NetworkUtils.hasInternet(applicationContext)) {
            Log.w(TAG, "Skipping: no internet connection")
            return Result.success()
        }

        val goalId = inputData.getString(ValidationConstants.EXTRA_GOAL_ID)
        if (goalId.isNullOrBlank()) {
            Log.w(TAG, "Missing goal id in work input")
            return Result.failure()
        }

        val service = GoalValidationService(applicationContext)
        when (val outcome = service.validateGoalById(goalId)) {
            is ValidationOutcome.Failed -> {
                Log.w(TAG, "Validation failed for goal=$goalId code=${outcome.code}")
            }
            is ValidationOutcome.Updated -> {
                Log.i(TAG, "Progress updated for goal=$goalId")
            }
            is ValidationOutcome.Completed -> {
                Log.i(TAG, "Goal completed: $goalId")
            }
            else -> Log.d(TAG, "Validation outcome for goal=$goalId: $outcome")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "GoalValidationWorker"
    }
}
