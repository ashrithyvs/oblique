package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.services.Prefs

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Decide flow
        when {
            !Prefs.isOnboardingDone() -> {
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            !Prefs.isPinSet() -> {
                startActivity(Intent(this, PinSetupActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
        }
        finish()

    }
}
