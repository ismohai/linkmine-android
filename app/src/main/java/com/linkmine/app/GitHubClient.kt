package com.linkmine.app

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GitHubClient {

    private const val REPO = "ismohai/linkmine-android"
    private const val FILE_PATH = "message.txt"
    private const val API_BASE = "https://api.github.com"
    private var token = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun init(githubToken: String) {
        token = githubToken
    }

    fun send(message: String): Boolean {
        return try {
            // 1. GET 获取当前文件的 SHA
            val sha = getFileSha() ?: return false

            // 2. PUT 更新文件内容
            val contentBase64 = Base64.encodeToString(
                message.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val body = JSONObject().apply {
                put("message", "msg")
                put("content", contentBase64)
                put("sha", sha)
            }

            val request = Request.Builder()
                .url("$API_BASE/repos/$REPO/contents/$FILE_PATH")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .put(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getFileSha(): String? {
        return try {
            val request = Request.Builder()
                .url("$API_BASE/repos/$REPO/contents/$FILE_PATH")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: return null)
                json.getString("sha")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
