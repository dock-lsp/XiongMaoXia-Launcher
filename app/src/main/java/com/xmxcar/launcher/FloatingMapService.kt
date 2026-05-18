package com.xmxcar.launcher

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * 地图导航悬浮窗服务
 *
 * 使用 WindowManager TYPE_APPLICATION_OVERLAY 创建悬浮窗
 * 通过 Intent 拉起已安装的高德/百度/腾讯导航
 * 支持车机版和手机版导航应用
 * 导航缩窗后悬浮窗固定在应用顶层显示
 */
class FloatingMapService : Service() {

    companion object {
        private const val TAG = "FloatingMapService"

        // 传递给服务的参数
        const val EXTRA_MAP_TYPE = "extra_map_type"  // "amap" | "baidu" | "tencent"
        const val EXTRA_IS_CAR_VERSION = "extra_is_car_version"  // true=车机版 false=手机版
        const val EXTRA_DEST_LAT = "extra_dest_lat"
        const val EXTRA_DEST_LNG = "extra_dest_lng"
        const val EXTRA_DEST_NAME = "extra_dest_name"

        // 默认窗口尺寸（中间区域）
        private const val DEFAULT_WIDTH_DP = 400
        private const val DEFAULT_HEIGHT_DP = 300
        private const val MIN_WIDTH_DP = 200
        private const val MIN_HEIGHT_DP = 150
        private const val MAX_WIDTH_DP = 800
        private const val MAX_HEIGHT_DP = 600

        /**
         * 启动导航悬浮窗
         */
        fun start(
            context: Context,
            mapType: String = "amap",
            isCarVersion: Boolean = true,
            destLat: Double? = null,
            destLng: Double? = null,
            destName: String? = null
        ) {
            val intent = Intent(context, FloatingMapService::class.java).apply {
                putExtra(EXTRA_MAP_TYPE, mapType)
                putExtra(EXTRA_IS_CAR_VERSION, isCarVersion)
                destLat?.let { putExtra(EXTRA_DEST_LAT, it) }
                destLng?.let { putExtra(EXTRA_DEST_LNG, it) }
                destName?.let { putExtra(EXTRA_DEST_NAME, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * 检查悬浮窗权限是否已授予
         */
        fun canDrawOverlays(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }

        /**
         * 跳转到悬浮窗权限设置页面
         */
        fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    // 地图配置
    data class MapConfig(
        val type: String,
        val name: String,
        val carPackage: String,
        val phonePackage: String,
        val carActivity: String?,
        val phoneActivity: String?
    )

    private val mapConfigs = listOf(
        MapConfig(
            type = "amap",
            name = "高德地图",
            carPackage = "com.autonavi.amapauto",
            phonePackage = "com.autonavi.minimap",
            carActivity = "com.autonavi.map.auto.NewMapActivity",
            phoneActivity = "com.autonavi.map.activity.NewMapActivity"
        ),
        MapConfig(
            type = "baidu",
            name = "百度地图",
            carPackage = "com.baidu.naviauto",
            phonePackage = "com.baidu.BaiduMap",
            carActivity = "com.baidu.naviauto.BaiduNaviAutoMapActivity",
            phoneActivity = "com.baidu.baidumaps.WelcomeActivity"
        ),
        MapConfig(
            type = "tencent",
            name = "腾讯地图",
            carPackage = "com.tencent.map",
            phonePackage = "com.tencent.map",
            carActivity = null,
            phoneActivity = "com.tencent.mapsdk.demo.MainActivity"
        )
    )

    private var windowManager: WindowManager? = null
    private var floatingContainer: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var currentWidthDp = DEFAULT_WIDTH_DP
    private var currentHeightDp = DEFAULT_HEIGHT_DP

    // 拖动相关
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlays(this)) {
            createPermissionRequestView()
            return START_NOT_STICKY
        }

        floatingContainer?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
            floatingContainer = null
        }

        if (floatingContainer == null) {
            val mapType = intent?.getStringExtra(EXTRA_MAP_TYPE) ?: "amap"
            val isCarVersion = intent?.getBooleanExtra(EXTRA_IS_CAR_VERSION, true) ?: true
            val destLat = intent?.getDoubleExtra(EXTRA_DEST_LAT, 0.0)
            val destLng = intent?.getDoubleExtra(EXTRA_DEST_LNG, 0.0)
            val destName = intent?.getStringExtra(EXTRA_DEST_NAME)
            createFloatingView(mapType, isCarVersion, destLat, destLng, destName)
        }

        return START_NOT_STICKY
    }

    /**
     * 创建权限请求视图
     */
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun createPermissionRequestView() {
        if (floatingContainer != null) return

        val density = resources.displayMetrics.density
        val widthPx = (DEFAULT_WIDTH_DP * density).toInt()
        val heightPx = (DEFAULT_HEIGHT_DP * density).toInt()

        val layout = FrameLayout(this).apply {
            setBackgroundColor(0xCC1A1A2E.toInt())
            elevation = 8f * density
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }

        val titleView = TextView(this).apply {
            text = "需要悬浮窗权限"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        val descView = TextView(this).apply {
            text = "请授予「显示在其他应用上层」权限\n以便显示导航悬浮窗"
            textSize = 14f
            setTextColor(0xB3FFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        val btnGrant = android.widget.Button(this).apply {
            text = "前往授权"
            setOnClickListener { requestOverlayPermission(this@FloatingMapService) }
        }

        val btnClose = android.widget.Button(this).apply {
            text = "取消"
            setOnClickListener { stopSelf() }
            setPadding(0, 16, 0, 0)
        }

        contentLayout.addView(titleView)
        contentLayout.addView(descView)
        contentLayout.addView(btnGrant)
        contentLayout.addView(btnClose)
        layout.addView(contentLayout)

        params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        try {
            windowManager?.addView(layout, params)
            floatingContainer = layout
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 创建导航悬浮窗
     */
    @SuppressLint("InflateParams", "SetTextI18n")
    private fun createFloatingView(
        mapType: String,
        isCarVersion: Boolean,
        destLat: Double?,
        destLng: Double?,
        destName: String?
    ) {
        val density = resources.displayMetrics.density
        val widthPx = (currentWidthDp * density).toInt()
        val heightPx = (currentHeightDp * density).toInt()

        val config = mapConfigs.find { it.type == mapType } ?: mapConfigs[0]
        val packageName = if (isCarVersion) config.carPackage else config.phonePackage

        val isInstalled = try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }

        // 创建 FrameLayout 容器
        val container = FrameLayout(this).apply {
            setBackgroundColor(0xCC1A1A2E.toInt())
            elevation = 8f * density
        }

        // ========== 标题栏 ==========
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0x80000000.toInt())
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (40 * density).toInt()
            )
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.drawable.ic_navigation)
            setColorFilter(0xFF00CED1.toInt())
            layoutParams = LinearLayout.LayoutParams(
                (20 * density).toInt(),
                (20 * density).toInt()
            ).apply { marginEnd = (8 * density).toInt() }
        }

        val titleView = TextView(this).apply {
            text = if (isInstalled) "${config.name}${if (isCarVersion) "车机版" else "手机版"}" else config.name
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val statusTextView = TextView(this).apply {
            text = if (isInstalled) "点击启动" else "未安装"
            textSize = 11f
            setTextColor(0x80FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (8 * density).toInt() }
        }

        val btnZoomOut = ImageView(this).apply {
            setImageResource(R.drawable.ic_minus)
            setColorFilter(0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                (28 * density).toInt(),
                (28 * density).toInt()
            ).apply { marginEnd = (4 * density).toInt() }
        }

        val btnZoomIn = ImageView(this).apply {
            setImageResource(R.drawable.ic_plus)
            setColorFilter(0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                (28 * density).toInt(),
                (28 * density).toInt()
            ).apply { marginEnd = (4 * density).toInt() }
        }

        val btnClose = ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(0xFFFFFFFF.toInt())
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                (28 * density).toInt(),
                (28 * density).toInt()
            )
        }

        titleBar.addView(iconView)
        titleBar.addView(titleView)
        titleBar.addView(statusTextView)
        titleBar.addView(btnZoomOut)
        titleBar.addView(btnZoomIn)
        titleBar.addView(btnClose)

        // ========== 内容区域 ==========
        val contentLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { topMargin = (40 * density).toInt() }
            setBackgroundColor(0xFF0D1642.toInt())
        }

        val loadingLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyle).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(0xFF00CED1.toInt())
        }

        val loadingText = TextView(this).apply {
            text = if (isInstalled) "正在启动导航..." else "请先安装${config.name}"
            textSize = 13f
            setTextColor(0x80FFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, (12 * density).toInt(), 0, 0)
        }

        loadingLayout.addView(progressBar)
        loadingLayout.addView(loadingText)
        contentLayout.addView(loadingLayout)

        container.addView(titleBar)
        container.addView(contentLayout)

        // 缩放按钮
        btnZoomIn.setOnClickListener { resizeWindow(1.2f) }
        btnZoomOut.setOnClickListener { resizeWindow(0.8f) }
        btnClose.setOnClickListener { stopSelf() }

        // 点击内容区域启动导航
        contentLayout.setOnClickListener {
            if (!isDragging && isInstalled) {
                launchNavigation(config, isCarVersion, destLat, destLng, destName)
            } else if (!isInstalled) {
                openAppMarket(packageName)
            }
        }

        // 标题栏拖动
        titleBar.setOnTouchListener { _, event ->
            handleDrag(event)
            true
        }

        // 计算初始位置（屏幕中间偏上）
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val initX = (screenWidth - widthPx) / 2
        val initY = screenHeight / 4

        params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initX
            y = initY
        }

        try {
            windowManager?.addView(container, params)
            floatingContainer = container
            handler.postDelayed({
                if (isInstalled) {
                    launchNavigation(config, isCarVersion, destLat, destLng, destName)
                }
            }, 500)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    /**
     * 启动导航
     */
    private fun launchNavigation(
        config: MapConfig,
        isCarVersion: Boolean,
        destLat: Double?,
        destLng: Double?,
        destName: String?
    ) {
        val packageName = if (isCarVersion) config.carPackage else config.phonePackage
        val activityName = if (isCarVersion) config.carActivity else config.phoneActivity

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                `package` = packageName
                if (activityName != null) {
                    component = ComponentName(packageName, activityName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // 高德地图：传递导航目标
                if (config.type == "amap") {
                    if (destLat != null && destLng != null) {
                        val uri = if (destName != null) {
                            "androidamap://route?sourceApplication=XMLauncher&slat=&slon=&sname=&dlat=$destLat&dlon=$destLng&dname=$destName&dev=0&m=0&t=1"
                        } else {
                            "androidamap://openNavigation?sourceApplication=XMLauncher&poiid=$destLat,$destLng"
                        }
                        setData(Uri.parse(uri))
                    }
                }
                // 百度地图
                else if (config.type == "baidu") {
                    if (destLat != null && destLng != null) {
                        val bdLat = destLat + 0.0065
                        val bdLng = destLng + 0.0060
                        val uri = "baidumap://map/navi?location=$bdLat,$bdLng&coord_type=bd09ll"
                        setData(Uri.parse(uri))
                    }
                }
            }

            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                startActivity(intent)
                updateStatus("正在导航中")
            } else {
                val simpleIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (simpleIntent != null) {
                    simpleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(simpleIntent)
                    updateStatus("正在导航中")
                } else {
                    updateStatus("无法启动")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            updateStatus("启动失败")
        }
    }

    /**
     * 打开应用市场
     */
    private fun openAppMarket(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e2: Exception) {
                updateStatus("无法打开应用市场")
            }
        }
    }

    /**
     * 更新状态文字
     */
    private fun updateStatus(status: String) {
        handler.post {
            floatingContainer?.let { container ->
                val tv = container.findViewById<TextView>(R.id.map_status_text)
                tv?.text = status
            }
        }
    }

    /**
     * 处理拖动
     */
    private fun handleDrag(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params?.x ?: 0
                initialY = params?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    isDragging = true
                }
                params?.x = initialX + dx.toInt()
                params?.y = initialY + dy.toInt()
                windowManager?.updateViewLayout(floatingContainer, params)
            }
        }
    }

    /**
     * 调整窗口大小
     */
    private fun resizeWindow(scale: Float) {
        val density = resources.displayMetrics.density
        val newWidthDp = (currentWidthDp * scale).toInt().coerceIn(MIN_WIDTH_DP, MAX_WIDTH_DP)
        val newHeightDp = (currentHeightDp * scale).toInt().coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)

        currentWidthDp = newWidthDp
        currentHeightDp = newHeightDp

        params?.width = (newWidthDp * density).toInt()
        params?.height = (newHeightDp * density).toInt()
        windowManager?.updateViewLayout(floatingContainer, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingContainer?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        floatingContainer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
