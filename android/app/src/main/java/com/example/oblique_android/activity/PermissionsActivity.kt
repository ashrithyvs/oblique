package com.example.oblique_android.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.R
import com.example.oblique_android.utils.OnboardingRouter
import com.example.oblique_android.utils.PermissionUtils
import com.example.oblique_android.utils.setupWindowInsets
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class PermissionsActivity : AppCompatActivity() {

    private lateinit var successBanner: View
    private lateinit var btnContinue: MaterialButton

    private val usageAccessLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshUi()
    }

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshUi()
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        setupWindowInsets(R.id.rootPermissions)

        successBanner = findViewById(R.id.permissionSuccess)
        btnContinue = findViewById(R.id.btnContinueSetup)

        btnContinue.setOnClickListener {
            if (PermissionUtils.hasRequiredPermissions(this)) {
                proceedToNextStep()
            } else {
                requestNextMissingPermission()
            }
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val allGranted = PermissionUtils.hasRequiredPermissions(this)
        successBanner.visibility = if (allGranted) View.VISIBLE else View.GONE
        btnContinue.text = if (allGranted) {
            getString(R.string.permissions_continue)
        } else {
            getString(R.string.permissions_grant)
        }
    }

    private fun requestNextMissingPermission() {
        when {
            !PermissionUtils.hasOverlayPermission(this) -> {
                overlayLauncher.launch(PermissionUtils.overlaySettingsIntent(this))
            }
            !PermissionUtils.hasUsageAccess(this) -> {
                usageAccessLauncher.launch(PermissionUtils.usageAccessSettingsIntent(this))
            }
            !PermissionUtils.hasNotificationPermission(this) -> {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> proceedToNextStep()
        }
    }

    private fun proceedToNextStep() {
        if (!PermissionUtils.hasRequiredPermissions(this)) {
            refreshUi()
            return
        }

        lifecycleScope.launch {
            val next = OnboardingRouter.nextSuspend(this@PermissionsActivity, validateToken = false)
            startActivity(Intent(this@PermissionsActivity, next))
            finish()
        }
    }
}
