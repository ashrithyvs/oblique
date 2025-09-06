package com.example.oblique_android

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.POST_NOTIFICATIONS
        // Add other required permissions here
    )

    fun hasRequiredPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }
}
