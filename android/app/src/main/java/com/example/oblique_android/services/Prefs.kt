package com.example.oblique_android.services

import android.content.Context
import android.content.SharedPreferences
import com.example.oblique_android.utils.PrefsUtils

object Prefs {
    private const val PREFS_NAME = "oblique_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_PIN_SET = "pin_set"
    private const val KEY_HAS_ALL_PERMISSIONS = "has_all_permissions"
    private const val KEY_MIGRATED_BLOCKED_APPS = "migrated_blocked_apps_v1"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    /**
     * Must be called once (e.g. in Application.onCreate).
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateBlockedAppsIfNeeded()
    }

    private fun migrateBlockedAppsIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED_BLOCKED_APPS, false)) return
        val legacy = prefs.getStringSet("selected_apps", null)
        if (!legacy.isNullOrEmpty()) {
            val current = PrefsUtils.loadBlockedSet(appContext).toMutableSet()
            current.addAll(legacy)
            PrefsUtils.saveBlockedSet(appContext, current)
        }
        prefs.edit()
            .remove("selected_apps")
            .putBoolean(KEY_MIGRATED_BLOCKED_APPS, true)
            .apply()
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun setOnboardingDone(done: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()

    fun isPinSet(): Boolean = prefs.getBoolean(KEY_PIN_SET, false)
    fun setPinSet(set: Boolean) = prefs.edit().putBoolean(KEY_PIN_SET, set).apply()

    /** Blocked apps — delegated to [PrefsUtils] (single source of truth). */
    fun getSelectedApps(): Set<String> = PrefsUtils.loadBlockedSet(appContext)

    fun setSelectedApps(apps: Set<String>) = PrefsUtils.saveBlockedSet(appContext, apps)

    fun hasAllPermissions(): Boolean = prefs.getBoolean(KEY_HAS_ALL_PERMISSIONS, false)
    fun setHasAllPermissions(value: Boolean) = prefs.edit().putBoolean(KEY_HAS_ALL_PERMISSIONS, value).apply()
}
