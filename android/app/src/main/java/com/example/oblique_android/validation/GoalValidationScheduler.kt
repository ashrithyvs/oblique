package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oblique_android.utils.ValidationConstants
import com.example.oblique_android.utils.WorkConstants
import com.example.oblique_android.validation.platform.DefaultPlatformSchedulingPolicy
import com.example.oblique_android.validation.platform.PlatformSchedulingPolicy
import java.util.concurrent.TimeUnit

class GoalValidationScheduler(
    private val context: Context,
    private val schedulingPolicy: PlatformSchedulingPolicy = DefaultPlatformSchedulingPolicy(),
) {
    private val wm = WorkManager.getInstance(context)

    fun schedule(
        goalId: String,
        deadline: Long,
        policy: PlatformSchedulingPolicy = schedulingPolicy,
    ) {
        cancelGoal(goalId)

        val now = System.currentTimeMillis()
        val delays = policy.computeDelays(deadline, now)
        if (delays.isEmpty()) {
            Log.i(TAG, "No future validation slots for goal=$goalId")
            return
        }

        delays.forEachIndexed { index, delayMs ->
            val input = Data.Builder()
                .putString(ValidationConstants.EXTRA_GOAL_ID, goalId)
                .build()

            val req = OneTimeWorkRequestBuilder<GoalValidationWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .addTag(ValidationConstants.WORK_TAG)
                .addTag(goalWorkTag(goalId))
                .build()

            wm.enqueueUniqueWork(
                WorkConstants.uniqueWorkName(goalId, index),
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }

        Log.i(TAG, "Scheduled ${delays.size} validations for goal=$goalId until deadline=$deadline")
    }

    fun cancelGoal(goalId: String) {
        wm.cancelAllWorkByTag(goalWorkTag(goalId))
    }

    fun cancelAll() {
        wm.cancelAllWorkByTag(ValidationConstants.WORK_TAG)
    }

    private fun goalWorkTag(goalId: String): String =
        "${WorkConstants.UNIQUE_WORK_PREFIX}goal_$goalId"

    companion object {
        private const val TAG = "GoalValidationScheduler"
    }
}
