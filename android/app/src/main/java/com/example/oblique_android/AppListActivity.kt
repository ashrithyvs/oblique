package com.example.oblique_android

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AppListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnStartService: Button
    private lateinit var btnBlockApp: Button
    private val installedApps = mutableListOf<String>()
    private val blockedApps = mutableListOf<String>()
    private var selectedApp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        listView = findViewById(R.id.listViewApps)
        btnStartService = findViewById(R.id.btnStartService)
        btnBlockApp = findViewById(R.id.btnBlockApp)

        checkOverlayPermission()
        loadInstalledApps()

        listView.setOnItemClickListener { _, _, position, _ ->
            selectedApp = installedApps[position]
            Toast.makeText(this, "Selected: $selectedApp", Toast.LENGTH_SHORT).show()
        }

        btnBlockApp.setOnClickListener {
            selectedApp?.let {
                blockedApps.add(it)
                Toast.makeText(this, "Blocked: $it", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Select an app first", Toast.LENGTH_SHORT).show()
        }

        btnStartService.setOnClickListener {
            if (blockedApps.isEmpty()) {
                Toast.makeText(this, "No apps blocked", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val serviceIntent = Intent(this, BlockedAppsService::class.java)
            serviceIntent.putStringArrayListExtra("blockedApps", ArrayList(blockedApps))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun loadInstalledApps() {
        val pm: PackageManager = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installedApps.clear()
        apps.forEach {
            if (pm.getLaunchIntentForPackage(it.packageName) != null &&
                it.packageName != packageName) {
                installedApps.add(it.packageName)
            }
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, installedApps)
        listView.adapter = adapter
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }
    }
}
