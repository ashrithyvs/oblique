package com.example.oblique_android.validation

import android.content.Context
import androidx.core.content.edit

/**
 * Simple rate-limiter: manual validation allowed once per hour.
 */
class ManualValidationLimiter(private val ctx: Context) {
    companion object {
        private const val PREF = "manual_validation_limit"
        private const val KEY_LAST = "last_validation_ms"
        private const val COOLDOWN_MS = 60 * 60 * 1000L
    }

    fun canTrigger(): Boolean {
        val last = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_LAST, 0L)
        return System.currentTimeMillis() - last >= COOLDOWN_MS
    }

    fun markTriggered() {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit { putLong(KEY_LAST, System.currentTimeMillis()) }
    }
}
