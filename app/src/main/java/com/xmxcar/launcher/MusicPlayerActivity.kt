package com.xmxcar.launcher

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * 音乐播放器 Activity
 * 支持歌词滚动显示、上下曲、暂停、选择音乐插件
 * 识别多种共存版音乐应用
 */
class MusicPlayerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MusicPlayerActivity"
        private const val UPDATE_INTERVAL = 500L
    }

    // UI 组件
    private lateinit var ivAlbumArt: ImageView
    private lateinit var tvSongTitle: TextView
    private lateinit var tvArtist: TextView
    private lateinit var tvAlbum: TextView
    private lateinit var seekBarProgress: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrevious: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnSelectMusicApp: ImageButton
    private lateinit var scrollViewLyrics: ScrollView
    private lateinit var layoutLyrics: LinearLayout
    private lateinit var tvNoLyrics: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var mediaController: MediaController? = null
    private var currentMusicApp: AppRecognizer.AppInfo? = null
    private var isPlaying = false
    private var lyricsList = mutableListOf<LyricLine>()
    private var currentLyricIndex = -1

    data class LyricLine(
        val time: Long,
        val text: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_player)

        initViews()
        setupListeners()
        scanMusicApps()
        connectToMediaSession()
    }

    private fun initViews() {
        ivAlbumArt = findViewById(R.id.iv_album_art)
        tvSongTitle = findViewById(R.id.tv_song_title)
        tvArtist = findViewById(R.id.tv_artist)
        tvAlbum = findViewById(R.id.tv_album)
        seekBarProgress = findViewById(R.id.seek_bar_progress)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnPrevious = findViewById(R.id.btn_previous)
        btnNext = findViewById(R.id.btn_next)
        btnSelectMusicApp = findViewById(R.id.btn_select_music_app)
        scrollViewLyrics = findViewById(R.id.scroll_view_lyrics)
        layoutLyrics = findViewById(R.id.layout_lyrics)
        tvNoLyrics = findViewById(R.id.tv_no_lyrics)

        // 返回按钮
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
    }

    private fun setupListeners() {
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnPrevious.setOnClickListener { playPrevious() }
        btnNext.setOnClickListener { playNext() }
        btnSelectMusicApp.setOnClickListener { showMusicAppSelector() }

        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaController?.transportControls?.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * 扫描已安装的音乐应用
     */
    private fun scanMusicApps() {
        val musicApps = AppRecognizer.getInstalledMusicApps(this)
        if (musicApps.isNotEmpty()) {
            currentMusicApp = musicApps.first()
            updateMusicAppInfo()
        }
    }

    /**
     * 显示音乐应用选择器
     */
    private fun showMusicAppSelector() {
        val musicApps = AppRecognizer.getInstalledMusicApps(this)
        if (musicApps.isEmpty()) {
            Toast.makeText(this, "未检测到音乐应用", Toast.LENGTH_SHORT).show()
            return
        }

        val appNames = musicApps.map { app ->
            val dualLabel = if (app.isDualApp) " [共存版]" else ""
            "${app.appName}$dualLabel"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择音乐应用")
            .setItems(appNames) { _, which ->
                currentMusicApp = musicApps[which]
                updateMusicAppInfo()
                launchMusicApp()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 更新当前音乐应用信息
     */
    private fun updateMusicAppInfo() {
        currentMusicApp?.let { app ->
            findViewById<TextView>(R.id.tv_current_app).text = app.appName
            app.icon?.let { ivAlbumArt.setImageDrawable(it) }
        }
    }

    /**
     * 启动音乐应用
     */
    private fun launchMusicApp() {
        currentMusicApp?.let { app ->
            try {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    Toast.makeText(this, "已启动 ${app.appName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "无法启动 ${app.appName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 连接到媒体会话
     */
    private fun connectToMediaSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = sessionManager.getActiveSessions(ComponentName(this, NotificationListener::class.java))

            if (controllers.isNotEmpty()) {
                mediaController = controllers.first()
                mediaController?.registerCallback(mediaCallback)
                updatePlaybackState(mediaController?.playbackState)
                updateMetadata(mediaController?.metadata)
            }
        }
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackState(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }
    }

    /**
     * 更新播放状态
     */
    private fun updatePlaybackState(state: PlaybackState?) {
        state?.let {
            isPlaying = it.state == PlaybackState.STATE_PLAYING
            updatePlayButton()

            // 更新进度
            val position = it.position
            val bufferedPosition = it.bufferedPosition
            seekBarProgress.progress = position.toInt()
            seekBarProgress.secondaryProgress = bufferedPosition.toInt()
            tvCurrentTime.text = formatTime(position)

            // 更新歌词高亮
            updateLyricsHighlight(position)
        }
    }

    /**
     * 更新媒体元数据
     */
    private fun updateMetadata(metadata: MediaMetadata?) {
        metadata?.let {
            val title = it.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知歌曲"
            val artist = it.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知艺术家"
            val album = it.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "未知专辑"
            val duration = it.getLong(MediaMetadata.METADATA_KEY_DURATION)

            tvSongTitle.text = title
            tvArtist.text = artist
            tvAlbum.text = album
            tvTotalTime.text = formatTime(duration)
            seekBarProgress.max = duration.toInt()

            // 尝试加载专辑封面
            val art = it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            if (art != null) {
                ivAlbumArt.setImageBitmap(art)
            }

            // 模拟加载歌词（实际应从音乐应用或网络获取）
            loadMockLyrics(title, artist)
        }
    }

    /**
     * 加载模拟歌词
     */
    private fun loadMockLyrics(title: String, artist: String) {
        // 这里应该根据歌曲信息从音乐应用或网络获取真实歌词
        // 目前使用模拟数据演示功能
        lyricsList.clear()
        val mockLyrics = listOf(
            LyricLine(0, "$title - $artist"),
            LyricLine(5000, "正在播放音乐..."),
            LyricLine(10000, "歌词功能需要音乐应用支持"),
            LyricLine(15000, "请确保音乐应用正在播放"),
            LyricLine(20000, "支持上下曲切换"),
            LyricLine(25000, "支持播放/暂停控制"),
            LyricLine(30000, "支持多种共存版音乐应用")
        )
        lyricsList.addAll(mockLyrics)
        displayLyrics()
    }

    /**
     * 显示歌词
     */
    private fun displayLyrics() {
        layoutLyrics.removeAllViews()

        if (lyricsList.isEmpty()) {
            tvNoLyrics.visibility = View.VISIBLE
            return
        }

        tvNoLyrics.visibility = View.GONE

        lyricsList.forEachIndexed { index, lyric ->
            val tvLyric = TextView(this).apply {
                text = lyric.text
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(32, 16, 32, 16)
                gravity = android.view.Gravity.CENTER
                tag = index
            }
            layoutLyrics.addView(tvLyric)
        }
    }

    /**
     * 更新歌词高亮
     */
    private fun updateLyricsHighlight(currentPosition: Long) {
        val newIndex = lyricsList.indexOfLast { it.time <= currentPosition }
        if (newIndex != currentLyricIndex && newIndex >= 0) {
            currentLyricIndex = newIndex

            // 更新高亮
            for (i in 0 until layoutLyrics.childCount) {
                val tv = layoutLyrics.getChildAt(i) as TextView
                if (i == newIndex) {
                    tv.setTextColor(ContextCompat.getColor(this, R.color.accent_color))
                    tv.textSize = 18f
                    tv.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start()

                    // 滚动到中间
                    scrollViewLyrics.post {
                        val scrollY = tv.top - scrollViewLyrics.height / 2 + tv.height / 2
                        scrollViewLyrics.smoothScrollTo(0, scrollY.coerceAtLeast(0))
                    }
                } else {
                    tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    tv.textSize = 16f
                    tv.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }
            }
        }
    }

    /**
     * 播放/暂停切换
     */
    private fun togglePlayPause() {
        mediaController?.let { controller ->
            if (isPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        } ?: run {
            // 如果没有媒体控制器，尝试启动音乐应用
            launchMusicApp()
        }
    }

    /**
     * 上一曲
     */
    private fun playPrevious() {
        mediaController?.transportControls?.skipToPrevious()
            ?: Toast.makeText(this, "请先启动音乐应用", Toast.LENGTH_SHORT).show()
    }

    /**
     * 下一曲
     */
    private fun playNext() {
        mediaController?.transportControls?.skipToNext()
            ?: Toast.makeText(this, "请先启动音乐应用", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新播放按钮状态
     */
    private fun updatePlayButton() {
        val icon = if (isPlaying) {
            R.drawable.ic_pause
        } else {
            R.drawable.ic_play
        }
        btnPlayPause.setImageResource(icon)
    }

    /**
     * 格式化时间
     */
    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    override fun onResume() {
        super.onResume()
        connectToMediaSession()
        startProgressUpdate()
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdate()
        mediaController?.unregisterCallback(mediaCallback)
    }

    private fun startProgressUpdate() {
        handler.postDelayed(progressUpdateRunnable, UPDATE_INTERVAL)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacks(progressUpdateRunnable)
    }

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            mediaController?.playbackState?.let { state ->
                val position = state.position
                seekBarProgress.progress = position.toInt()
                tvCurrentTime.text = formatTime(position)
                updateLyricsHighlight(position)
            }
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    /**
     * 通知监听服务（用于获取媒体会话）
     */
    class NotificationListener : android.service.notification.NotificationListenerService()
}
