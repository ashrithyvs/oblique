package com.example.oblique_android

import android.content.Context

object BlockedAppsStore {
    private const val PREFS = "blocked_apps"
    private const val KEY_APPS = "apps"

    fun saveBlockedApps(context: Context, apps: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_APPS, apps)
            .apply()
    }

    fun getBlockedApps(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_APPS, emptySet()) ?: emptySet()
    }
}
