package com.example.oblique_android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PINManager {
    private const val PREF_FILE = "secure_prefs"
    private const val KEY_PIN = "user_pin"

    private fun prefs(ctx: Context) =
        EncryptedSharedPreferences.create(
            ctx,
            PREF_FILE,
            MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun savePin(ctx: Context, pin: String) {
        prefs(ctx).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(ctx: Context): String? = prefs(ctx).getString(KEY_PIN, null)

    fun isPinSet(ctx: Context): Boolean = getPin(ctx) != null

    fun validatePin(ctx: Context, pin: String): Boolean = getPin(ctx) == pin
}
