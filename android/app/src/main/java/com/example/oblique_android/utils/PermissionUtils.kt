package com.example.oblique_android.utils

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object PermissionUtils {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasRequiredPermissions(ctx: Context): Boolean {
        return hasUsageAccess(ctx) &&
            hasOverlayPermission(ctx) &&
            hasNotificationPermission(ctx)
    }

    fun usageAccessSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun requestUsageAccess(context: Context) {
        context.startActivity(
            usageAccessSettingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun requestOverlayPermission(context: Context) {
        context.startActivity(
            overlaySettingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
