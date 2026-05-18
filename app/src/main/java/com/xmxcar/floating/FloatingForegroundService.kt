package com.xmxcar.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

import com.xmxcar.launcher.R

/**
 * 悬浮窗前台保活服务
 * 支持音乐、地图、天气三种悬浮窗类型
 */
class FloatingForegroundService : Service() {

    private val CHANNEL_ID = "FloatingServiceChannel"
    private val NOTIFICATION_ID = 1001

    private var floatingManager: FloatingWindowManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        floatingManager = FloatingWindowManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = intent?.getStringExtra("type")
        val shouldStop = intent?.getBooleanExtra("stop", false) ?: false

        if (shouldStop && type != null) {
            // 停止指定类型的悬浮窗
            floatingManager?.hideFloatingWindow(type)
        } else if (type != null) {
            // 显示指定类型的悬浮窗
            floatingManager?.showFloatingWindow(type)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务销毁时停止所有悬浮窗
        floatingManager?.destroyAll()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 创建通知渠道（Android 8.0+需要）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "保持悬浮窗后台运行"
            channel.enableVibration(false)
            channel.setSound(null, null)

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("熊猫侠车载启动器")
            .setContentText("悬浮窗服务正在运行")
            .setSmallIcon(R.drawable.ic_music)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)

        return builder.build()
    }
}
