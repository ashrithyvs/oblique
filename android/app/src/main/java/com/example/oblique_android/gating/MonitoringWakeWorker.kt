package com.example.oblique_android.gating

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Wakes at deadline+buffer or unblock-window expiry to start/stop [MonitoringService]. */
class MonitoringWakeWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Monitoring wake — re-evaluating gate state")
        MonitoringController.syncMonitoringState(applicationContext)
        return Result.success()
    }

    companion object {
        private const val TAG = "MonitoringWakeWorker"
    }
}
