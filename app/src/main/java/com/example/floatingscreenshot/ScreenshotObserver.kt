package com.example.floatingscreenshot

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import java.util.Locale

/**
 * 监听系统截屏目录 /DCIM/Screenshots 是否有新截图，并弹窗提示文件名
 */
class ScreenshotObserver(
    private val context: Context,
    private val onScreenshotDetected: (String) -> Unit,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private val contentResolver: ContentResolver = context.contentResolver
    private var isObserving = false
    private var lastImageId: Long = -1L
    private var lastDateAdded: Long = -1L

    private val logTag = "ScreenshotObserver"

    fun start() {
        if (isObserving) return

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            this
        )
        isObserving = true

        Log.d(logTag, "开始监听系统截图目录")
        initLatestScreenshot()
    }

    fun stop() {
        if (!isObserving) return

        contentResolver.unregisterContentObserver(this)
        isObserving = false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        checkLatestScreenshot()
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        checkLatestScreenshot()
    }

    private fun initLatestScreenshot() {
        queryLatestScreenshot()?.let { result ->
            lastImageId = result.id
            lastDateAdded = result.dateAdded
        }
    }

    private fun checkLatestScreenshot() {
        val result = queryLatestScreenshot()
        if (result == null) {
            Log.d(logTag, "未查询到最新图片")
            return
        }

        Log.d(
            logTag,
            "检测到媒体库更新: id=${result.id}, name=${result.displayName}, " +
                    "bucket=${result.bucketDisplayName}, path=${result.relativePath}, data=${result.dataPath}"
        )

        val isNewShot = when {
            result.id > 0 && result.id != lastImageId -> true
            result.dateAdded > 0 && result.dateAdded != lastDateAdded -> true
            else -> false
        }

        if (isNewShot && isScreenshotBucket(result)) {
            lastImageId = result.id
            lastDateAdded = result.dateAdded

            Handler(Looper.getMainLooper()).post {
                // 获取完整的文件路径
                val filePath = getFullPathFromResult(result)
                onScreenshotDetected(filePath)
            }
        } else {
            if (!isNewShot) {
                Log.d(logTag, "更新的图片不是新截图（可能是重复事件）")
            } else {
                Log.d(logTag, "更新的图片路径不在截图目录，忽略")
            }
        }
    }

    private fun queryLatestScreenshot(): MediaStoreImage? {
        val projectionList = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Images.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projectionList.add(MediaStore.Images.Media.DATA)
        }

        val projection = projectionList.toTypedArray()

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val displayName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "Screenshot"
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val bucketName = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)) ?: ""
                val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)) ?: ""
                } else {
                    ""
                }
                val dataPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ""
                } else {
                    @Suppress("DEPRECATION")
                    it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) ?: ""
                }
                return MediaStoreImage(id, displayName, dateAdded, bucketName, relativePath, dataPath)
            }
        }

        return null
    }

    /**
     * 从 MediaStoreImage 获取完整的文件路径
     */
    private fun getFullPathFromResult(image: MediaStoreImage): String {
        // Android 10+ 使用 ContentUri 方式访问
        // 对于低版本，直接使用 DATA 列的路径
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 需要通过 ContentResolver 访问文件
            // 这里返回 Uri 路径，稍后需要转换为实际文件路径
            // 为了简化，我们可以尝试从 MediaStore 复制文件到缓存目录
            val uri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                image.id.toString()
            )
            
            // 尝试获取实际文件路径
            getRealPathFromUri(uri) ?: uri.toString()
        } else {
            // Android 9 及以下可以直接使用 DATA 列
            image.dataPath
        }
    }
    
    /**
     * 从 Uri 获取真实文件路径（Android 10+）
     */
    private fun getRealPathFromUri(uri: Uri): String? {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Images.Media.DISPLAY_NAME)
        } else {
            @Suppress("DEPRECATION")
            arrayOf(MediaStore.Images.Media.DATA)
        }
        
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ 需要通过 ContentResolver 读取文件内容
                    // 这里我们将文件复制到缓存目录
                    val displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    )
                    return copyUriToCache(uri, displayName)
                } else {
                    @Suppress("DEPRECATION")
                    return cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * 将 Uri 指向的文件复制到缓存目录（Android 10+）
     */
    private fun copyUriToCache(uri: Uri, displayName: String): String? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val cacheFile = java.io.File(context.cacheDir, displayName)
            
            inputStream.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(logTag, "已将截图复制到缓存: ${cacheFile.absolutePath}")
            return cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e(logTag, "复制截图到缓存失败: ${e.message}", e)
            return null
        }
    }
    
    private fun isScreenshotBucket(image: MediaStoreImage): Boolean {
        val bucketLower = image.bucketDisplayName.lowercase(Locale.getDefault())
        val relativeLower = image.relativePath.lowercase(Locale.getDefault())
        val dataLower = image.dataPath.lowercase(Locale.getDefault())

        val keywords = listOf(
            "screenshot",
            "screenshots",
            "screen_shot",
            "screen-shot",
            "screen capture",
            "screen_cap",
            "screen-record",
            "截屏",
            "屏幕截图",
            "螢幕截圖"
        )

        val matched = keywords.any { keyword ->
            bucketLower.contains(keyword) ||
                    relativeLower.contains(keyword) ||
                    dataLower.contains(keyword)
        }

        if (!matched) {
            Log.d(
                logTag,
                "图片未匹配截图目录: bucket='${image.bucketDisplayName}', relative='${image.relativePath}', data='${image.dataPath}'"
            )
        }

        return matched
    }

    private data class MediaStoreImage(
        val id: Long,
        val displayName: String,
        val dateAdded: Long,
        val bucketDisplayName: String,
        val relativePath: String,
        val dataPath: String
    )
}

