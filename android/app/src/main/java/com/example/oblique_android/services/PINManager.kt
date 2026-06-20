package com.example.oblique_android.services

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PINManager {
    private const val PREFS_NAME = "pin_secure_prefs"
    private const val LEGACY_PREFS_NAME = "secure_prefs"
    private const val KEY_PIN = "user_pin"
    private const val MASTER_KEY_ALIAS = "pin_master_key_alias"

    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context, PREFS_NAME, MASTER_KEY_ALIAS).also {
                migrateFromLegacy(context, it)
            }
        } catch (e: Exception) {
            context.deleteSharedPreferences(PREFS_NAME)
            createEncryptedPrefs(context, PREFS_NAME, MASTER_KEY_ALIAS)
        }
    }

    private fun createEncryptedPrefs(
        context: Context,
        fileName: String,
        keyAlias: String
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context, keyAlias)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migrateFromLegacy(context: Context, current: SharedPreferences) {
        if (current.contains(KEY_PIN)) return
        try {
            val legacy = createEncryptedPrefs(context, LEGACY_PREFS_NAME, "master_key_alias")
            val pin = legacy.getString(KEY_PIN, null) ?: return
            current.edit().putString(KEY_PIN, pin).apply()
        } catch (_: Exception) {
            try {
                val legacyPlain = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
                val pin = legacyPlain.getString(KEY_PIN, null) ?: return
                current.edit().putString(KEY_PIN, pin).apply()
            } catch (_: Exception) { }
        }
    }

    fun savePin(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(context: Context): String? {
        return getPrefs(context).getString(KEY_PIN, null)
    }

    fun isPinSet(context: Context): Boolean {
        return !getPin(context).isNullOrEmpty()
    }

    fun verifyPin(context: Context, input: String): Boolean {
        return getPin(context) == input
    }

    fun clearPin(context: Context) {
        getPrefs(context).edit().remove(KEY_PIN).apply()
    }
}
