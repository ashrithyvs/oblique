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
    ) : PlatformValidationResult()
}

interface PlatformGoalValidator {
    val platformKey: String
    suspend fun validate(context: PlatformValidationContext): PlatformValidationResult
}
