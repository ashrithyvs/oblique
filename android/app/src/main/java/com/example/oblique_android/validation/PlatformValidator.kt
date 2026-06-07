package com.example.oblique_android.validation

import android.content.Context
import java.util.*

/**
 * Base abstraction for any validation source (e.g., LeetCode, Duolingo).
 * Validation means verifying current goal progress by querying an external platform.
 */
data class ValidationResult(
    val totalSolved: Int,
    val byDifficulty: Map<String, Int>,
    val earliest: Date? = null,
    val latest: Date? = null,
    val rawEvidence: Map<String, Any>? = null
)

interface PlatformValidator {
    suspend fun validate(
        username: String?,
        windowStart: Date,
        windowEnd: Date,
        targetCount: Int?
    ): ValidationResult
}

/** Factory returning platform-specific validator implementation */
object PlatformValidatorFactory {
    fun getValidatorFor(platform: String, ctx: Context): PlatformValidator {
        return when (platform.lowercase(Locale.getDefault())) {
//            "leetcode" -> LeetCodeValidator(ctx)
            "duolingo" -> DuolingoValidator(ctx)
            else -> NoOpValidator()
        }
    }
}


class NoOpValidator : PlatformValidator {
    override suspend fun validate(
        username: String?,
        windowStart: Date,
        windowEnd: Date,
        targetCount: Int?
    ): ValidationResult = ValidationResult(0, emptyMap())
}
