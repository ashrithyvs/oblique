package com.example.oblique_android.utils

import android.content.Context

object PrefsUtils {
    private const val PREFS_BLOCKED = "blocked_apps"
    private const val KEY_PKGS = "pkgs"

    fun saveBlockedSet(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_PKGS, pkgs).apply()
    }

    fun loadBlockedSet(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .getStringSet(KEY_PKGS, emptySet()) ?: emptySet()
    }
}
