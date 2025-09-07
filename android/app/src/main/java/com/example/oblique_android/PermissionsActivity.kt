package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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
                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
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
            val next = if (PINManager.isPinSet(this)) {
                DashboardActivity::class.java
            } else {
                PinSetupActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }
    }
}
