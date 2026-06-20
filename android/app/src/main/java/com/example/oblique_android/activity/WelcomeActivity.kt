package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.setupWindowInsets

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        setupWindowInsets(R.id.rootWelcome)

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            OnboardingRouter.markOnboardingDone(this)
            startActivity(Intent(this, PermissionsActivity::class.java))
            finish()
        }
    }
}
