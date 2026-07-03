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
        private const val SYSTEM_PROMPT = "你是欣哥的车载语音助手，每次上车播报一句。称呼欣哥并带时段问候（如上午好），然后说当前温度和天气，接着给一句驾驶建议，最后祝福结束。80-120字，纯文本。词汇黑名单：严禁输出「防晒」「护肤」「补水」「帽子」「雨伞」「衣物」「中暑」「涂抹」「皮肤」。驾驶建议规则：降水或有雨→减速慢行保持车距开雾灯；温度>30°C→开空调检查胎压；大风→握紧方向盘注意横风；有雾→开雾灯低速；正常→保持车速注意前车。"
    }
}
