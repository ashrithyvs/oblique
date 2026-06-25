package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.ValidationConstants
import com.example.oblique_android.utils.WorkConstants
import com.example.oblique_android.validation.platform.DefaultPlatformSchedulingPolicy
import com.example.oblique_android.validation.platform.PlatformSchedulingPolicy

/**
 * Manages per-goal validation schedules independent of app-blocking monitoring.
 */
class GoalValidationScheduleManager(
    private val context: Context,
    private val scheduler: GoalValidationScheduler = GoalValidationScheduler(context),
    private val schedulingPolicy: PlatformSchedulingPolicy = DefaultPlatformSchedulingPolicy(),
) {
    fun scheduleGoal(goal: Goal) {
        if (goal.status != GoalStatusConstants.ACTIVE) {
            cancelGoal(goal.id)
            return
        }
        if (!isSchedulablePlatform(goal.platform)) {
            Log.d(TAG, "Skipping schedule for unsupported platform=${goal.platform}")
            return
        }
        scheduler.schedule(goal.id, goal.deadline, schedulingPolicy)
    }

    fun cancelGoal(goalId: String) {
        scheduler.cancelGoal(goalId)
    }

    fun rescheduleGoal(goal: Goal) {
        cancelGoal(goal.id)
        scheduleGoal(goal)
    }

    fun cancelAll() {
        scheduler.cancelAll()
    }

    fun refreshForGoals(goals: List<Goal>) {
        cancelAll()
        goals.filter { it.status == GoalStatusConstants.ACTIVE && isSchedulablePlatform(it.platform) }
            .forEach { scheduleGoal(it) }
    }

    private fun isSchedulablePlatform(platform: String): Boolean {
        val key = PlatformConstants.normalizePlatform(platform)
        return key == PlatformConstants.KEY_LEETCODE
    }

    companion object {
        private const val TAG = "GoalValidationScheduleManager"
    }
}
