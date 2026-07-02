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
    private val apiKey = "a17f2480fd1647478642cca342059778.LjWAqCmSxYFLyCIl"

    suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val requestBody = mapOf(
            "model" to "glm-4-flash",
            "messages" to listOf(
                mapOf("role" to "system", "content" to SYSTEM_PROMPT),
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to 300,
            "temperature" to 0.7
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

    companion object {
        private const val SYSTEM_PROMPT = "你是一个幽默俏皮的车载语音助手，负责在车主上车时生成一段风趣的迎宾播报。要求：1)语气轻松俏皮，可以玩梗、调侃，像朋友聊天一样 2)必须包含时间问候+天气简述+一句有趣的行车祝福 3)控制在50-90字。风格参考：「早啊老司机！今儿25度小风吹着，湿度刚好，是飙车…哦不是，是安全驾驶的好天气。系好安全带，咱们出发~」"
    }
}
