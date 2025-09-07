package com.example.oblique_android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object PINManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_PIN = "user_pin"

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

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
