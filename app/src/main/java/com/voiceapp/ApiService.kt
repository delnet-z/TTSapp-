package com.voiceapp

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * GLM API 响应模型
 */
data class GlmResponse(
    val choices: List<Choice>
) {
    data class Choice(
        val message: Message
    ) {
        data class Message(
            val content: String
        )
    }
}

/**
 * GLM大模型API调用服务
 */
class ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val repo = VoiceContentRepo()
    private val apiKey = "a17f2480fd1647478642cca342059778.LjWAqCmSxYFLyCIl"

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val sysPrompt = repo.getSysPrompt()
            .replace("{name}", repo.getNickname())
        val temperature = repo.getTemperature()

        val requestBody = mapOf(
            "model" to repo.getModelName(),
            "messages" to listOf(
                mapOf("role" to "system", "content" to sysPrompt),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 300,
            "temperature" to temperature
        )

        val jsonBody = gson.toJson(requestBody)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://open.bigmodel.cn/api/paas/v4/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("GLM API 返回空响应")

        if (!response.isSuccessful) {
            throw Exception("GLM API 错误 ${response.code}: $responseBody")
        }

        val glmResponse = gson.fromJson(responseBody, GlmResponse::class.java)
        glmResponse.choices.firstOrNull()?.message?.content
            ?: throw Exception("GLM API 返回内容为空")
    }
}
