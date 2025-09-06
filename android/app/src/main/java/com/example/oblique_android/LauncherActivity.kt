package com.example.oblique_android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs: SharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        val pinSet = prefs.getBoolean("pin_set", false)
        val appsChosen = prefs.getBoolean("apps_chosen", false)
        val permissionsGranted = PermissionUtils.hasRequiredPermissions(this)

        val nextIntent = when {
            !pinSet || !appsChosen -> Intent(this, WelcomeActivity::class.java)
            !permissionsGranted -> Intent(this, PermissionsActivity::class.java)
            else -> Intent(this, DashboardActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}
