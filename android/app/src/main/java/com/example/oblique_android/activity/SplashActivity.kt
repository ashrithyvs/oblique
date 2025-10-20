package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.network.ApiClient
import com.example.oblique_android.network.api.UserApi
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.Prefs
import kotlinx.coroutines.launch
import javax.crypto.AEADBadTagException

class SplashActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var userApi: UserApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize prefs if not yet done (in case Application didn’t)
        Prefs.init(this)

        lifecycleScope.launch {
            try {
                // ✅ Check if onboarding (welcome) is done
                if (!Prefs.isOnboardingDone()) {
                    goTo(WelcomeActivity::class.java)
                    return@launch
                }

                // ✅ Initialize secure token and API client
                tokenManager = TokenManager.getInstance(this@SplashActivity)
                userApi = ApiClient.getClient(this@SplashActivity).create(UserApi::class.java)

                decideNextScreen()
            } catch (e: AEADBadTagException) {
                Log.e("SplashActivity", "Corrupted encrypted prefs, resetting...", e)
                handleCorruptedPrefs()
            } catch (e: Exception) {
                Log.e("SplashActivity", "Unexpected init error", e)
                handleCorruptedPrefs()
            } finally {
                finish()
            }
        }
    }

    private suspend fun decideNextScreen() {
        // 1️⃣ Permissions
        if (!Prefs.hasAllPermissions()) {
            goTo(PermissionsActivity::class.java)
            return
        }

        // 2️⃣ Authentication
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            goTo(LoginActivity::class.java)
            return
        }

        val user = try {
            userApi.getCurrentUser()
        } catch (e: Exception) {
            if (e is retrofit2.HttpException && e.code() == 401) {
                tokenManager.clear()
                goTo(LoginActivity::class.java)
            } else {
                Log.e("SplashActivity", "Fetch user failed", e)
                goTo(DashboardActivity::class.java)
            }
            return
        }

        // 3️⃣ PIN logic
        if (!Prefs.isPinSet()) {
            goTo(PinSetupActivity::class.java)
            return
        }
        if (Prefs.isPinJustCreated()) {
            goTo(PinConfirmActivity::class.java)
            return
        }

        // 4️⃣ Blocked apps
        if (Prefs.getSelectedApps().isEmpty()) {
            goTo(AppListActivity::class.java)
            return
        }

        // ✅ All set
        goTo(DashboardActivity::class.java)
    }

    private fun handleCorruptedPrefs() {
        try {
            applicationContext.deleteSharedPreferences("secure_prefs")
        } catch (_: Exception) { }
        goTo(LoginActivity::class.java)
    }

    private fun goTo(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }
}
