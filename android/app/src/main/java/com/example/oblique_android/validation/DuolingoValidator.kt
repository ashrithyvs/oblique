package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Safe stub for Duolingo validation. Returns zeroed progress until integrated later.
 */
class DuolingoValidator(private val ctx: Context) : PlatformValidator {
    companion object { private const val TAG = "DuolingoValidator" }

    override suspend fun validate(
        username: String?,
        windowStart: Date,
        windowEnd: Date,
        targetCount: Int?
    ): ValidationResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "DuolingoValidator called safely (stub)")
        ValidationResult(0, emptyMap())
    }
}
