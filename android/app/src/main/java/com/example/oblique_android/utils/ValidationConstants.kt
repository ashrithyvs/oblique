package com.example.oblique_android.utils

object ValidationConstants {
    const val WORK_TAG = "goal_validation"
    const val EXTRA_GOAL_ID = "goal_id"

    const val MANUAL_VALIDATION_PREF = "manual_validation_limit"
    const val KEY_LAST_VALIDATION_MS = "last_validation_ms"
    const val MANUAL_COOLDOWN_MS = 60 * 60 * 1000L

    const val ERROR_MISSING_USERNAME = "MISSING_USERNAME"
    const val ERROR_INVALID_TIME_RANGE = "INVALID_TIME_RANGE"
    const val ERROR_NO_INTERNET = "NO_INTERNET"
    const val ERROR_FETCH_SUBMISSION_FAILED = "FETCH_SUBMISSION_FAILED"
    const val ERROR_EMPTY_SUBMISSION_LIST = "EMPTY_SUBMISSION_LIST"
    const val ERROR_UNSUPPORTED = "UNSUPPORTED"
    const val ERROR_INVALID_WINDOW = "INVALID_WINDOW"
    const val ERROR_NO_VALIDATOR = "NO_VALIDATOR"
}
