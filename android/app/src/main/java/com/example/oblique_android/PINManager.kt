package com.example.oblique_android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PINManager {
    private const val PREF_FILE = "secure_prefs"
    private const val KEY_PIN = "user_pin"

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun setPIN(context: Context, pin: String) {
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPIN(context: Context): String? = getPrefs(context).getString(KEY_PIN, null)
    fun isPINSet(context: Context): Boolean = getPIN(context) != null
}
