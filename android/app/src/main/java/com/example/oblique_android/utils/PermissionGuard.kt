package com.example.oblique_android.utils

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.activity.PermissionsActivity

object PermissionGuard {

    /**
     * Redirects to [PermissionsActivity] when required permissions are missing.
     * @return true if the current screen may continue.
     */
    fun ensureGranted(activity: AppCompatActivity): Boolean {
        if (PermissionUtils.hasRequiredPermissions(activity)) return true
        activity.startActivity(
            Intent(activity, PermissionsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        activity.finish()
        return false
    }
}
