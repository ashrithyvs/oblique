package com.example.oblique_android.prefs

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.AEADBadTagException

class TokenManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "auth_secure_prefs"
        private const val LEGACY_PREFS_FILE = "secure_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val MASTER_KEY_ALIAS = "auth_master_key_alias"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs by lazy { initPrefs() }

    private fun initPrefs(): android.content.SharedPreferences {
        return try {
            createEncryptedPrefs(PREFS_FILE, MASTER_KEY_ALIAS).also { migrateFromLegacy(it) }
        } catch (e: AEADBadTagException) {
            Log.e("TokenManager", "Corrupted auth prefs — resetting.", e)
            safeResetPrefs()
        } catch (e: Exception) {
            Log.e("TokenManager", "Auth prefs init failed, retrying cleanly", e)
            safeResetPrefs()
        }
    }

    private fun migrateFromLegacy(current: android.content.SharedPreferences) {
        if (current.contains(KEY_TOKEN)) return
        try {
            val legacy = createEncryptedPrefs(LEGACY_PREFS_FILE, "master_key_alias")
            val token = legacy.getString(KEY_TOKEN, null) ?: return
            current.edit().putString(KEY_TOKEN, token).apply()
        } catch (_: Exception) { }
    }

    private fun createEncryptedPrefs(fileName: String, keyAlias: String): android.content.SharedPreferences {
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

    private fun safeResetPrefs(): android.content.SharedPreferences {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(MASTER_KEY_ALIAS)) {
                ks.deleteEntry(MASTER_KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Failed to reset keystore cleanly", e)
        }
        context.deleteSharedPreferences(PREFS_FILE)
        return createEncryptedPrefs(PREFS_FILE, MASTER_KEY_ALIAS)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}
