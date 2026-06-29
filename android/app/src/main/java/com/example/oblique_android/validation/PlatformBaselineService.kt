package com.example.oblique_android.validation

import android.content.Context
import com.example.oblique_android.R
import com.example.oblique_android.models.Goal
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.GoalStatusConstants
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.validation.platform.DefaultValidationWindowPolicy
import com.example.oblique_android.validation.platform.PlatformUserVerification
import com.example.oblique_android.validation.platform.PlatformValidatorRegistry
import com.example.oblique_android.validation.platform.ValidationWindowPolicy

sealed class BaselineSetupResult {
    data object Success : BaselineSetupResult()
    data class Failure(val message: String) : BaselineSetupResult()
}

class PlatformBaselineService(
    context: Context,
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
    private val goalsRepo: GoalsRepository = GoalsRepository(context),
) {
    private val appContext = context.applicationContext

    suspend fun setupBaselinesForGoals(
        goals: List<Goal>,
        usernames: Map<String, String>,
        onStatus: (String) -> Unit,
    ): BaselineSetupResult {
        val activeGoals = goals.filter { it.status == GoalStatusConstants.ACTIVE }
        if (activeGoals.isEmpty()) return BaselineSetupResult.Success

        val platformsNeeded = activeGoals
            .map { PlatformConstants.normalizePlatform(it.platform) }
            .distinct()

        for (platform in platformsNeeded) {
            val username = usernames[platform]?.trim().orEmpty()
            if (username.isBlank()) {
                return BaselineSetupResult.Failure(
                    appContext.getString(R.string.baseline_username_required, platform),
                )
            }

            val validator = registry.get(platform)
                ?: return BaselineSetupResult.Failure(
                    appContext.getString(R.string.baseline_no_validator, platform),
                )

            onStatus(platformStatusMessage(platform))
            when (val verification = validator.verifyUser(username)) {
                is PlatformUserVerification.Valid -> Unit
                is PlatformUserVerification.Invalid ->
                    return BaselineSetupResult.Failure(verification.message)
                is PlatformUserVerification.NotSupported -> Unit
                is PlatformUserVerification.Error ->
                    return BaselineSetupResult.Failure(
                        verification.message ?: verification.code,
                    )
            }
        }

        onStatus(appContext.getString(R.string.loading_setting_up_baseline))
        val now = System.currentTimeMillis()

        for (goal in activeGoals) {
            val platform = PlatformConstants.normalizePlatform(goal.platform)
            val username = usernames[platform]?.trim().orEmpty()
            if (username.isBlank()) continue

            val validator = registry.get(platform) ?: continue
            val window = windowPolicy.window(goal.deadline, now)
                ?: return BaselineSetupResult.Failure(
                    appContext.getString(R.string.baseline_invalid_window),
                )

            val (from, to) = window
            val baseline = validator.fetchBaselineCount(username, from, to)
                ?: return BaselineSetupResult.Failure(
                    appContext.getString(R.string.baseline_fetch_failed, platform),
                )

            val evidence = mapOf(
                "source" to "onboarding_baseline",
                "platform" to platform,
                "from" to from,
                "to" to to,
                "baselineValue" to baseline,
            )
            goalsRepo.setGoalBaseline(goal.id, baseline, username, evidence)
        }

        onStatus(appContext.getString(R.string.loading_almost_done))
        return BaselineSetupResult.Success
    }

    private fun platformStatusMessage(platform: String): String = when (platform) {
        PlatformConstants.KEY_LEETCODE -> appContext.getString(R.string.loading_checking_leetcode)
        PlatformConstants.KEY_DUOLINGO -> appContext.getString(R.string.loading_checking_duolingo)
        else -> appContext.getString(R.string.loading_checking_platform, platform)
    }
}
