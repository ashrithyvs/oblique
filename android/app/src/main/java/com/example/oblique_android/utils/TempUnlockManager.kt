package com.example.oblique_android.utils

import android.content.Context

object TempUnlockManager {
    private const val PREFS_NAME = "temp_unlocks"

    fun setTempUnlock(context: Context, pkg: String, durationMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiry = System.currentTimeMillis() + durationMillis
        prefs.edit().putLong(pkg, expiry).apply()
    }

    fun isTempUnlocked(context: Context, pkg: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(pkg, 0)
        return System.currentTimeMillis() < expiry
    }

    fun clearTempUnlock(context: Context, pkg: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(pkg).apply()
    }
}
