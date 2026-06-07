package com.example.oblique_android.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.example.oblique_android.R
import com.example.oblique_android.repository.GoalsRepository
import com.example.oblique_android.utils.NetworkUtils
import com.example.oblique_android.utils.PrefsUtils
import com.example.oblique_android.utils.TempUnlockManager
import com.example.oblique_android.validation.GoalValidationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonitoringService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval: Long = 10_000L
    private lateinit var usageStatsManager: UsageStatsManager
    private var currentBlockedApp: String? = null
    private val TAG = "MonitoringService"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {

            super.onCreate()
            Log.i(TAG, "MonitoringService started")

        if (!hasUsageAccessPermission()) {
            Log.e(TAG, "❌ Missing Usage Access permission. Open Settings -> Usage Access and enable it for this app.")
            stopSelf()
            return
        }

            if (!NetworkUtils.hasInternet(this)) {
                Log.w(TAG, "Internet unavailable → skipping validation scheduling to prevent user lockout")
                return
            }

            usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
            startForeground(1, createNotification())
            handler.post(checkRunnable)

            val scheduler = GoalValidationScheduler(applicationContext)

            // Schedule validation for all LeetCode goals automatically
            ioScope.launch {
                try {
                    val repo = GoalsRepository(applicationContext)
                    val goals = repo.listGoals()
                    for (g in goals) {
                        if (g.platform.lowercase() == "leetcode" && g.status == "active") {
                            scheduler.schedule(g.id, g.deadline)
                        }
                    }
                    Log.i(TAG, "Goal validation schedules initialized")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule validations: ${e.message}")
                }
        }

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

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun getForegroundApp(): String? {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000 // Look back 10 seconds

        val events = usageStatsManager.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null
        var lastEventTime = 0L

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
                lastEventTime = event.timeStamp
            }
        }

        if (lastForegroundApp == null) {
            Log.w(TAG, "⚠️ No foreground event detected in last 10s — check Usage Access permission.")
        } else {
            Log.d(TAG, "✅ Foreground app detected: $lastForegroundApp at $lastEventTime")
        }

        return lastForegroundApp
    }


    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        GoalValidationScheduler(this).cancelAll()
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
