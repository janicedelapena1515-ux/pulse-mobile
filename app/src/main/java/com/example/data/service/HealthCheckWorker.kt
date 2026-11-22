package com.example.data.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.OptimizationLogEntity
import com.example.data.local.PulseDatabase
import com.example.util.FormatUtils
import java.util.concurrent.TimeUnit

class HealthCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val telemetryService = DeviceTelemetryService(applicationContext)
        val snapshot = telemetryService.refreshSnapshot()
        val freeStoragePct = 100f - snapshot.storagePercent
        val temp = snapshot.batteryTempC

        val needsAttention = freeStoragePct < 15f || (temp != null && temp >= 42f) || snapshot.isLowMemory

        val db = PulseDatabase.getInstance(applicationContext)
        db.optimizationLogDao().insertLog(
            OptimizationLogEntity(
                timestampMillis = System.currentTimeMillis(),
                scoreBefore = snapshot.pulseScore.totalScore,
                scoreAfter = snapshot.pulseScore.totalScore,
                bytesRecovered = 0L,
                actionsSummary = "Scheduled background health check",
                honestNotes = "Storage free: ${FormatUtils.formatBytes(snapshot.storageAvailBytes)}, RAM: ${snapshot.ramPercent.toInt()}% used"
            )
        )

        if (needsAttention) {
            postHealthNotificationIfPermitted(
                score = snapshot.pulseScore.totalScore,
                freeStorage = FormatUtils.formatBytes(snapshot.storageAvailBytes),
                tempC = temp
            )
        }

        return Result.success()
    }

    private fun postHealthNotificationIfPermitted(score: Int, freeStorage: String, tempC: Float?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Health Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Alerts when storage headroom is low or device temperature is elevated"
            }
            nm.createNotificationChannel(channel)
        }

        val tempNote = tempC?.let { " | Temp ${FormatUtils.formatTempC(it)}" } ?: ""
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pulse_notification)
            .setContentTitle("Pulse Mobile Health Check ($score/100)")
            .setContentText("Free storage: $freeStorage$tempNote")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val WORK_NAME = "pulse_periodic_health_check"
        private const val CHANNEL_ID = "pulse_health_alerts"
        private const val NOTIFICATION_ID = 7021

        fun schedule(context: Context, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (enabled) {
                val request = PeriodicWorkRequestBuilder<HealthCheckWorker>(12, TimeUnit.HOURS)
                    .build()
                wm.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } else {
                wm.cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
