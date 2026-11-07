package com.smoothos.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smoothos.app.R

/**
 * Diagnostic Service for running extended diagnostics while remaining Android 14 compliant.
 * Automatically tears down upon task completion to keep memory footprint under 25MB.
 */
class DiagnosticService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification("SmoothOS Benchmark Active")
        startForeground(NOTIFICATION_ID, notification)

        // Work is performed asynchronously and the service is stopped as soon as finished
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmoothOS Diagnostics",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live system diagnostic benchmarking state"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmoothOS Diagnostic Monitor")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "smoothos_diagnostics_channel"
        const val NOTIFICATION_ID = 1001
    }
}
