package com.example.oblique_android.services

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREFS_NAME = "oblique_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_PIN_SET = "pin_set"
    private const val KEY_PIN_JUST_CREATED = "pin_just_created"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_HAS_ALL_PERMISSIONS = "has_all_permissions"

    private lateinit var prefs: SharedPreferences

    /**
     * Must be called once (e.g. in Application.onCreate).
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    fun setOnboardingDone(done: Boolean) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()

    fun isPinSet(): Boolean = prefs.getBoolean(KEY_PIN_SET, false)
    fun setPinSet(set: Boolean) = prefs.edit().putBoolean(KEY_PIN_SET, set).apply()

    fun isPinJustCreated(): Boolean = prefs.getBoolean(KEY_PIN_JUST_CREATED, false)
    fun setPinJustCreated(value: Boolean) = prefs.edit().putBoolean(KEY_PIN_JUST_CREATED, value).apply()

    fun getSelectedApps(): Set<String> = prefs.getStringSet(KEY_SELECTED_APPS, emptySet()) ?: emptySet()
    fun setSelectedApps(apps: Set<String>) = prefs.edit().putStringSet(KEY_SELECTED_APPS, apps).apply()

    fun hasAllPermissions(): Boolean = prefs.getBoolean(KEY_HAS_ALL_PERMISSIONS, false)
    fun setHasAllPermissions(value: Boolean) = prefs.edit().putBoolean(KEY_HAS_ALL_PERMISSIONS, value).apply()
}
