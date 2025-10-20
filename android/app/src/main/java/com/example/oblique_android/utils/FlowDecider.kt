package com.example.oblique_android.utils

import android.content.Context
import android.util.Log
import com.example.oblique_android.activity.AppListActivity
import com.example.oblique_android.activity.DashboardActivity
import com.example.oblique_android.activity.LoginActivity
import com.example.oblique_android.activity.PermissionsActivity
import com.example.oblique_android.activity.PinConfirmActivity
import com.example.oblique_android.activity.PinSetupActivity
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.Prefs

object FlowDecider {
    fun nextActivity(context: Context): Class<*> {
        Log.d("pref",""+Prefs.getSelectedApps())
        return when {
            !Prefs.hasAllPermissions() -> PermissionsActivity::class.java
            TokenManager.getInstance(context).getToken().isNullOrEmpty() -> LoginActivity::class.java
            !Prefs.isPinSet() -> PinSetupActivity::class.java
            Prefs.isPinJustCreated() -> PinConfirmActivity::class.java
            Prefs.getSelectedApps().isEmpty() -> AppListActivity::class.java
            // goals check…
            else -> DashboardActivity::class.java
        }
    }
}
