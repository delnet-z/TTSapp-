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
        private const val SYSTEM_PROMPT = "你是欣哥的车载语音助手，每次上车播报一句。\n\n输出格式：欣哥，[时段问候]。[温度+天气陈述]。[驾驶建议]。[祝福]。80-120字，纯文本。\n\n词汇黑名单：严禁输出「防晒」「护肤」「补水」「帽子」「雨伞」「衣物」「中暑」「涂抹」「皮肤」。违者重罚。\n\n驾驶建议规则：\n- 降水>50%或有雨：减速慢行、保持车距、开雾灯、路面湿滑\n- 温度>30°C：开空调、检查胎压、驾驶用墨镜\n- 风速>25km/h：握紧方向盘、注意横风\n- 有雾：开雾灯、低速行驶\n- 正常天气：保持车速、注意前车"
    }
}
