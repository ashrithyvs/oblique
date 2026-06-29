package com.example.oblique_android.gating

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oblique_android.services.MonitoringService
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.WorkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Starts [MonitoringService] only when goals require blocking (period not met after buffer).
 * Validation schedules run independently via [com.example.oblique_android.validation.GoalValidationScheduleManager].
 */
object MonitoringController {
    private const val TAG = "MonitoringController"

    suspend fun syncMonitoringState(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        if (!Prefs.isProtectionEnabled()) {
            stopMonitoring(appContext)
            cancelWakeSchedule(appContext)
            return@withContext
        }

        val gateManager = GoalGateManager(appContext)
        if (gateManager.needsForegroundMonitoring()) {
            Log.i(TAG, "Starting foreground monitoring — goal not met after buffer")
            startMonitoring(appContext)
        } else {
            Log.i(TAG, "Stopping foreground monitoring — goal met or still in grace period")
            stopMonitoring(appContext)
        }
        scheduleNextWake(appContext, gateManager)
    }

    fun startMonitoring(context: Context) {
        val intent = Intent(context, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopMonitoring(context: Context) {
        context.stopService(Intent(context, MonitoringService::class.java))
    }

    private suspend fun scheduleNextWake(context: Context, gateManager: GoalGateManager) {
        val appContext = context.applicationContext
        if (!Prefs.isProtectionEnabled()) {
            cancelWakeSchedule(appContext)
            return
        }

        val nextAt = gateManager.nextMonitoringTransitionMs() ?: run {
            cancelWakeSchedule(appContext)
            return
        }

        val delayMs = (nextAt - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<MonitoringWakeWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(WorkConstants.MONITORING_WAKE_TAG)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WorkConstants.MONITORING_WAKE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        Log.i(TAG, "Scheduled monitoring wake in ${delayMs / 1000}s")
    }

    private fun cancelWakeSchedule(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(WorkConstants.MONITORING_WAKE_WORK)
    }
}
