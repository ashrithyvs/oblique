package com.example.oblique_android

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents

class BlockedAppsService : Service() {

    private var blockedApps = listOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 1000) // Check every 1 second
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = Notification.Builder(this, "blockedAppsChannel")
            .setContentTitle("Oblique Service Running")
            .setContentText("Monitoring blocked apps")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        blockedApps = intent?.getStringArrayListExtra("blockedApps") ?: emptyList()
        handler.post(runnable)
        return START_STICKY
    }

    private fun checkForegroundApp() {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usm.queryEvents(time - 2000, time)
        val event = UsageEvents.Event()
        var lastPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        lastPackage?.let {
            if (blockedApps.contains(it)) {
                val overlayIntent = Intent(this, OverlayService::class.java)
                overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startService(overlayIntent)
            }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "blockedAppsChannel",
                "Blocked Apps Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
