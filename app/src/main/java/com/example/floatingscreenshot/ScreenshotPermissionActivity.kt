package com.example.floatingscreenshot

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class ScreenshotPermissionActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("ScreenshotActivity", "权限请求结果: resultCode=${result.resultCode}, data=${result.data}")
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            android.util.Log.d("ScreenshotActivity", "权限授予成功，开始截屏")
            startScreenCapture(result.resultCode, result.data!!)
        } else {
            android.util.Log.w("ScreenshotActivity", "权限被拒绝或取消")
            Toast.makeText(this, "截屏权限被拒绝", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("ScreenshotActivity", "Activity onCreate")
        
        // 设置透明背景
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        // 请求截屏权限
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        android.util.Log.d("ScreenshotActivity", "启动权限请求")
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        try {
            android.util.Log.d("ScreenshotActivity", "开始 startScreenCapture")
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            
            android.util.Log.d("ScreenshotActivity", "屏幕尺寸: ${width}x${height}, density=$density")

            // 在 Activity 中创建 MediaProjection，不需要前台服务
            android.util.Log.d("ScreenshotActivity", "创建 MediaProjection")
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            android.util.Log.d("ScreenshotActivity", "MediaProjection 创建成功")
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            android.util.Log.d("ScreenshotActivity", "ImageReader 创建成功")

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )
            android.util.Log.d("ScreenshotActivity", "VirtualDisplay 创建成功")

            // 延迟500ms后截屏，让权限窗口消失
            android.util.Log.d("ScreenshotActivity", "500ms 后开始截屏")
            handler.postDelayed({
                captureScreen()
            }, 500)
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotActivity", "startScreenCapture 失败", e)
            e.printStackTrace()
            Toast.makeText(this, "截屏失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun captureScreen() {
        try {
            android.util.Log.d("ScreenshotActivity", "captureScreen 开始")
            val image = imageReader?.acquireLatestImage()
            android.util.Log.d("ScreenshotActivity", "获取图像: ${image != null}")
            
            if (image != null) {
                android.util.Log.d("ScreenshotActivity", "图像尺寸: ${image.width}x${image.height}")
                val bitmap = imageToBitmap(image)
                image.close()
                android.util.Log.d("ScreenshotActivity", "转换为 Bitmap: ${bitmap != null}")
                
                if (bitmap != null) {
                    saveBitmap(bitmap)
                } else {
                    android.util.Log.e("ScreenshotActivity", "Bitmap 转换失败")
                }
            } else {
                android.util.Log.w("ScreenshotActivity", "未能获取图像")
                Toast.makeText(this, "未能捕获屏幕图像", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotActivity", "captureScreen 失败", e)
            e.printStackTrace()
            Toast.makeText(this, "截屏失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            android.util.Log.d("ScreenshotActivity", "清理资源并关闭 Activity")
            cleanup()
            finish()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // 如果有padding，裁剪掉
            return if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            android.util.Log.d("ScreenshotActivity", "saveBitmap 开始，尺寸: ${bitmap.width}x${bitmap.height}")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Screenshot_$timestamp.png"
            android.util.Log.d("ScreenshotActivity", "文件名: $fileName")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: 使用 MediaStore API 保存到 Downloads 目录
                android.util.Log.d("ScreenshotActivity", "使用 MediaStore API 保存到 Downloads")
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val imageUri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                if (imageUri != null) {
                    android.util.Log.d("ScreenshotActivity", "MediaStore URI 创建成功: $imageUri")
                    val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                    outputStream?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    
                    android.util.Log.d("ScreenshotActivity", "截图保存成功到 Downloads")
                    handler.post {
                        Toast.makeText(this, "截屏已保存到 Downloads: $fileName", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.e("ScreenshotActivity", "无法创建 MediaStore URI")
                    throw Exception("无法创建文件")
                }
            } else {
                // Android 9 及以下: 直接保存到 Downloads 目录
                android.util.Log.d("ScreenshotActivity", "直接保存到 Downloads 目录")
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                    android.util.Log.d("ScreenshotActivity", "创建 Downloads 目录")
                }
                
                val file = File(downloadsDir, fileName)
                android.util.Log.d("ScreenshotActivity", "文件路径: ${file.absolutePath}")
                
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                
                android.util.Log.d("ScreenshotActivity", "截图保存成功: ${file.absolutePath}")
                handler.post {
                    Toast.makeText(this, "截屏已保存到: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenshotActivity", "保存截图失败", e)
            e.printStackTrace()
            handler.post {
                Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cleanup() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}
