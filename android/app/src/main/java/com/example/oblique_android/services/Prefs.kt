package com.example.oblique_android.services

import android.content.Context
import android.content.SharedPreferences
import com.example.oblique_android.utils.PrefsUtils

object Prefs {
    private const val PREFS_NAME = "oblique_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_APP_SELECTION_DONE = "app_selection_done"
    private const val KEY_PLATFORM_SETUP_DONE = "platform_setup_done"
    private const val KEY_MIGRATED_BLOCKED_APPS = "migrated_blocked_apps_v1"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    /**
     * Must be called once (e.g. in Application.onCreate).
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        migrateBlockedAppsIfNeeded()
        migrateAppSelectionIfNeeded()
        migratePlatformSetupIfNeeded()
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

    fun isAppSelectionDone(): Boolean = prefs.getBoolean(KEY_APP_SELECTION_DONE, false)
    fun setAppSelectionDone(done: Boolean) = prefs.edit().putBoolean(KEY_APP_SELECTION_DONE, done).apply()

    fun isPlatformSetupDone(): Boolean = prefs.getBoolean(KEY_PLATFORM_SETUP_DONE, false)
    fun setPlatformSetupDone(done: Boolean) = prefs.edit().putBoolean(KEY_PLATFORM_SETUP_DONE, done).apply()

    private fun migrateAppSelectionIfNeeded() {
        if (prefs.contains(KEY_APP_SELECTION_DONE)) return
        // Users who already finished PIN setup completed app selection in a prior version.
        if (PINManager.isPinSet(appContext)) {
            prefs.edit().putBoolean(KEY_APP_SELECTION_DONE, true).apply()
        }
    }

    private fun migratePlatformSetupIfNeeded() {
        if (prefs.contains(KEY_PLATFORM_SETUP_DONE)) return
        if (PINManager.isPinSet(appContext) && isAppSelectionDone()) {
            prefs.edit().putBoolean(KEY_PLATFORM_SETUP_DONE, true).apply()
        }
    }

    /** Blocked apps — delegated to [PrefsUtils] (single source of truth). */
    fun getSelectedApps(): Set<String> = PrefsUtils.loadBlockedSet(appContext)

    fun setSelectedApps(apps: Set<String>) = PrefsUtils.saveBlockedSet(appContext, apps)

    fun isProtectionEnabled(): Boolean = prefs.getBoolean(KEY_PROTECTION_ENABLED, false)

    fun setProtectionEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
}
