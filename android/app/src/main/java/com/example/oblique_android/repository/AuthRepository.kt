package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.models.AuthOutcome
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.AuthApi
import com.example.oblique_android.network.api.LoginRequest
import com.example.oblique_android.network.api.RegisterRequest
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.utils.ApiErrorParser
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
     * Device unlock PIN is configured locally after auth via PinSetupActivity.
     */
    suspend fun register(name: String, email: String, password: String): AuthOutcome =
        withContext(Dispatchers.IO) {
            try {
                val req = RegisterRequest(
                    name = name,
                    email = email,
                    password = password
                )
                val response = api.register(req)

                if (!response.token.isNullOrEmpty()) {
                    tokenManager.saveToken(response.token)
                    safeWarmupAfterAuth()
                    return@withContext AuthOutcome(success = true)
                }
                AuthOutcome(success = false, errorMessage = "Registration failed")
            } catch (ex: Exception) {
                ex.printStackTrace()
                AuthOutcome(
                    success = false,
                    errorMessage = ApiErrorParser.messageFrom(ex, "Registration failed")
                )
            }
        }

    /**
     * Login existing user.
     */
    suspend fun login(email: String, password: String): AuthOutcome =
        withContext(Dispatchers.IO) {
            try {
                val req = LoginRequest(email = email, password = password)
                val response = api.login(req)
                if (!response.token.isNullOrEmpty()) {
                    tokenManager.saveToken(response.token)
                    safeWarmupAfterAuth()
                    return@withContext AuthOutcome(success = true)
                }
                AuthOutcome(success = false, errorMessage = "Login failed")
            } catch (ex: Exception) {
                ex.printStackTrace()
                AuthOutcome(
                    success = false,
                    errorMessage = ApiErrorParser.messageFrom(ex, "Login failed")
                )
            }
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
