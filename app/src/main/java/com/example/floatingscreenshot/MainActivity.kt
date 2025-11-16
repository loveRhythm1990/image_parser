package com.example.floatingscreenshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            android.util.Log.d("MainActivity", "读取媒体权限已授予")
            startFloatingWindow()
        } else {
            android.util.Log.w("MainActivity", "读取媒体权限被拒绝")
            Toast.makeText(this, "需要读取媒体权限才能监听系统截图", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            checkPermissionsAndStart()
        }

        stopButton.setOnClickListener {
            stopFloatingWindow()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun checkPermissionsAndStart() {
        android.util.Log.d("MainActivity", "checkPermissionsAndStart 被调用")
        
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                android.util.Log.d("MainActivity", "没有悬浮窗权限，弹出对话框")
                // 请求悬浮窗权限
                AlertDialog.Builder(this)
                    .setTitle("需要悬浮窗权限")
                    .setMessage("应用需要在其他应用上显示悬浮窗的权限才能工作")
                    .setPositiveButton("前往设置") { _, _ ->
                        android.util.Log.d("MainActivity", "用户点击了前往设置")
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return
            }
        }

        android.util.Log.d("MainActivity", "已有悬浮窗权限，启动服务")
        // 检查读取媒体权限
        if (!hasStoragePermission()) {
            android.util.Log.d("MainActivity", "缺少读取媒体权限，准备请求")
            requestStoragePermissions()
            return
        }

        // 启动悬浮窗服务
        startFloatingWindow()
    }

    private fun startFloatingWindow() {
        try {
            android.util.Log.d("MainActivity", "准备启动悬浮窗服务")
            val intent = Intent(this, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
                android.util.Log.d("MainActivity", "调用了 startForegroundService")
            } else {
                startService(intent)
                android.util.Log.d("MainActivity", "调用了 startService")
            }
            Toast.makeText(this, "悬浮窗服务已启动，请查看屏幕左上方", Toast.LENGTH_LONG).show()
            updateStatus()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "启动服务失败: ${e.message}", e)
            Toast.makeText(this, "启动失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopFloatingWindow() {
        val intent = Intent(this, FloatingWindowService::class.java)
        stopService(intent)
        Toast.makeText(this, "悬浮窗已停止", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun updateStatus() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        if (hasPermission) {
            statusText.text = "✓ 悬浮窗权限已授予"
            startButton.text = "启动悬浮窗"
        } else {
            statusText.text = "✗ 需要悬浮窗权限，点击下方按钮授权"
            startButton.text = "授权并启动"
        }
        // 按钮始终可点击
        startButton.isEnabled = true
    }

    private fun hasStoragePermission(): Boolean {
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

    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        storagePermissionLauncher.launch(permissions)
    }
}

