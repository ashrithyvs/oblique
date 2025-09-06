package com.example.oblique_android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.app.usage.UsageStatsManager
import kotlinx.coroutines.runBlocking

class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 1000 // check every 1 second
    private lateinit var usageStatsManager: UsageStatsManager
    private var currentBlockedApp: String? = null
    private lateinit var repo: BlockedAppRepository

    override fun onCreate() {
        super.onCreate()
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        repo = BlockedAppRepository.getInstance(this)
        startForeground(1, createNotification())
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val blockedApps = runBlocking { repo.getBlockedPackageNames() }
            val foregroundApp = getForegroundApp()

            if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                if (currentBlockedApp != foregroundApp) {
                    currentBlockedApp = foregroundApp
                    val intent = Intent(this@MonitoringService, OverlayService::class.java)
                    intent.putExtra("blockedApp", foregroundApp)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startService(intent)
                }
            } else {
                if (currentBlockedApp != null) {
                    stopService(Intent(this@MonitoringService, OverlayService::class.java))
                    currentBlockedApp = null
                }
            }

            handler.postDelayed(this, interval)
        }
    }

    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 2000 // last 2 seconds
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
            .setContentTitle("Regretn't Monitoring")
            .setContentText("Watching your blocked apps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
}
