package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.AuthApi
import com.example.oblique_android.network.api.LoginRequest
import com.example.oblique_android.network.api.RegisterRequest
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.PINManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AuthRepository (API-first).
 *
 * Responsibilities:
 * - register / login via backend AuthApi
 * - save token via TokenManager
 * - after successful auth, warm/sync app caches by calling GoalsRepository & BlockedAppsRepository
 * - logout clears token and attempts to clear any in-memory caches.
 *
 * Note: No Room DAOs are required here anymore.
 */
class AuthRepository(
    private val context: Context
) {
    private val api: AuthApi by lazy {
        ApiClient.getClient(context).create(AuthApi::class.java)
    }

    private val tokenManager = TokenManager.getInstance(context)

    // API-only repositories (should exist in your project)
    private val goalsRepo = GoalsRepository(context)
    private val blockedRepo = BlockedAppsRepository(context)

    /**
     * Register a new user with backend.
     * PIN is pulled automatically from PINManager if available.
     */
    suspend fun register(name: String, email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val pin = try { PINManager.getPin(context) } catch (_: Exception) { null }
                val req = RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    pin = pin
                )
                val response = api.register(req)

                if (!response.token.isNullOrEmpty()) {
                    tokenManager.saveToken(response.token)
                    // try to warm local caches by calling repos (best-effort)
                    safeWarmupAfterAuth()
                    return@withContext true
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            false
        }

    /**
     * Login existing user.
     */
    suspend fun login(email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val req = LoginRequest(email = email, password = password)
                val response = api.login(req)
                if (!response.token.isNullOrEmpty()) {
                    tokenManager.saveToken(response.token)
                    safeWarmupAfterAuth()
                    return@withContext true
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            false
        }

    /**
     * Logout user: clear token and try to clear in-memory caches.
     * Since Room/local DB is removed, we don't try to access non-existent DAOs.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        tokenManager.clear()

        // Best-effort: if repositories expose cache-clear methods, call them.
        // Use try/catch so missing method won't crash app.
        try {
            // try clearCache() if implemented
            val clearCacheMethod = goalsRepo::class.members.firstOrNull { it.name == "clearCache" }
            clearCacheMethod?.call(goalsRepo)
        } catch (_: Exception) { /* ignore */ }

        try {
            val clearCacheMethod = blockedRepo::class.members.firstOrNull { it.name == "clearCache" }
            clearCacheMethod?.call(blockedRepo)
        } catch (_: Exception) { /* ignore */ }
    }

    fun isLoggedIn(): Boolean = tokenManager.getToken() != null

    /**
     * Attempt to warm/sync application caches after login/registration.
     * This is best-effort: any exception is swallowed so auth still succeeds.
     */
    private suspend fun safeWarmupAfterAuth() {
        // We call these synchronously on background thread (caller is already on IO context).
        try {
            // listGoals() may throw; swallow exceptions
            try {
                val goals = goalsRepo.listGoals()
                // some repositories might expose a method to set in-memory cache; ignore here
            } catch (_: Exception) { /* ignore */ }

            try {
                val blocked = blockedRepo.listBlockedApps()
            } catch (_: Exception) { /* ignore */ }
        } catch (_: Exception) { /* ignore */ }
    }
}
