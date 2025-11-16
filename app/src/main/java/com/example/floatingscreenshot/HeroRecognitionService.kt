package com.example.floatingscreenshot

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * 英雄识别响应数据类
 */
data class HeroRecognitionResponse(
    val heroes: List<String>,
    val success: Boolean,
    val message: String
)

/**
 * 英雄识别服务类
 * 负责调用 FastAPI 服务识别游戏截图中的英雄
 */
class HeroRecognitionService {
    
    companion object {
        private const val TAG = "HeroRecognitionService"
        private const val API_URL = "http://shanghai.idc.matrixorigin.cn:30026/api/v1/recognize-heroes"
        
        // OkHttp 客户端，不设置超时时间
        private val client = OkHttpClient.Builder()
            .connectTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * 上传图片到服务器进行英雄识别
     * @param imagePath 图片文件路径
     * @param callback 回调接口
     */
    fun recognizeHeroes(imagePath: String, callback: RecognitionCallback) {
        Log.d(TAG, "开始识别英雄，图片路径: $imagePath")
        
        val file = File(imagePath)
        if (!file.exists()) {
            Log.e(TAG, "图片文件不存在: $imagePath")
            callback.onFailure("图片文件不存在")
            return
        }
        
        // 构建 multipart 请求
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .build()
        
        val request = Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()
        
        // 异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "网络请求失败: ${e.message}", e)
                callback.onFailure("网络请求失败: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d(TAG, "收到服务器响应，状态码: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "无错误详情"
                        Log.e(TAG, "服务器返回错误: ${response.code}, 错误详情: $errorBody")
                        callback.onFailure("服务器返回错误: ${response.code}, $errorBody")
                        return
                    }
                    
                    val responseBody = response.body?.string()
                    if (responseBody == null) {
                        Log.e(TAG, "响应体为空")
                        callback.onFailure("响应体为空")
                        return
                    }
                    
                    Log.d(TAG, "服务器响应内容: $responseBody")
                    
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val heroesArray = jsonObject.getJSONArray("heroes")
                        val heroes = mutableListOf<String>()
                        
                        for (i in 0 until heroesArray.length()) {
                            heroes.add(heroesArray.getString(i))
                        }
                        
                        val success = jsonObject.getBoolean("success")
                        val message = jsonObject.optString("message", "")
                        
                        val result = HeroRecognitionResponse(heroes, success, message)
                        Log.d(TAG, "识别成功，英雄列表: $heroes")
                        callback.onSuccess(result)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "解析响应失败: ${e.message}", e)
                        callback.onFailure("解析响应失败: ${e.message}")
                    }
                }
            }
        })
    }
    
    /**
     * 识别结果回调接口
     */
    interface RecognitionCallback {
        fun onSuccess(response: HeroRecognitionResponse)
        fun onFailure(error: String)
    }
}

