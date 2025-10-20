package com.example.oblique_android.prefs

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.AEADBadTagException

class TokenManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "secure_prefs"
        private const val MASTER_KEY_ALIAS = "master_key_alias"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs by lazy {
        initPrefs()
    }

    private fun initPrefs(): android.content.SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

        } catch (e: AEADBadTagException) {
            Log.e("TokenManager", "Corrupted secure prefs — resetting.", e)
            safeResetPrefs()
        } catch (e: Exception) {
            Log.e("TokenManager", "Secure prefs init failed, retrying cleanly", e)
            safeResetPrefs()
        }
    }

    private fun safeResetPrefs(): android.content.SharedPreferences {
        try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(MASTER_KEY_ALIAS)) {
                ks.deleteEntry(MASTER_KEY_ALIAS)
                Log.w("TokenManager", "Deleted invalidated KeyStore entry: $MASTER_KEY_ALIAS")
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "Failed to reset keystore cleanly", e)
        }

        context.deleteSharedPreferences(PREFS_FILE)

        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
