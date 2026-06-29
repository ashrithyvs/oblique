package com.example.oblique_android.utils

import android.util.Log
import com.example.oblique_android.BuildConfig
import com.example.oblique_android.network.api.GoalDto

/**
 * Dev-only logging for goal validation (enabled when [BuildConfig.DEV_MODE]).
 * Uses [Log.i] so lines appear alongside other validation tags in logcat.
 */
object ValidationDevLogger {
    private const val TAG = "GoalValidationDev"

    fun logValidationStart(
        goalId: String,
        platform: String,
        windowStartMs: Long,
        windowEndMs: Long,
        storedProgress: Int,
        targetValue: Int,
        baselineValue: Int = 0,
    ) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(
            TAG,
            "validate start goalId=$goalId platform=$platform window=[$windowStartMs..$windowEndMs] " +
                "baseline=$baselineValue storedProgress=$storedProgress target=$targetValue",
        )
    }

    fun logPlatformResult(
        goalId: String,
        outcome: String,
        currentValue: Int,
        storedProgress: Int,
        evidence: Map<String, Any>? = null,
        baselineValue: Int = 0,
    ) {
        if (!BuildConfig.DEV_MODE) return
        val computedProgress = (currentValue - baselineValue).coerceAtLeast(0)
        val submissionsFetched = evidence?.get("submissionCount")
        Log.i(
            TAG,
            "platform $outcome goalId=$goalId solvedInWindow=$currentValue baseline=$baselineValue " +
                "computedProgress=$computedProgress storedProgress=$storedProgress " +
                "submissionsFetched=$submissionsFetched evidence=$evidence",
        )
    }

    fun logPlatformFailure(goalId: String, code: String, message: String?) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(TAG, "platform FAILURE goalId=$goalId code=$code message=$message")
    }

    fun logExternalApi(method: String, url: String, payload: String, responseSummary: String) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(TAG, "→ $method $url\npayload=$payload\n← $responseSummary")
    }

    fun logSyncSkipped(goalId: String, reason: String) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(TAG, "backend sync skipped goalId=$goalId reason=$reason")
    }

    fun logApiRequest(method: String, path: String, payload: Map<String, Any>) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(TAG, "→ $method $path\npayload=$payload")
    }

    fun logApiResponse(method: String, path: String, response: GoalDto) {
        if (!BuildConfig.DEV_MODE) return
        Log.i(
            TAG,
            "← $method $path\nresponse={id=${response.id}, status=${response.status}, " +
                "progress=${response.progress}, targetValue=${response.targetValue}, " +
                "platform=${response.platform}, completedAt=${response.completedAt}}",
        )
    }

    fun logApiError(method: String, path: String, payload: Map<String, Any>, error: Throwable) {
        if (!BuildConfig.DEV_MODE) return
        Log.e(TAG, "✗ $method $path\npayload=$payload\nerror=${error.message}", error)
    }
}
