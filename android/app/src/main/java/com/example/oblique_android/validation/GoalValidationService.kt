package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.AppTime
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.TimeProvider
import com.example.oblique_android.utils.ValidationConstants
import com.example.oblique_android.utils.ValidationDevLogger
import com.example.oblique_android.validation.platform.GoalPeriodPolicy
import com.example.oblique_android.validation.platform.GoalProgressSyncer
import com.example.oblique_android.validation.platform.LeetCodePlatformValidator
import com.example.oblique_android.validation.platform.PlatformValidationContext
import com.example.oblique_android.validation.platform.PlatformValidationResult
import com.example.oblique_android.validation.platform.PlatformValidatorRegistry
import com.example.oblique_android.validation.platform.SyncOutcome
import com.example.oblique_android.validation.platform.ValidationWindowPolicy
import com.example.oblique_android.validation.platform.DefaultValidationWindowPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ValidationOutcome {
    data object Skipped : ValidationOutcome()
    data object NoChange : ValidationOutcome()
    data class Updated(val goalId: String) : ValidationOutcome()
    data class Completed(val goalId: String) : ValidationOutcome()
    data class Failed(val code: String, val message: String? = null) : ValidationOutcome()
}

class GoalValidationService(
    private val context: Context,
    private val registry: PlatformValidatorRegistry = PlatformValidatorRegistry(
        PlatformValidatorRegistry.defaultValidators().map { validator ->
            if (validator is LeetCodePlatformValidator) {
                LeetCodePlatformValidator(context)
            } else {
                validator
            }
        },
    ),
    private val windowPolicy: ValidationWindowPolicy = DefaultValidationWindowPolicy(),
    private val periodPolicy: GoalPeriodPolicy = GoalPeriodPolicy(),
    private val allocator: PeriodProgressAllocator = PeriodProgressAllocator(periodPolicy),
    private val syncer: GoalProgressSyncer = GoalProgressSyncer(GoalsRepository(context)),
    private val goalsRepo: GoalsRepository = GoalsRepository(context),
    private val timeProvider: TimeProvider = AppTime.provider(),
) {
    suspend fun validateGoalById(goalId: String): ValidationOutcome = withContext(Dispatchers.IO) {
        val goals = goalsRepo.listGoals()
        val goal = goals.find { it.id == goalId }
            ?: return@withContext ValidationOutcome.Failed("GOAL_NOT_FOUND", "Goal $goalId not found")

        val platformKey = PlatformConstants.normalizePlatform(goal.platform)
        val username = PrefsUtils.getPlatformUsername(context, platformKey)
        validateGoal(goal, username)
    }

    suspend fun validateGoal(goal: Goal, username: String?): ValidationOutcome = withContext(Dispatchers.IO) {
        if (goal.status != GoalStatusConstants.ACTIVE) {
            return@withContext ValidationOutcome.Skipped
        }

        val platformKey = PlatformConstants.normalizePlatform(goal.platform)
        val now = timeProvider.nowMs()
        val bufferMs = PrefsUtils.getDeadlineBufferMs(context)
        val timeOfDay = periodPolicy.deadlineTimeOfDayMs(goal)

        if (timeOfDay > 0L && platformKey == PlatformConstants.KEY_LEETCODE) {
            return@withContext validateWithPeriodAllocation(goal, username, bufferMs, now)
        }

        return@withContext validateLegacyWindow(goal, username, now)
    }

    private suspend fun validateWithPeriodAllocation(
        goal: Goal,
        username: String?,
        bufferMs: Long,
        now: Long,
    ): ValidationOutcome {
        if (username.isNullOrBlank()) {
            return ValidationOutcome.Failed(
                ValidationConstants.ERROR_MISSING_USERNAME,
                "Platform username not configured",
            )
        }

        val leetCodeValidator = registry.get(goal.platform) as? LeetCodePlatformValidator
            ?: return ValidationOutcome.Failed(
                ValidationConstants.ERROR_NO_VALIDATOR,
                "No validator for platform ${goal.platform}",
            )

        val currentPeriod = periodPolicy.currentPeriod(goal, bufferMs, now)
        ValidationDevLogger.logValidationStart(
            goalId = goal.id,
            platform = goal.platform,
            windowStartMs = currentPeriod.periodStartMs,
            windowEndMs = minOf(now, currentPeriod.creditEndMs),
            storedProgress = goal.progress,
            targetValue = goal.targetValue,
            baselineValue = goal.baselineValue,
        )

        val submissions = leetCodeValidator.fetchTimestampedSubmissions(username)
            ?: return ValidationOutcome.Failed(
                ValidationConstants.ERROR_FETCH_SUBMISSION_FAILED,
                "Failed to fetch submissions",
            )

        val allocation = allocator.allocate(goal, submissions, bufferMs, now)
        val currentProgress = allocation.currentPeriodProgress.coerceAtMost(goal.targetValue)

        ValidationDevLogger.logPlatformResult(
            goal.id,
            if (currentProgress == goal.progress) "NO_CHANGE" else "SUCCESS",
            currentProgress,
            goal.progress,
            mapOf(
                "periodDeadlineMs" to allocation.currentPeriodDeadlineMs,
                "totalAssigned" to allocation.totalAssigned,
                "progressByDeadline" to allocation.progressByDeadline,
            ),
            goal.baselineValue,
        )

        if (currentProgress == goal.progress &&
            goal.lastSatisfiedPeriodDeadlineMs >= allocation.currentPeriodDeadlineMs
        ) {
            ValidationDevLogger.logSyncSkipped(goal.id, "period progress unchanged")
            return ValidationOutcome.NoChange
        }

        return when (
            val sync = syncer.syncPeriod(
                goal,
                allocation.currentPeriodDeadlineMs,
                currentProgress,
                mapOf(
                    "platform" to platformKey(goal.platform),
                    "progressByDeadline" to allocation.progressByDeadline,
                    "submissionCount" to submissions.size,
                ),
            )
        ) {
            is SyncOutcome.NoChange -> ValidationOutcome.NoChange
            is SyncOutcome.ProgressUpdated -> {
                Log.i(TAG, "Goal=${goal.id} period progress=$currentProgress / target=${goal.targetValue}")
                ValidationOutcome.Updated(goal.id)
            }
            is SyncOutcome.PeriodSatisfied -> {
                Log.i(TAG, "Goal=${goal.id} period satisfied at deadline=${allocation.currentPeriodDeadlineMs}")
                ValidationOutcome.Updated(goal.id)
            }
            is SyncOutcome.Completed -> ValidationOutcome.Completed(goal.id)
            is SyncOutcome.Error -> ValidationOutcome.Failed("SYNC_ERROR", sync.message)
        }
    }

    private suspend fun validateLegacyWindow(
        goal: Goal,
        username: String?,
        now: Long,
    ): ValidationOutcome {
        val validator = registry.get(goal.platform)
            ?: return ValidationOutcome.Failed(
                ValidationConstants.ERROR_NO_VALIDATOR,
                "No validator for platform ${goal.platform}",
            )

        val window = windowPolicy.window(goal.deadline, now)
            ?: return ValidationOutcome.Failed(
                ValidationConstants.ERROR_INVALID_WINDOW,
                "Validation window invalid",
            )

        val (from, to) = window
        ValidationDevLogger.logValidationStart(
            goalId = goal.id,
            platform = goal.platform,
            windowStartMs = from,
            windowEndMs = to,
            storedProgress = goal.progress,
            targetValue = goal.targetValue,
            baselineValue = goal.baselineValue,
        )

        val validationContext = PlatformValidationContext(
            goal = goal,
            username = username,
            windowStartMs = from,
            windowEndMs = to,
            nowMs = now,
        )

        return when (val result = validator.validate(validationContext)) {
            is PlatformValidationResult.NoChange -> ValidationOutcome.NoChange
            is PlatformValidationResult.Failure -> {
                Log.w(TAG, "Validation failed goal=${goal.id} code=${result.code}")
                ValidationOutcome.Failed(result.code, result.message)
            }
            is PlatformValidationResult.Success -> {
                when (val sync = syncer.sync(goal, result.currentValue, result.evidence)) {
                    is SyncOutcome.NoChange -> ValidationOutcome.NoChange
                    is SyncOutcome.ProgressUpdated -> ValidationOutcome.Updated(goal.id)
                    is SyncOutcome.Completed -> ValidationOutcome.Completed(goal.id)
                    is SyncOutcome.PeriodSatisfied -> ValidationOutcome.Updated(goal.id)
                    is SyncOutcome.Error -> ValidationOutcome.Failed("SYNC_ERROR", sync.message)
                }
            }
        }
    }

    private fun platformKey(platform: String) = PlatformConstants.normalizePlatform(platform)

    companion object {
        private const val TAG = "GoalValidationService"
    }
}
