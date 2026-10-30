package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.HealthSyncApp
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ContinuousSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val notificationId = 1001
    private val channelId = "health_sync_continuous_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildSyncNotification("Đang chuẩn bị đồng bộ đa thiết bị…")
        startForeground(notificationId, notification)

        serviceScope.launch {
            val app = application as? HealthSyncApp
            val engine = app?.syncEngine

            while (isActive) {
                val summary = engine?.healthSummary?.value
                val activeCount = summary?.activeDeviceCount ?: 0
                val bpm = summary?.liveHeartRate ?: 72
                val consensus = summary?.syncConsensusPercent ?: 99.0f

                val text = if (activeCount > 0) {
                    "$activeCount thiết bị kết nối • Nhịp tim: $bpm BPM • Khớp: ${String.format("%.1f", consensus)}%"
                } else {
                    "Đang duy trì lắng nghe thiết bị Bluetooth BLE & Ứng dụng sức khỏe"
                }

                val updatedNotification = buildSyncNotification(text)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(notificationId, updatedNotification)

                delay(5000)
            }
        }

        return START_STICKY
    }

    private fun buildSyncNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ContinuousSyncService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("HealthSync • Đồng bộ liên tục")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .addAction(0, "Dừng đồng bộ", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Đồng bộ hóa dữ liệu sức khỏe",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị trạng thái đồng bộ đa đồng hồ thông minh theo thời gian thực"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "com.example.service.START_SYNC"
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_SYNC"

        fun start(context: Context) {
            val intent = Intent(context, ContinuousSyncService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ContinuousSyncService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
