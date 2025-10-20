package com.example.oblique_android.services

import android.app.*
import android.app.usage.UsageEvents
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
            Log.d("MonitoringService", "Foreground app: $foregroundApp")


            if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                Log.d("MonitoringService", "Detected blocked app: $foregroundApp")
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

    @Suppress("DEPRECATION")
    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000
        val events = usageStatsManager.queryEvents(beginTime, endTime)
        var lastApp: String? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            // Compatibility: handle both old and new event types
            val isForeground =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+ added Event.ACTIVITY_RESUMED and Event.ACTIVITY_PAUSED
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                }

            if (isForeground) {
                lastApp = event.packageName
            }
        }
        return lastApp
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
