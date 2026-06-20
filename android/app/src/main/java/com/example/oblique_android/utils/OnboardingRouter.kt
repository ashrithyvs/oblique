package com.example.oblique_android.utils

import android.content.Context
import android.util.Log
import com.example.oblique_android.activity.AppListActivity
import com.example.oblique_android.activity.DashboardActivity
import com.example.oblique_android.activity.GoalsActivity
import com.example.oblique_android.activity.LoginActivity
import com.example.oblique_android.activity.PermissionsActivity
import com.example.oblique_android.activity.PinSetupActivity
import com.example.oblique_android.activity.WelcomeActivity
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.PrefsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Single source of truth for onboarding / post-auth navigation.
 *
 * Flow:
 * 1. Fresh install: Welcome -> Permissions -> Auth -> Goals -> App list -> PIN -> Dashboard
 * 2. Returning user: Splash resolves the first incomplete step
 */
object OnboardingRouter {

    fun markOnboardingDone(context: Context) {
        Prefs.init(context)
        Prefs.setOnboardingDone(true)
    }

    suspend fun nextSuspend(context: Context, validateToken: Boolean = true): Class<*> {
        Prefs.init(context)

        if (!Prefs.isOnboardingDone()) {
            return WelcomeActivity::class.java
        }
        if (!PermissionUtils.hasRequiredPermissions(context)) {
            return PermissionsActivity::class.java
        }

        val tokenManager = TokenManager.getInstance(context)
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            return LoginActivity::class.java
        }

        if (validateToken) {
            try {
                val userApi = ApiClient.getClient(context).create(UserApi::class.java)
                withContext(Dispatchers.IO) {
                    val user = userApi.getCurrentUser()
                    user.platformUsernames?.forEach { (platform, uname) ->
                        if (uname.isNotBlank()) {
                            PrefsUtils.savePlatformUsername(context, platform, uname)
                        }
                    }
                    user.displayName?.let { PrefsUtils.saveDisplayName(context, it) }
                }
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 401) {
                    tokenManager.clear()
                    return LoginActivity::class.java
                }
                Log.e("OnboardingRouter", "Token validation failed, continuing offline", e)
            }
        }

        return resolvePostAuth(context)
    }

    fun afterGoals(context: Context): Class<*> {
        Prefs.init(context)
        return AppListActivity::class.java
    }

    fun afterApps(context: Context): Class<*> {
        Prefs.init(context)
        return if (needsPinSetup(context)) PinSetupActivity::class.java
        else DashboardActivity::class.java
    }

    fun afterPin(context: Context): Class<*> = DashboardActivity::class.java

    private suspend fun resolvePostAuth(context: Context): Class<*> {
        return try {
            val goals = withContext(Dispatchers.IO) {
                GoalsRepository(context).listGoals()
            }
            when {
                goals.isEmpty() -> GoalsActivity::class.java
                !Prefs.isAppSelectionDone() -> AppListActivity::class.java
                needsPinSetup(context) -> PinSetupActivity::class.java
                else -> DashboardActivity::class.java
            }
        } catch (e: Exception) {
            Log.e("OnboardingRouter", "Could not load goals, using local state", e)
            resolvePostAuthOffline(context)
        }
    }

    private fun resolvePostAuthOffline(context: Context): Class<*> {
        return when {
            !Prefs.isAppSelectionDone() -> AppListActivity::class.java
            needsPinSetup(context) -> PinSetupActivity::class.java
            else -> DashboardActivity::class.java
        }
    }

    fun needsPinSetup(context: Context): Boolean = !PINManager.isPinSet(context)
}
