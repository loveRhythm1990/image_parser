package com.example.floatingscreenshot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "FloatingWindowChannel"
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("FloatingWindow", "Service onCreate 被调用")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        return START_STICKY
    }

    private fun createFloatingWindow() {
        try {
            // 加载悬浮窗布局
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)

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
            screenshotButton?.setOnClickListener {
                android.util.Log.d("FloatingWindow", "悬浮按钮被点击")
                android.widget.Toast.makeText(this, "准备截屏...", android.widget.Toast.LENGTH_SHORT).show()
                // 启动截屏权限Activity
                val intent = Intent(this, ScreenshotPermissionActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            // 设置拖动功能
            setupDragListener(floatingView!!, params)
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindow", "创建悬浮窗失败: ${e.message}", e)
            android.widget.Toast.makeText(this, "创建悬浮窗失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun setupDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 判断是点击还是拖动
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    if (diffX < 10 && diffY < 10) {
                        // 视为点击
                        v.performClick()
                    }
                    false
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

