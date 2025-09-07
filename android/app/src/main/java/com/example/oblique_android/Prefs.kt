// app/src/main/java/com/example/oblique_android/Prefs.kt
package com.example.oblique_android

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREF_NAME = "oblique_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_PIN_SET = "pin_set"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    // ---- Generic helpers ----
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defValue)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defValue: String? = null): String? {
        return prefs.getString(key, defValue)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // ---- Explicit helpers for onboarding & PIN ----
    fun isOnboardingDone(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun isPinSet(): Boolean =
        prefs.getBoolean(KEY_PIN_SET, false)

    fun setPinSet(set: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_SET, set).apply()
    }
}
