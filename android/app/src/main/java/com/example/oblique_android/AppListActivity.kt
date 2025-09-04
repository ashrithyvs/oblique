package com.example.oblique_android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppListActivity : AppCompatActivity() {

    private lateinit var startMonitoringButton: Button
    private lateinit var stopMonitoringButton: Button
    private lateinit var recyclerView: RecyclerView
    private val selectedApps = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        recyclerView = findViewById(R.id.recyclerView)
        startMonitoringButton = findViewById(R.id.btnStartMonitoring)
        stopMonitoringButton = findViewById(R.id.btnStopMonitoring)

        val installedApps = packageManager.getInstalledApplications(0)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppAdapter(installedApps) { app ->
            if (selectedApps.contains(app.packageName)) selectedApps.remove(app.packageName)
            else selectedApps.add(app.packageName)
        }

        startMonitoringButton.setOnClickListener {
            BlockedAppsStore.saveBlockedApps(this, selectedApps)
            val intent = Intent(this, MonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent)
            else startService(intent)
        }

        stopMonitoringButton.setOnClickListener {
            stopService(Intent(this, MonitoringService::class.java))
        }
    }
}
