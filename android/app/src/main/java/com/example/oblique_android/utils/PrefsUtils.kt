package com.example.oblique_android.utils

import android.content.Context
import android.util.Log
import com.example.oblique_android.network.ApiClient
import kotlinx.coroutines.runBlocking

/**
 * PrefsUtils — canonical helper for small app prefs used across the app.
 *
 * This file includes:
 *  - blocked apps storage
 *  - display name helpers
 *  - robust platform username caching + fetch (blocking + suspend variants)
 *
 * Implementation notes:
 *  - Network call obtains the current user as a Map<String, Any> so we don't depend on DTOs.
 *  - We look for keys: "platformUsernames", "usernames", or nested user -> platformUsernames.
 *  - Cached keys are saved in the same "user_prefs" SharedPreferences.
 */

interface UserMapApi {
    // The method name must match your server. Using "/api/user/me" style is typical.
    // Retrofit + Moshi will map the JSON to Map<String, Any>.
    @retrofit2.http.GET("/api/user/me")
    suspend fun getCurrentUser(): Map<String, Any>

    // Some servers expose preferences under a different endpoint. If you have
    // /api/user/me/preferences uncomment and use below instead of getCurrentUser().
    // @GET("/api/user/me/preferences")
    // suspend fun getPreferences(): Map<String, Any>
}


object PrefsUtils {

    private const val PREFS_BLOCKED = "blocked_apps"
    private const val PREFS_USER = "user_prefs"
    private const val KEY_PKGS = "pkgs"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_DEADLINE_BUFFER_MS = "deadline_buffer_ms"
    private const val KEY_VALIDATION_TIME_OFFSET_MS = "validation_time_offset_ms"
    private const val KEY_USERNAMES_PREFIX = "platform_username_" // stored as platform_username_leetcode

    const val MAX_DEADLINE_BUFFER_MS = 3 * 60 * 60 * 1000L

    // ---------- Blocked apps ----------
    fun saveBlockedSet(context: Context, pkgs: Set<String>) {
        context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PKGS, pkgs)
            .apply()
    }

    fun loadBlockedSet(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_BLOCKED, Context.MODE_PRIVATE)
            .getStringSet(KEY_PKGS, emptySet()) ?: emptySet()
    }

    // ---------- Display name ----------
    fun saveDisplayName(context: Context, name: String) {
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DISPLAY_NAME, name)
            .apply()
    }

    fun getDisplayName(context: Context): String? {
        return context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_NAME, null)
    }

    fun saveDeadlineBufferMs(context: Context, bufferMs: Long) {
        val clamped = bufferMs.coerceIn(0L, MAX_DEADLINE_BUFFER_MS)
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DEADLINE_BUFFER_MS, clamped)
            .apply()
    }

    fun getDeadlineBufferMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .getLong(KEY_DEADLINE_BUFFER_MS, 0L)
            .coerceIn(0L, MAX_DEADLINE_BUFFER_MS)
    }

    fun saveValidationTimeOffsetMs(context: Context, offsetMs: Long) {
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_VALIDATION_TIME_OFFSET_MS, offsetMs)
            .apply()
    }

    fun getValidationTimeOffsetMs(context: Context): Long {
        return context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .getLong(KEY_VALIDATION_TIME_OFFSET_MS, 0L)
    }

    fun clearUserPrefs(context: Context) {
        context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    // ---------- Platform username caching & fetching ----------
    /**
     * Blocking helper — safe to call from worker threads / synchronous contexts.
     * Tries cache first, then fetches from backend (/api/user/me or similar).
     *
     * Returns null if not found.
     */
    fun getPlatformUsername(context: Context, platform: String): String? {
        val prefs = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
        val key = "$KEY_USERNAMES_PREFIX$platform"
        prefs.getString(key, null)?.let { return it }

        // fallback — fetch from backend synchronously (runBlocking)
        return try {
            runBlocking {
                getPlatformUsernameSuspend(context, platform)
            }
        } catch (e: Exception) {
            Log.e("PrefsUtils", "getPlatformUsername runBlocking fetch failed: ${e.message}")
            null
        }
    }

    /**
     * Suspend variant — call from coroutines.
     *
     * Attempts to fetch the current user as a Map (to avoid DTO mismatches) then extracts the
     * platform-usernames map. Caches any discovered usernames.
     */
    suspend fun getPlatformUsernameSuspend(context: Context, platform: String): String? {
        val prefs = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
        val key = "$KEY_USERNAMES_PREFIX$platform"
        prefs.getString(key, null)?.let { return it }

        return try {
            // Create a Retrofit interface that returns a Map — avoids DTO coupling.
            // This local interface will be used only inside this function.

            val api = ApiClient.getClient(context).create(UserMapApi::class.java)
            val resp = try {
                api.getCurrentUser()
            } catch (e: Exception) {
                Log.w("PrefsUtils", "getCurrentUser failed: ${e.message}")
                emptyMap<String, Any>()
            }

            // Extract platformUsernames / usernames / or nested structures defensively
            val platformMap = extractPlatformUsernames(resp)

            // cache all usernames found
            if (platformMap.isNotEmpty()) {
                val editor = prefs.edit()
                for ((k, v) in platformMap) {
                    if (!v.isNullOrBlank()) {
                        editor.putString("$KEY_USERNAMES_PREFIX$k", v)
                    }
                }
                editor.apply()
            }

            val username = platformMap[platform]
            if (!username.isNullOrBlank()) {
                Log.i("PrefsUtils", "Fetched username for $platform: $username")
            } else {
                Log.d("PrefsUtils", "No username for $platform in backend response")
            }
            username
        } catch (e: Exception) {
            Log.e("PrefsUtils", "Failed to fetch platform username suspend: ${e.message}")
            null
        }
    }

    /**
     * Save a specific platform username locally (useful after user updates preferences in UI).
     */
    fun savePlatformUsername(context: Context, platform: String, username: String) {
        val prefs = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_USERNAMES_PREFIX$platform", username).apply()
    }

    /**
     * Clear all platform usernames cached in prefs
     */
    fun clearPlatformUsernames(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(KEY_USERNAMES_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }

    // ---------- helpers ----------
    /**
     * Defensive extractor for nested response shapes
     *
     * Looks for keys:
     *  - "platformUsernames": { leetcode: "bob", ... }
     *  - "usernames": { ... }
     *  - nested "user" -> "platformUsernames"
     */
    @Suppress("UNCHECKED_CAST")
    private fun extractPlatformUsernames(resp: Map<String, Any>): Map<String, String> {
        try {
            // direct top-level maps
            val tryKeys = listOf("platformUsernames", "platform_usernames", "usernames", "platformUserName", "platform_user_name")
            for (k in tryKeys) {
                val candidate = resp[k]
                if (candidate is Map<*, *>) {
                    return candidate.mapNotNull { entry ->
                        val kk = entry.key?.toString() ?: return@mapNotNull null
                        val vv = entry.value?.toString() ?: return@mapNotNull null
                        kk to vv
                    }.toMap()
                }
            }

            // nested user: { platformUsernames: { ... } }
            val userObj = resp["user"]
            if (userObj is Map<*, *>) {
                for (k in tryKeys) {
                    val candidate = userObj[k]
                    if (candidate is Map<*, *>) {
                        return candidate.mapNotNull { entry ->
                            val kk = entry.key?.toString() ?: return@mapNotNull null
                            val vv = entry.value?.toString() ?: return@mapNotNull null
                            kk to vv
                        }.toMap()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PrefsUtils", "extractPlatformUsernames error: ${e.message}")
        }
        return emptyMap()
    }
}
