package com.example.oblique_android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)
        if (!isFirstLaunch) {
            startActivity(Intent(this, AppListActivity::class.java))
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Usage Access & Overlay Permission")
            .setMessage(
                "This app helps you reduce distractions by blocking apps you choose. " +
                        "It requires Usage Access permission to monitor app usage and Overlay permission " +
                        "to show a blocking screen. You can override blocked apps with a PIN."
            )
            .setPositiveButton("Continue") { _, _ ->
                requestPermissions()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestPermissions() {
        val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivityForResult(usageIntent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val overlayIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(overlayIntent, 1002)
            } else {
                finishSetup()
            }
        } else if (requestCode == 1002) {
            finishSetup()
        }
    }

    private fun finishSetup() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("first_launch", false).apply()

        startActivity(Intent(this, AppListActivity::class.java))
        finish()
    }
}
