package com.example.oblique_android.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.example.oblique_android.R
import com.example.oblique_android.gating.GoalGateManager
import com.example.oblique_android.utils.TempUnlockManager
import kotlinx.coroutines.runBlocking

class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 10_000L
    private lateinit var usageStatsManager: UsageStatsManager
    private var currentBlockedApp: String? = null
    private val TAG = "MonitoringService"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MonitoringService started")

        startForeground(1, createNotification())

        if (!hasUsageAccessPermission()) {
            Log.e(TAG, "Missing Usage Access permission — stopping monitoring.")
            stopSelf()
            return
        }

        usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        handler.post(checkRunnable)
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            val blockedApps = com.example.oblique_android.utils.PrefsUtils.loadBlockedSet(this@MonitoringService)
            val foregroundApp = getForegroundApp()
            Log.d(TAG, "Foreground app: $foregroundApp")

            if (foregroundApp != null && blockedApps.contains(foregroundApp)) {
                val goalsGateOpen = runBlocking {
                    GoalGateManager(this@MonitoringService).shouldBlockApps().not()
                }
                if (goalsGateOpen) {
                    Log.d(TAG, "Skipping $foregroundApp (goals satisfied until next deadline+buffer)")
                    if (currentBlockedApp != null) {
                        stopService(Intent(this@MonitoringService, OverlayService::class.java))
                        currentBlockedApp = null
                    }
                } else if (!TempUnlockManager.isTempUnlocked(this@MonitoringService, foregroundApp)) {
                    if (currentBlockedApp != foregroundApp) {
                        currentBlockedApp = foregroundApp
                        Log.d(TAG, "Blocking $foregroundApp")
                        val intent = Intent(this@MonitoringService, OverlayService::class.java)
                        intent.putExtra("blockedApp", foregroundApp)
                        startService(intent)
                    }
                } else {
                    Log.d(TAG, "Skipping $foregroundApp (temporarily unlocked)")
                }
            } else {
                if (currentBlockedApp != null) {
                    Log.d(TAG, "No longer blocking $currentBlockedApp")
                    stopService(Intent(this@MonitoringService, OverlayService::class.java))
                    currentBlockedApp = null
                }
            }
            handler.postDelayed(this, interval)
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun getForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000

        val events = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForeground = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                else ->
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isForeground) {
                lastForegroundApp = event.packageName
            }
        }

        return lastForegroundApp
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
                NotificationManager.IMPORTANCE_LOW,
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
