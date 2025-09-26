package com.example.oblique_android.services

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.*
import android.util.Log
import com.example.oblique_android.R
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.TempUnlockManager
class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 1000
    private lateinit var usageStatsManager: UsageStatsManager
    private var currentBlockedApp: String? = null

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        startForeground(1, createNotification())
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val blockedApps = PrefsUtils.loadBlockedSet(this@MonitoringService)
            val foregroundApp = getForegroundApp()

            if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                if (!TempUnlockManager.isTempUnlocked(this@MonitoringService, foregroundApp)) {
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

    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
        )
        return usageStats?.maxByOrNull { it.lastTimeUsed }?.packageName
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
