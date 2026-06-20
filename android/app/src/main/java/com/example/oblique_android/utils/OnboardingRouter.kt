package com.example.oblique_android.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.oblique_android.activity.AppListActivity
import com.example.oblique_android.activity.DashboardActivity
import com.example.oblique_android.activity.LoginActivity
import com.example.oblique_android.activity.PermissionsActivity
import com.example.oblique_android.activity.PinSetupActivity
import com.example.oblique_android.activity.WelcomeActivity
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.PrefsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Single source of truth for onboarding / post-auth navigation.
 */
object OnboardingRouter {

    fun markOnboardingDone(context: Context) {
        Prefs.init(context)
        Prefs.setOnboardingDone(true)
    }

    /**
     * @param validateToken When true (cold start), validates token via GET /api/user/me.
     */
    suspend fun nextSuspend(context: Context, validateToken: Boolean = true): Class<*> {
        Prefs.init(context)

        if (!Prefs.isOnboardingDone()) {
            return WelcomeActivity::class.java
        }
        if (!Prefs.hasAllPermissions()) {
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

        if (needsPinSetup(context)) {
            return PinSetupActivity::class.java
        }
        if (Prefs.getSelectedApps().isEmpty()) {
            return AppListActivity::class.java
        }
        return DashboardActivity::class.java
    }

    fun next(context: Context, validateToken: Boolean = false): Class<*> {
        Prefs.init(context)
        if (!Prefs.isOnboardingDone()) return WelcomeActivity::class.java
        if (!Prefs.hasAllPermissions()) return PermissionsActivity::class.java
        if (TokenManager.getInstance(context).getToken().isNullOrEmpty()) return LoginActivity::class.java
        if (needsPinSetup(context)) return PinSetupActivity::class.java
        if (Prefs.getSelectedApps().isEmpty()) return AppListActivity::class.java
        return DashboardActivity::class.java
    }

    fun start(context: Context, cls: Class<*>) {
        context.startActivity(Intent(context, cls))
    }

    /**
     * Device unlock PIN lives only in [PINManager]. [Prefs.isPinSet] is a legacy flag kept in sync.
     */
    fun needsPinSetup(context: Context): Boolean {
        Prefs.init(context)
        val stored = PINManager.isPinSet(context)
        if (Prefs.isPinSet() && !stored) {
            Prefs.setPinSet(false)
        }
        return !stored
    }
}
