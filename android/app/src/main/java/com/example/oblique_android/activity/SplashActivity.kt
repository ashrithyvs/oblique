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

class SplashActivity : AppCompatActivity() {

    private val tokenManager by lazy { TokenManager(this) }
    private val userApi by lazy { ApiClient.getClient(this).create(UserApi::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            decideNextScreen()
            finish()
        }
    }

    private suspend fun decideNextScreen() {
        // 1️⃣ Permissions
        if (!Prefs.hasAllPermissions()) {
            goTo(PermissionsActivity::class.java); return
        }

        // 2️⃣ Authentication
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            goTo(LoginActivity::class.java); return
        }

        val user = try {
            userApi.getCurrentUser()
        } catch (e: Exception) {
            tokenManager.clear()
            goTo(LoginActivity::class.java)
            return
        }
        Log.d("asd","User:%d"+user)
        // 3️⃣ PIN (check ONLY after login)
        if (user.hasPin != true) {
            goTo(PinSetupActivity::class.java); return
        }
        if (Prefs.isPinJustCreated()) {
            goTo(PinConfirmActivity::class.java); return
        }

        // 4️⃣ Blocked apps
        if (user.blockedApps.isNullOrEmpty()) {
            goTo(AppListActivity::class.java); return
        }

        // 5️⃣ Goals
        if (user.goals.isNullOrEmpty()) {
            goTo(GoalsActivity::class.java); return
        }

        // 6️⃣ Otherwise → Dashboard
        goTo(DashboardActivity::class.java)
    }


    private fun goTo(cls: Class<*>) {
        startActivity(Intent(this, cls))
    }
}
