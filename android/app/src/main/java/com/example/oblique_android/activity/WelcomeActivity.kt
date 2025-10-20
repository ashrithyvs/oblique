package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.Prefs

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        Prefs.init(this)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            Prefs.setOnboardingDone(true)

            // ✅ First ensure user has granted permissions
            if (!Prefs.hasAllPermissions()) {
                startActivity(Intent(this, PermissionsActivity::class.java))
                finish()
                return@setOnClickListener
            }

            val token = TokenManager.getInstance(this).getToken()
            val pinPrefs = getSharedPreferences("secure_prefs", MODE_PRIVATE)
            val pinExists = pinPrefs.contains("user_pin")

            when {
                !pinExists -> startActivity(Intent(this, PinSetupActivity::class.java))
                token.isNullOrEmpty() -> startActivity(Intent(this, LoginActivity::class.java))
                else -> startActivity(Intent(this, DashboardActivity::class.java))
            }
            finish()
        }
    }
}
