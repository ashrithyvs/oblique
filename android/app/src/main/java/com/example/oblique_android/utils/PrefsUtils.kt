package com.example.oblique_android.utils

import android.content.Context

object PrefsUtils {
    private const val PREFS_BLOCKED = "blocked_apps"
    private const val PREFS_USER = "user_prefs"
    private const val KEY_PKGS = "pkgs"
    private const val KEY_DISPLAY_NAME = "display_name"

    // 🔒 Blocked apps
    fun saveBlockedSet(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PKGS, pkgs)
            .apply()
    }

    fun loadBlockedSet(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .getStringSet(KEY_PKGS, emptySet()) ?: emptySet()
    }

    // 🧑‍💼 Display name (new)
    fun saveDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_NAME, name)
            .apply()
    }

    fun getDisplayName(context: Context): String? {
        return context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_NAME, null)
    }

    fun clearUserPrefs(context: Context) {
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
