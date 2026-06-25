package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.ValidationConstants
import com.example.oblique_android.validation.platform.DefaultValidationWindowPolicy
import com.example.oblique_android.validation.platform.GoalProgressSyncer
import com.example.oblique_android.validation.platform.PlatformValidationContext
import com.example.oblique_android.validation.platform.PlatformValidationResult
import com.example.oblique_android.validation.platform.PlatformValidatorRegistry
import com.example.oblique_android.validation.platform.SyncOutcome
import com.example.oblique_android.validation.platform.ValidationWindowPolicy
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
            if (validator is com.example.oblique_android.validation.platform.LeetCodePlatformValidator) {
                com.example.oblique_android.validation.platform.LeetCodePlatformValidator(context)
            } else {
                validator
            }
        },
    ),
    private val windowPolicy: ValidationWindowPolicy = DefaultValidationWindowPolicy(),
    private val syncer: GoalProgressSyncer = GoalProgressSyncer(GoalsRepository(context)),
    private val goalsRepo: GoalsRepository = GoalsRepository(context),
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

        val validator = registry.get(goal.platform)
            ?: return@withContext ValidationOutcome.Failed(
                ValidationConstants.ERROR_NO_VALIDATOR,
                "No validator for platform ${goal.platform}",
            )

        val now = System.currentTimeMillis()
        val window = windowPolicy.window(goal.deadline, now)
            ?: return@withContext ValidationOutcome.Failed(
                ValidationConstants.ERROR_INVALID_WINDOW,
                "Validation window invalid",
            )

        val (from, to) = window
        val validationContext = PlatformValidationContext(
            goal = goal,
            username = username,
            windowStartMs = from,
            windowEndMs = to,
            nowMs = now,
        )

        when (val result = validator.validate(validationContext)) {
            is PlatformValidationResult.NoChange -> ValidationOutcome.NoChange
            is PlatformValidationResult.Failure -> {
                Log.w(TAG, "Validation failed goal=${goal.id} code=${result.code}")
                ValidationOutcome.Failed(result.code, result.message)
            }
            is PlatformValidationResult.Success -> {
                Log.i(TAG, "Goal=${goal.id} progress=${result.currentValue} / target=${goal.targetValue}")
                when (val sync = syncer.sync(goal, result.currentValue, result.evidence)) {
                    is SyncOutcome.NoChange -> ValidationOutcome.NoChange
                    is SyncOutcome.ProgressUpdated -> ValidationOutcome.Updated(goal.id)
                    is SyncOutcome.Completed -> ValidationOutcome.Completed(goal.id)
                    is SyncOutcome.Error -> ValidationOutcome.Failed("SYNC_ERROR", sync.message)
                }
            }
        }
    }

    companion object {
        private const val TAG = "GoalValidationService"
    }
}
