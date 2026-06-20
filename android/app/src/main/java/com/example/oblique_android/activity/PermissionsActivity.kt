package com.example.oblique_android.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.utils.PermissionUtils
import com.example.oblique_android.utils.setupWindowInsets
import com.example.oblique_android.R

class PermissionsActivity : AppCompatActivity() {

    private val usageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasUsageAccess(this)) {
            checkAndProceed()
        } else {
            Toast.makeText(this, "Usage access permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasOverlayPermission(this)) {
            checkAndProceed()
        } else {
            Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkAndProceed()
        } else {
            Toast.makeText(this, "Notification permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        setupWindowInsets(R.id.rootPermissions)

        val btnGrant = findViewById<Button>(R.id.btnGrantPermissions)
        btnGrant.setOnClickListener {
            when {
                !PermissionUtils.hasUsageAccess(this) -> {
                    usageAccessLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                !PermissionUtils.hasOverlayPermission(this) -> {
                    overlayLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
                !PermissionUtils.hasNotificationPermission(this) -> {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> checkAndProceed()
            }
        }
    }

    private fun checkAndProceed() {
        if (PermissionUtils.hasUsageAccess(this)
            && PermissionUtils.hasOverlayPermission(this)
            && PermissionUtils.hasNotificationPermission(this)
        ) {
            // ✅ Mark in Prefs that permissions are granted
            com.example.oblique_android.services.Prefs.setHasAllPermissions(true)

            // ✅ Return to SplashActivity to re-run the whole flow
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

}
