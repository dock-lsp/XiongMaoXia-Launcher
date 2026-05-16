package com.pandora.carlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

/**
 * 悬浮音乐卡片服务
 * 监听系统音乐播放状态，显示歌词和播放控制
 */
class FloatingMusicService : Service() {

    companion object {
        private const val TAG = "FloatingMusic"

        // 通知相关
        private const val CHANNEL_ID = "floating_music_channel"
        private const val NOTIFICATION_ID = 1001

        // 音乐广播 Action（标准 Android 音乐播放广播）
        private const val ACTION_METADATA_CHANGED = "com.android.music.metachanged"
        private const val ACTION_PLAY_STATE_CHANGED = "com.android.music.playstatechanged"
        private const val ACTION_PLAYBACK_COMPLETED = "com.android.music.playbackcomplete"

        // 酷我音乐
        private const val ACTION_KUWO_META = "cn.kuwo.player.metachanged"
        private const val ACTION_KUWO_PLAYSTATE = "cn.kuwo.player.playstatechanged"

        // 网易云音乐
        private const val ACTION_NETEASE_META = "com.netease.cloudmusic.metachanged"
        private const val ACTION_NETEASE_PLAYSTATE = "com.netease.cloudmusic.playstatechanged"

        // 默认尺寸
        private const val DEFAULT_WIDTH = 320
        private const val DEFAULT_HEIGHT = 180
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    // UI 组件
    private var tvSong: TextView? = null
    private var tvArtist: TextView? = null
    private var tvLyric: TextView? = null
    private var btnPlay: ImageView? = null
    private var btnPrev: ImageView? = null
    private var btnNext: ImageView? = null
    private var btnClose: ImageView? = null
    private var btnOpenApp: ImageView? = null

    // 当前播放状态
    private var isPlaying = false
    private var currentSong = "未播放"
    private var currentArtist = ""
    private var currentMusicPackage: String? = null

    // 拖动相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    // 音乐广播接收器
    private val musicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            handleMusicIntent(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        registerMusicReceiver()
        
        // 创建通知通道（Android 8.0+）
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (floatingView == null) {
            createFloatingView()
        }
        return START_STICKY
    }

    /**
     * 创建通知通道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮音乐",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮音乐播放控制"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent, 
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.apply {
            setContentTitle("熊猫侠 - 悬浮音乐")
            setContentText("$currentSong - $currentArtist")
            setSmallIcon(R.drawable.ic_music)
            setContentIntent(pendingIntent)
            setOngoing(true)
            setShowWhen(false)
        }.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingView()
        unregisterMusicReceiver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 创建悬浮窗
     */
    private fun createFloatingView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
                stopSelf()
                return
            }
        }

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.layout_floating_music, null)

        // 初始化 WindowManager 参数
        params = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = dpToPx(DEFAULT_WIDTH)
            height = dpToPx(DEFAULT_HEIGHT)
            gravity = Gravity.TOP or Gravity.END
            x = dpToPx(16)
            y = dpToPx(100)
        }

        // 初始化 UI
        initUI()

        // 添加到窗口
        windowManager?.addView(floatingView, params)
    }

    /**
     * 初始化 UI 组件
     */
    private fun initUI() {
        floatingView?.run {
            tvSong = findViewById(R.id.music_tv_song)
            tvArtist = findViewById(R.id.music_tv_artist)
            tvLyric = findViewById(R.id.music_tv_lyric_current)
            btnPlay = findViewById(R.id.music_btn_play)
            btnPrev = findViewById(R.id.music_btn_prev)
            btnNext = findViewById(R.id.music_btn_next)
            btnClose = findViewById(R.id.music_btn_close)
            btnOpenApp = findViewById(R.id.music_btn_open_app)

            // 设置点击事件
            btnPlay?.setOnClickListener { togglePlayPause() }
            btnPrev?.setOnClickListener { sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
            btnNext?.setOnClickListener { sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_NEXT) }
            btnClose?.setOnClickListener { stopSelf() }
            btnOpenApp?.setOnClickListener { openMusicApp() }

            // 设置拖动
            setOnTouchListener { _, event -> handleTouch(event) }

            // 更新显示
            updateDisplay()
        }
    }

    /**
     * 处理触摸事件（拖动）
     */
    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params?.x ?: 0
                initialY = params?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (abs(dx) > 5 || abs(dy) > 5) {
                    isDragging = true
                    params?.x = (initialX - dx).toInt()
                    params?.y = (initialY + dy).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                return isDragging
            }
        }
        return false
    }

    /**
     * 注册音乐广播接收器
     */
    private fun registerMusicReceiver() {
        val filter = IntentFilter().apply {
            // 标准 Android 音乐广播
            addAction(ACTION_METADATA_CHANGED)
            addAction(ACTION_PLAY_STATE_CHANGED)
            addAction(ACTION_PLAYBACK_COMPLETED)

            // 酷我音乐
            addAction(ACTION_KUWO_META)
            addAction(ACTION_KUWO_PLAYSTATE)

            // 网易云音乐
            addAction(ACTION_NETEASE_META)
            addAction(ACTION_NETEASE_PLAYSTATE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(musicReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(musicReceiver, filter)
        }
    }

    /**
     * 注销广播接收器
     */
    private fun unregisterMusicReceiver() {
        try {
            unregisterReceiver(musicReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * 处理音乐广播
     */
    private fun handleMusicIntent(intent: Intent) {
        // 提取歌曲信息
        val song = intent.getStringExtra("track") ?: intent.getStringExtra("song") ?: ""
        val artist = intent.getStringExtra("artist") ?: intent.getStringExtra("singer") ?: ""
        val playing = intent.getBooleanExtra("playing", false)

        if (song.isNotEmpty()) {
            currentSong = song
            currentArtist = artist
            isPlaying = playing
            currentMusicPackage = intent.getStringExtra("package")

            updateDisplay()
        }
    }

    /**
     * 更新显示
     */
    private fun updateDisplay() {
        tvSong?.text = currentSong
        tvArtist?.text = if (currentArtist.isNotEmpty()) "- $currentArtist" else ""
        tvLyric?.text = currentSong

        // 更新播放按钮图标
        btnPlay?.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    /**
     * 切换播放/暂停
     */
    private fun togglePlayPause() {
        sendMediaButton(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        isPlaying = !isPlaying
        updateDisplay()
    }

    /**
     * 发送媒体按钮事件
     */
    private fun sendMediaButton(keyCode: Int) {
        val eventDown = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_DOWN, keyCode
        )
        val eventUp = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_UP, keyCode
        )

        sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
            Intent.EXTRA_KEY_EVENT, eventDown
        ))
        sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).putExtra(
            Intent.EXTRA_KEY_EVENT, eventUp
        ))
    }

    /**
     * 打开音乐应用
     */
    private fun openMusicApp() {
        val musicApps = AppRecognizer.getInstalledMusicApps(this)
        if (musicApps.isEmpty()) {
            Toast.makeText(this, "未检测到音乐应用", Toast.LENGTH_SHORT).show()
            return
        }

        // 优先打开当前播放的应用，否则打开第一个
        val targetApp = musicApps.find { it.packageName == currentMusicPackage } 
            ?: musicApps.first()

        try {
            val intent = packageManager.getLaunchIntentForPackage(targetApp.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 移除悬浮窗
     */
    private fun removeFloatingView() {
        try {
            floatingView?.let { windowManager?.removeView(it) }
            floatingView = null
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * dp 转 px
     */
    private fun dpToPx(dp: Int): Int {
        val metrics = resources.displayMetrics
        return (dp * metrics.density).toInt()
    }
}
