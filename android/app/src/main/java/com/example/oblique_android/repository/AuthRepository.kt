package com.example.oblique_android.repository

import android.content.Context
import com.example.oblique_android.dao.BlockedAppDao
import com.example.oblique_android.dao.GoalDao
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.AuthApi
import com.example.oblique_android.network.api.LoginRequest
import com.example.oblique_android.network.api.RegisterRequest
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.PINManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val context: Context,
    private val goalDao: GoalDao,
    private val blockedDao: BlockedAppDao
) {
    private val api: AuthApi by lazy {
        ApiClient.getClient(context).create(AuthApi::class.java)
    }

    private val tokenManager = TokenManager(context)
    private val goalsRepo = GoalsRepository(context, goalDao)
    private val blockedRepo = BlockedAppsRepository(context, blockedDao)

    /**
     * Register a new user with backend.
     * PIN is pulled automatically from PINManager if available.
     */
    suspend fun register(name: String, email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val pin = PINManager.getPin(context)
                val req = RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    pin = pin // may be null if PIN not set
                )
                val response = api.register(req)
                if (response.token.isNotEmpty()) {
                    tokenManager.saveToken(response.token)
                    // initial sync
                    goalsRepo.refreshFromServer()
                    blockedRepo.refreshFromServer()
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
                if (response.token.isNotEmpty()) {
                    tokenManager.saveToken(response.token)
                    // sync after login
                    goalsRepo.refreshFromServer()
                    blockedRepo.refreshFromServer()
                    return@withContext true
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            false
        }

   suspend fun logout() {
        tokenManager.clear()
        goalDao.clearAll()
        blockedDao.clearAll()
    }

    fun isLoggedIn(): Boolean = tokenManager.getToken() != null
}
