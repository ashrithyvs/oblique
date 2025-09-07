package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<MaterialButton>(R.id.btnGetStarted).setOnClickListener {
            Prefs.setOnboardingDone(true)
            if (!PermissionUtils.hasUsageAccess(this) || !PermissionUtils.hasOverlayPermission(this)) {
                startActivity(Intent(this, PermissionsActivity::class.java))
            } else {
                startActivity(Intent(this, if (PINManager.isPinSet(this)) DashboardActivity::class.java else PinSetupActivity::class.java))
            }
            finish()
        }
    }
}
