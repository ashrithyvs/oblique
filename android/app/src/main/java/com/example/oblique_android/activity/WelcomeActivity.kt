package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.prefs.TokenManager

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            val token = TokenManager(this).getToken()
            val pinPrefs = getSharedPreferences("regretnt_pin_prefs", MODE_PRIVATE)
            val pinExists = pinPrefs.contains("user_pin")

            when {
                !pinExists -> {
                    // First-time user → PIN setup flow
                    startActivity(Intent(this, PinSetupActivity::class.java))
                }
                token.isNullOrEmpty() -> {
                    // PIN exists but no login token → go to login/register
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                else -> {
                    // Token exists → try dashboard
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
            }
            finish()
        }
    }
}
