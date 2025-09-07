package com.example.oblique_android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.app.usage.UsageStatsManager
import android.util.Log

class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 1000 // check every 1 second
    private lateinit var usageStatsManager: UsageStatsManager
    private var currentBlockedApp: String? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        startForeground(1, createNotification())
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val blockedApps = getBlockedAppsFromPrefs()
            val foregroundApp = getForegroundApp()

            if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                if (!isTempUnlocked(foregroundApp)) {
                    // app is blocked AND not temporarily unlocked
                    if (currentBlockedApp != foregroundApp) {
                        currentBlockedApp = foregroundApp
                        Log.d("MonitoringService", "Blocking $foregroundApp")
                        val intent = Intent(this@MonitoringService, OverlayService::class.java)
                        intent.putExtra("blockedApp", foregroundApp)
                        startService(intent)
                    }
                } else {
                    Log.d("MonitoringService", "Skipping $foregroundApp (temporarily unlocked)")
                }
            } else {
                if (currentBlockedApp != null) {
                    Log.d("MonitoringService", "No longer blocking $currentBlockedApp")
                    stopService(Intent(this@MonitoringService, OverlayService::class.java))
                    currentBlockedApp = null
                }
            }
            handler.postDelayed(this, interval)
        }
    }

    private fun getBlockedAppsFromPrefs(): Set<String> {
        val prefs = getSharedPreferences("blocked_apps", MODE_PRIVATE)
        return prefs.getStringSet("pkgs", emptySet()) ?: emptySet()
    }

    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
        )
        return usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun isTempUnlocked(pkg: String): Boolean {
        val prefs = getSharedPreferences("temp_unlocked", MODE_PRIVATE)
        return prefs.getBoolean(pkg, false)
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Oblique Monitoring")
            .setContentText("Watching your blocked apps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
