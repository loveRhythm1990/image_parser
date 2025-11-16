package com.example.floatingscreenshot

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var screenshotObserver: ScreenshotObserver? = null
    private var screenshotPanelView: View? = null
    private var screenshotPanelParams: WindowManager.LayoutParams? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val countdownTimers = mutableMapOf<Int, CountDownTimer>()
    private var panelPositionX: Int = 0
    private var panelPositionY: Int = 0
    private var panelPositionInitialized = false
    
    // 英雄数据管理器和识别服务
    private lateinit var heroDataManager: HeroDataManager
    private val heroRecognitionService = HeroRecognitionService()
    
    // 当前识别到的英雄名字列表
    private val currentHeroes = mutableListOf("英雄1", "英雄2", "英雄3", "英雄4", "英雄5")
    
    // 技能列表
    private val skillsList = listOf(
        "闪现", "惩戒", "终结", "狂暴", "疾跑", 
        "治疗", "干扰", "晕眩", "净化", "弱化", "传送"
    )
    
    // 记录每个英雄当前选择的技能（默认为闪现）
    private val selectedSkills = mutableMapOf(
        R.id.hero1SkillButton to "闪现",
        R.id.hero2SkillButton to "闪现",
        R.id.hero3SkillButton to "闪现",
        R.id.hero4SkillButton to "闪现",
        R.id.hero5SkillButton to "闪现"
    )

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "FloatingWindowChannel"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FloatingWindow", "Service onCreate 被调用")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        heroDataManager = HeroDataManager(this)
        createNotificationChannel()
        
        // 启动前台服务，指定 MEDIA_PROJECTION 类型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 需要指定前台服务类型
            android.util.Log.d("FloatingWindow", "使用 MEDIA_PROJECTION 类型启动前台服务")
            startForeground(
                NOTIFICATION_ID, 
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            android.util.Log.d("FloatingWindow", "Android 10 以下，使用普通前台服务")
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("FloatingWindow", "Service onStartCommand 被调用")
        if (floatingView == null) {
            createFloatingWindow()
        } else {
            android.util.Log.d("FloatingWindow", "悬浮窗已存在")
        }

        startScreenshotObserver()
        return START_STICKY
    }

    private fun createFloatingWindow() {
        try {
            // 加载悬浮窗布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
            floatingView?.alpha = 0.5f

            // 设置窗口参数
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.START
            params.x = 100
            params.y = 100

            // 添加到窗口管理器
            windowManager.addView(floatingView, params)
            android.util.Log.d("FloatingWindow", "悬浮窗已添加到屏幕")
            
            // 显示提示
            android.widget.Toast.makeText(this, "悬浮按钮已显示", android.widget.Toast.LENGTH_SHORT).show()

            // 设置点击事件
            val screenshotButton = floatingView?.findViewById<ImageView>(R.id.screenshotButton)
            // 点击操作留空，保留拖动逻辑

            if (screenshotButton != null) {
                // 设置拖动功能
                setupDragListener(floatingView!!, screenshotButton, params)
            } else {
                android.util.Log.e("FloatingWindow", "未找到悬浮按钮，无法启用拖动")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindow", "创建悬浮窗失败: ${e.message}", e)
            android.widget.Toast.makeText(this, "创建悬浮窗失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun setupDragListener(rootView: View, dragView: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(rootView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        dragView.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮窗服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮窗运行"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮截屏")
            .setContentText("悬浮窗正在运行")
            .setSmallIcon(R.drawable.ic_screenshot)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }

        stopScreenshotObserver()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScreenshotObserver() {
        if (!hasMediaReadPermission()) {
            android.util.Log.w("FloatingWindow", "缺少读取媒体权限，无法监听系统截图")
            return
        }

        if (screenshotObserver == null) {
            screenshotObserver = ScreenshotObserver(this, onScreenshotDetected = { screenshotPath ->
                android.util.Log.d("FloatingWindow", "检测到系统截图: $screenshotPath")
                
                // 上传截图到服务器进行英雄识别
                uploadScreenshotForRecognition(screenshotPath)
            })
        }

        screenshotObserver?.start()
        android.util.Log.d("FloatingWindow", "已开始监听系统截图目录")
    }

    private fun stopScreenshotObserver() {
        screenshotObserver?.stop()
        screenshotObserver = null
        removeScreenshotPanel()
        android.util.Log.d("FloatingWindow", "已停止监听系统截图目录")
    }
    
    /**
     * 上传截图到服务器进行英雄识别
     */
    private fun uploadScreenshotForRecognition(screenshotPath: String) {
        android.util.Log.d("FloatingWindow", "========== 开始上传截图进行识别 ==========")
        android.util.Log.d("FloatingWindow", "截图路径: $screenshotPath")
        
        // 检查文件是否存在
        val file = java.io.File(screenshotPath)
        android.util.Log.d("FloatingWindow", "文件是否存在: ${file.exists()}")
        if (file.exists()) {
            android.util.Log.d("FloatingWindow", "文件大小: ${file.length()} bytes")
            Toast.makeText(this, "正在识别英雄...\n文件大小: ${file.length()/1024}KB", Toast.LENGTH_LONG).show()
        } else {
            android.util.Log.e("FloatingWindow", "文件不存在！")
            Toast.makeText(this, "❌ 截图文件不存在", Toast.LENGTH_LONG).show()
            return
        }
        
        heroRecognitionService.recognizeHeroes(screenshotPath, object : HeroRecognitionService.RecognitionCallback {
            override fun onSuccess(response: HeroRecognitionResponse) {
                android.util.Log.d("FloatingWindow", "识别成功: ${response.heroes}")
                mainHandler.post {
                    Toast.makeText(
                        this@FloatingWindowService, 
                        "识别成功: ${response.heroes.joinToString(", ")}", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 更新英雄列表
                    updateHeroes(response.heroes)
                    
                    // 显示面板
                    showScreenshotPanel()
                }
            }
            
            override fun onFailure(error: String) {
                android.util.Log.e("FloatingWindow", "识别失败: $error")
                mainHandler.post {
                    // 显示详细的错误信息
                    val errorMsg = when {
                        error.contains("Unable to resolve host") -> 
                            "❌ 网络错误：无法连接服务器\n请检查网络连接"
                        error.contains("timeout") -> 
                            "❌ 网络超时\n服务器响应时间过长"
                        error.contains("文件不存在") -> 
                            "❌ 截图文件不存在\n请检查存储权限"
                        error.contains("403") || error.contains("401") -> 
                            "❌ 服务器拒绝访问\nAPI密钥可能有误"
                        error.contains("500") || error.contains("502") || error.contains("503") -> 
                            "❌ 服务器错误\n服务器暂时不可用"
                        else -> "❌ 识别失败\n$error"
                    }
                    
                    Toast.makeText(
                        this@FloatingWindowService, 
                        errorMsg, 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // 即使识别失败，也显示面板（使用默认英雄名字）
                    showScreenshotPanel()
                }
            }
        })
    }
    
    /**
     * 更新英雄列表
     */
    private fun updateHeroes(heroes: List<String>) {
        currentHeroes.clear()
        
        // 最多添加5个英雄
        for (i in 0 until 5) {
            if (i < heroes.size) {
                val heroName = heroes[i]
                // 总是使用识别到的英雄名字，即使在数据库中找不到
                currentHeroes.add(heroName)
                
                if (heroDataManager.hasHero(heroName)) {
                    android.util.Log.d("FloatingWindow", "英雄 ${i+1}: $heroName ✅ (在数据库中找到)")
                } else {
                    android.util.Log.w("FloatingWindow", "英雄 ${i+1}: $heroName ⚠️ (未在数据库中找到，将使用随机 CD)")
                }
            } else {
                // 如果服务器返回的英雄少于5个，使用默认名字
                currentHeroes.add("英雄${i+1}")
                android.util.Log.d("FloatingWindow", "英雄 ${i+1}: 使用默认名字")
            }
        }
        
        android.util.Log.d("FloatingWindow", "✅ 更新后的英雄列表: $currentHeroes")
    }

    private fun showScreenshotPanel() {
        mainHandler.post {
            if (screenshotPanelView == null) {
                screenshotPanelView = LayoutInflater.from(this).inflate(R.layout.screenshot_panel, null).apply {
                    alpha = 0.8f
                }

                screenshotPanelParams = createPanelLayoutParams()

                try {
                    windowManager.addView(screenshotPanelView, screenshotPanelParams)
                    android.util.Log.d("FloatingWindow", "已显示截图提示面板")
                    
                    // 强制测量布局，确保所有子视图都被正确测量
                    screenshotPanelView?.post {
                        val width = screenshotPanelView?.width ?: 0
                        val height = screenshotPanelView?.height ?: 0
                        android.util.Log.d("FloatingWindow", "面板实际尺寸: ${width}x${height}")
                        
                        // 检查英雄5按钮是否存在
                        val hero5Container = screenshotPanelView?.findViewById<View>(R.id.hero5Container)
                        val hero5Visible = hero5Container?.visibility == View.VISIBLE
                        val hero5Width = hero5Container?.width ?: 0
                        android.util.Log.d("FloatingWindow", "英雄5容器: 可见=$hero5Visible, 宽度=$hero5Width")
                    }
                    
                    // 更新英雄名字显示
                    updateHeroNamesDisplay()
                    
                    // 设置拖动功能
                    setupPanelDragListener()
                    setupSkillButtonClickListeners()
                    
                    // 初始化倒计时为 0（不自动启动）
                    initializeCountdownsToZero()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindow", "显示截图提示面板失败: ${e.message}", e)
                    screenshotPanelView = null
                    screenshotPanelParams = null
                }
            } else {
                try {
                    cancelCountdowns()
                    screenshotPanelParams = createPanelLayoutParams()
                    windowManager.updateViewLayout(screenshotPanelView, screenshotPanelParams)
                    screenshotPanelView?.visibility = View.VISIBLE
                    
                    // 更新英雄名字显示
                    updateHeroNamesDisplay()
                    
                    // 重新设置拖动功能
                    setupPanelDragListener()
                    setupSkillButtonClickListeners()
                    
                    // 初始化倒计时为 0（不自动启动）
                    initializeCountdownsToZero()
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindow", "更新截图提示面板失败: ${e.message}", e)
                }
            }
        }
    }

    private fun setupPanelDragListener() {
        val panel = screenshotPanelView ?: return
        val params = screenshotPanelParams ?: return

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        panel.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    val screenWidth = resources.displayMetrics.widthPixels
                    val screenHeight = resources.displayMetrics.heightPixels
                    val panelWidth = panel.width.coerceAtLeast(1)
                    val panelHeight = panel.height.coerceAtLeast(1)

                    params.x = (initialX + deltaX).coerceIn(0, screenWidth - panelWidth)
                    params.y = (initialY + deltaY).coerceIn(0, screenHeight - panelHeight)
                    try {
                        windowManager.updateViewLayout(panel, params)
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindow", "更新面板位置失败: ${e.message}")
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    // 如果移动距离很小，视为点击而非拖动
                    if (diffX < 10 && diffY < 10) {
                        panel.performClick()
                    }
                    panelPositionX = params.x
                    panelPositionY = params.y
                    true
                }
                else -> false
            }
        }
    }

    private fun createPanelLayoutParams(): WindowManager.LayoutParams {
        ensurePanelPositionInitialized()

        // 移除 FLAG_NOT_TOUCHABLE，让面板可以响应触摸事件
        // 添加 FLAG_LAYOUT_NO_LIMITS 允许面板内容完整显示
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = panelPositionX
            y = panelPositionY
        }
    }

    private fun ensurePanelPositionInitialized() {
        if (panelPositionInitialized) return

        val metrics = resources.displayMetrics
        val defaultMargin = dpToPx(8)
        // 竖屏：靠右显示，横屏：靠上显示
        if (metrics.widthPixels < metrics.heightPixels) {
            // 竖屏模式 - 面板在右侧，从上往下约1/3处
            panelPositionX = defaultMargin
            panelPositionY = metrics.heightPixels / 3
        } else {
            // 横屏模式 - 面板在顶部，从左往右约1/4处
            panelPositionX = metrics.widthPixels / 4
            panelPositionY = defaultMargin
        }
        panelPositionInitialized = true
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun removeScreenshotPanel() {
        mainHandler.post {
            if (screenshotPanelView != null) {
                try {
                    windowManager.removeView(screenshotPanelView)
                    android.util.Log.d("FloatingWindow", "已移除截图提示面板")
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindow", "移除截图提示面板失败: ${e.message}", e)
                } finally {
                    cancelCountdowns()
                    screenshotPanelView = null
                    screenshotPanelParams = null
                }
            }
        }
    }

    /**
     * 更新英雄名字显示
     */
    private fun updateHeroNamesDisplay() {
        android.util.Log.d("FloatingWindow", "========== 开始更新英雄名字显示 ==========")
        android.util.Log.d("FloatingWindow", "当前英雄列表: $currentHeroes")
        android.util.Log.d("FloatingWindow", "面板视图是否为空: ${screenshotPanelView == null}")
        
        if (screenshotPanelView == null) {
            android.util.Log.e("FloatingWindow", "面板视图为空，无法更新英雄名字")
            return
        }
        
        val heroNameIds = listOf(
            R.id.hero1Name,
            R.id.hero2Name,
            R.id.hero3Name,
            R.id.hero4Name,
            R.id.hero5Name
        )
        
        heroNameIds.forEachIndexed { index, nameId ->
            val nameView = screenshotPanelView?.findViewById<TextView>(nameId)
            android.util.Log.d("FloatingWindow", "查找英雄${index+1}的 TextView, ID=$nameId, 找到=${nameView != null}")
            
            if (nameView == null) {
                android.util.Log.e("FloatingWindow", "未找到英雄${index+1}的 TextView (ID=$nameId)")
            } else if (index < currentHeroes.size) {
                val oldText = nameView.text.toString()
                val newText = currentHeroes[index]
                nameView.text = newText
                android.util.Log.d("FloatingWindow", "✅ 更新英雄名字 ${index+1}: \"$oldText\" -> \"$newText\"")
            }
        }
        
        android.util.Log.d("FloatingWindow", "========== 英雄名字更新完成 ==========")
    }
    
    /**
     * 初始化所有倒计时为 0（不自动启动）
     */
    private fun initializeCountdownsToZero() {
        val countdownIds = listOf(
            R.id.hero1UltimateCdButton,
            R.id.hero1SkillCdButton,
            R.id.hero2UltimateCdButton,
            R.id.hero2SkillCdButton,
            R.id.hero3UltimateCdButton,
            R.id.hero3SkillCdButton,
            R.id.hero4UltimateCdButton,
            R.id.hero4SkillCdButton,
            R.id.hero5UltimateCdButton,
            R.id.hero5SkillCdButton
        )
        
        countdownIds.forEach { id ->
            val countdownView = screenshotPanelView?.findViewById<TextView>(id)
            countdownView?.text = "0"
        }
        
        android.util.Log.d("FloatingWindow", "已初始化所有倒计时为 0")
    }

    private fun setupSkillButtonClickListeners() {
        // 大招按钮配置
        val ultimateButtons = listOf(
            Pair(R.id.hero1UltimateButton, R.id.hero1UltimateCdButton),
            Pair(R.id.hero2UltimateButton, R.id.hero2UltimateCdButton),
            Pair(R.id.hero3UltimateButton, R.id.hero3UltimateCdButton),
            Pair(R.id.hero4UltimateButton, R.id.hero4UltimateCdButton),
            Pair(R.id.hero5UltimateButton, R.id.hero5UltimateCdButton)
        )
        
        // 技能按钮配置（用于选择技能）
        val skillButtons = listOf(
            Pair(R.id.hero1SkillButton, R.id.hero1SkillCdButton),
            Pair(R.id.hero2SkillButton, R.id.hero2SkillCdButton),
            Pair(R.id.hero3SkillButton, R.id.hero3SkillCdButton),
            Pair(R.id.hero4SkillButton, R.id.hero4SkillCdButton),
            Pair(R.id.hero5SkillButton, R.id.hero5SkillCdButton)
        )

        // 为大招按钮设置点击事件（点击启动倒计时）
        ultimateButtons.forEachIndexed { index, (ultimateButtonId, cdButtonId) ->
            val ultimateButton = screenshotPanelView?.findViewById<TextView>(ultimateButtonId)
            ultimateButton?.setOnClickListener {
                android.util.Log.d("FloatingWindow", "大招按钮被点击: $ultimateButtonId")
                startUltimateCountdown(index, cdButtonId)
            }
        }
        
        // 为技能按钮设置点击事件（点击启动倒计时，长按选择技能）
        skillButtons.forEach { (skillButtonId, cdButtonId) ->
            val skillButton = screenshotPanelView?.findViewById<TextView>(skillButtonId)
            
            // 设置初始显示的技能名称
            val currentSkill = selectedSkills[skillButtonId] ?: "闪现"
            skillButton?.text = currentSkill
            
            // 点击启动倒计时
            skillButton?.setOnClickListener {
                android.util.Log.d("FloatingWindow", "技能按钮被点击: $skillButtonId，启动倒计时")
                startSkillCountdown(skillButtonId, cdButtonId)
            }
            
            // 长按显示技能选择对话框
            skillButton?.setOnLongClickListener {
                android.util.Log.d("FloatingWindow", "技能按钮长按: $skillButtonId，显示技能选择")
                showSkillSelectionDialog(skillButtonId, cdButtonId)
                true
            }
        }
    }
    
    private fun showSkillSelectionDialog(skillButtonId: Int, cdButtonId: Int) {
        val skillButton = screenshotPanelView?.findViewById<TextView>(skillButtonId) ?: return
        val currentSkill = selectedSkills[skillButtonId] ?: "闪现"
        val currentIndex = skillsList.indexOf(currentSkill).coerceAtLeast(0)
        
        try {
            // 创建一个悬浮窗形式的技能选择面板
            showSkillSelectionPanel(skillButtonId, cdButtonId, currentIndex)
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindow", "显示技能选择对话框失败: ${e.message}", e)
            // 降级方案：循环切换技能
            val nextIndex = (currentIndex + 1) % skillsList.size
            val selectedSkill = skillsList[nextIndex]
            selectedSkills[skillButtonId] = selectedSkill
            skillButton.text = selectedSkill
        }
    }
    
    private fun showSkillSelectionPanel(skillButtonId: Int, cdButtonId: Int, currentIndex: Int) {
        val skillButton = screenshotPanelView?.findViewById<TextView>(skillButtonId) ?: return
        
        // 创建根容器 LinearLayout
        val rootLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        
        // 添加标题
        val titleView = TextView(this).apply {
            text = "选择技能"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 0, 0, dpToPx(8))
            gravity = Gravity.CENTER
        }
        rootLayout.addView(titleView)
        
        // 创建可滚动的技能列表容器
        val scrollView = android.widget.ScrollView(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(300) // 限制最大高度为 300dp
            )
        }
        
        // 创建技能列表 LinearLayout
        val skillsLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }
        
        // 添加技能选项
        skillsList.forEachIndexed { index, skill ->
            val skillItemView = TextView(this).apply {
                text = skill
                textSize = 14f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
                gravity = Gravity.CENTER
                
                // 当前选中的技能高亮显示
                if (index == currentIndex) {
                    setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    setBackgroundColor(android.graphics.Color.parseColor("#424242"))
                }
                
                // 设置点击事件
                setOnClickListener {
                    android.util.Log.d("FloatingWindow", "选择了技能: $skill")
                    
                    // 更新选中的技能
                    selectedSkills[skillButtonId] = skill
                    
                    // 更新按钮显示
                    skillButton.text = skill
                    
                    // 不自动启动倒计时，用户需要点击按钮才启动
                    
                    // 移除选择面板
                    try {
                        windowManager.removeView(rootLayout)
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingWindow", "移除选择面板失败: ${e.message}")
                    }
                }
            }
            
            // 添加间距
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) {
                params.topMargin = dpToPx(4)
            }
            skillItemView.layoutParams = params
            
            skillsLayout.addView(skillItemView)
        }
        
        // 将技能列表添加到 ScrollView
        scrollView.addView(skillsLayout)
        
        // 将 ScrollView 添加到根容器
        rootLayout.addView(scrollView)
        
        // 添加取消按钮
        val cancelButton = TextView(this).apply {
            text = "取消"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
            setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
            gravity = Gravity.CENTER
            
            setOnClickListener {
                try {
                    windowManager.removeView(rootLayout)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindow", "移除选择面板失败: ${e.message}")
                }
            }
        }
        
        val cancelParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cancelParams.topMargin = dpToPx(12)
        cancelButton.layoutParams = cancelParams
        rootLayout.addView(cancelButton)
        
        // 创建悬浮窗参数
        val params = WindowManager.LayoutParams(
            dpToPx(200),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        // 点击外部关闭面板
        rootLayout.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                try {
                    windowManager.removeView(rootLayout)
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWindow", "移除选择面板失败: ${e.message}")
                }
                true
            } else {
                false
            }
        }
        
        // 添加到窗口管理器
        try {
            windowManager.addView(rootLayout, params)
            android.util.Log.d("FloatingWindow", "技能选择面板已显示")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindow", "添加选择面板失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 启动大招倒计时
     * @param heroIndex 英雄索引 (0-4)
     * @param cdButtonId 倒计时按钮 ID
     */
    private fun startUltimateCountdown(heroIndex: Int, cdButtonId: Int) {
        if (heroIndex < 0 || heroIndex >= currentHeroes.size) {
            android.util.Log.w("FloatingWindow", "无效的英雄索引: $heroIndex")
            return
        }
        
        val heroName = currentHeroes[heroIndex]
        val cdSeconds = heroDataManager.getHeroUltimateCd(heroName)
        
        android.util.Log.d("FloatingWindow", "🚀 启动 $heroName 的大招倒计时: ${cdSeconds}秒")
        
        startCountdownWithDuration(cdButtonId, cdSeconds)
    }
    
    /**
     * 启动技能倒计时
     * @param skillButtonId 技能按钮 ID
     * @param cdButtonId 倒计时按钮 ID
     */
    private fun startSkillCountdown(skillButtonId: Int, cdButtonId: Int) {
        val skillName = selectedSkills[skillButtonId] ?: "闪现"
        val cdSeconds = heroDataManager.getCommonSkillCd(skillName)
        
        android.util.Log.d("FloatingWindow", "⚡ 启动技能 $skillName 的倒计时: ${cdSeconds}秒")
        
        if (cdSeconds <= 0) {
            android.util.Log.w("FloatingWindow", "技能 $skillName 的 CD 为 0，不启动倒计时")
            return
        }
        
        startCountdownWithDuration(cdButtonId, cdSeconds)
    }
    
    /**
     * 启动指定时长的倒计时
     * @param cdButtonId 倒计时按钮 ID
     * @param durationSeconds 倒计时时长（秒）
     */
    private fun startCountdownWithDuration(cdButtonId: Int, durationSeconds: Int) {
        // 取消该按钮的旧倒计时（如果存在）
        countdownTimers[cdButtonId]?.cancel()

        val countdownView = screenshotPanelView?.findViewById<TextView>(cdButtonId) ?: return
        countdownView.text = durationSeconds.toString()

        val timer = object : CountDownTimer(durationSeconds * 1000L, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownView.text = ((millisUntilFinished / 1000L).toInt()).toString()
            }

            override fun onFinish() {
                countdownView.text = "0"
                countdownTimers.remove(cdButtonId)
            }
        }

        countdownTimers[cdButtonId] = timer
        timer.start()
    }

    private fun cancelCountdowns() {
        countdownTimers.values.forEach { it.cancel() }
        countdownTimers.clear()
    }

    private fun hasMediaReadPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

