package com.voiceapp.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * GLM API 响应数据类
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
 * 后台配置API响应
 */
data class ConfigResponse(
    val success: Boolean,
    val data: KeywordConfig? = null,
    val message: String = ""
)

/**
 * API 服务层
 * - 调用智谱 GLM-4-Flash 免费API生成语音文本
 * - 从后台拉取关键词配置
 */
class ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        // GLM-4-Flash API 地址
        private const val GLM_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        // 默认后台配置接口（用户可在设置中修改）
        const val DEFAULT_CONFIG_URL = "https://your-server.com/api/voice-config"
    }

    /**
     * 调用GLM-4-Flash根据关键词生成语音文本
     */
    suspend fun generateVoiceText(
        apiKey: String,
        keywords: List<String>,
        promptTemplate: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = promptTemplate.replace("{keywords}", keywords.joinToString("、"))

            val requestBody = mapOf(
                "model" to "glm-4-flash",
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "你是一个智能语音助手，负责生成自然流畅的中文语音播报内容。"
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to prompt
                    )
                ),
                "temperature" to 0.7,
                "max_tokens" to 200
            )

            val request = Request.Builder()
                .url(GLM_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    gson.toJson(requestBody)
                ))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GLM API 错误: ${response.code} $body"))
            }

            val glmResponse = gson.fromJson(body, GlmResponse::class.java)
            val text = glmResponse.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("GLM 返回为空"))

            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从后台拉取关键词配置
     */
    suspend fun fetchConfig(configUrl: String): Result<KeywordConfig> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(configUrl)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("配置接口错误: ${response.code}"))
            }

            val configResponse = gson.fromJson(body, ConfigResponse::class.java)
            val config = configResponse.data
                ?: return@withContext Result.failure(Exception("配置数据为空"))

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
