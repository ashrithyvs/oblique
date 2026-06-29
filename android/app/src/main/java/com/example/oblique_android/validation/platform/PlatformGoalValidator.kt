package com.example.oblique_android.validation.platform

import com.example.oblique_android.models.Goal

data class PlatformValidationContext(
    val goal: Goal,
    val username: String?,
    val windowStartMs: Long,
    val windowEndMs: Long,
    val nowMs: Long = System.currentTimeMillis(),
)

sealed class PlatformValidationResult {
    data class Success(
        val currentValue: Int,
        val evidence: Map<String, Any>? = null,
    ) : PlatformValidationResult()

    data class Failure(
        val code: String,
        val message: String? = null,
    ) : PlatformValidationResult()

    data class NoChange(
        val currentValue: Int,
        val evidence: Map<String, Any>? = null,
    ) : PlatformValidationResult()
}

sealed class PlatformUserVerification {
    data object Valid : PlatformUserVerification()
    data class Invalid(val message: String) : PlatformUserVerification()
    data class NotSupported(val message: String) : PlatformUserVerification()
    data class Error(val code: String, val message: String? = null) : PlatformUserVerification()
}

interface PlatformGoalValidator {
    val platformKey: String
    suspend fun validate(context: PlatformValidationContext): PlatformValidationResult
    suspend fun verifyUser(username: String): PlatformUserVerification =
        PlatformUserVerification.NotSupported("User verification not implemented for $platformKey")

    suspend fun fetchBaselineCount(
        username: String,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Int? = null
}
