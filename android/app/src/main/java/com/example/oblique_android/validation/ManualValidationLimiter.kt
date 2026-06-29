package com.example.oblique_android.validation

import android.content.Context
import androidx.core.content.edit
import com.example.oblique_android.BuildConfig
import com.example.oblique_android.utils.ValidationConstants

/**
 * Rate-limiter: manual validation allowed once per hour (skipped when [BuildConfig.DEV_MODE]).
 */
class ManualValidationLimiter(private val ctx: Context) {

    fun canTrigger(): Boolean {
        if (BuildConfig.DEV_MODE) return true
        val last = ctx.getSharedPreferences(ValidationConstants.MANUAL_VALIDATION_PREF, Context.MODE_PRIVATE)
            .getLong(ValidationConstants.KEY_LAST_VALIDATION_MS, 0L)
        return System.currentTimeMillis() - last >= ValidationConstants.MANUAL_COOLDOWN_MS
    }

    fun markTriggered() {
        if (BuildConfig.DEV_MODE) return
        ctx.getSharedPreferences(ValidationConstants.MANUAL_VALIDATION_PREF, Context.MODE_PRIVATE)
            .edit { putLong(ValidationConstants.KEY_LAST_VALIDATION_MS, System.currentTimeMillis()) }
    }
}
