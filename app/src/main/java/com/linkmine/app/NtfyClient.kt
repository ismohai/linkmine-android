package com.linkmine.app

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object NtfyClient {
    
    // 你的专属频道名，可以修改为任意字符串
    private const val TOPIC = "linkmine-p8k2m5x9"
    private const val BASE_URL = "https://ntfy.sh"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    fun send(message: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$BASE_URL/$TOPIC")
                .post(message.toRequestBody("text/plain".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
