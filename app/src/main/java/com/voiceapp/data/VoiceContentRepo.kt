package com.voiceapp.data

import com.voiceapp.VoiceApp
import com.voiceapp.data.ApiService.Companion.DEFAULT_CONFIG_URL

/**
 * 语音内容仓库 - 管理API Key和配置URL的本地存储
 */
object VoiceContentRepo {
    private val prefs = VoiceApp.instance.getSharedPreferences("voice_prefs", 0)

    private const val KEY_GLM_API_KEY = "glm_api_key"
    private const val KEY_CONFIG_URL = "config_url"
    private const val KEY_LAST_KEYWORDS = "last_keywords"

    fun getGlmApiKey(): String = prefs.getString(KEY_GLM_API_KEY, "a17f2480fd1647478642cca342059778.LjWAqCmSxYFLyCIl") ?: "a17f2480fd1647478642cca342059778.LjWAqCmSxYFLyCIl"

    fun setGlmApiKey(key: String) {
        prefs.edit().putString(KEY_GLM_API_KEY, key).apply()
    }

    fun getConfigUrl(): String = prefs.getString(KEY_CONFIG_URL, DEFAULT_CONFIG_URL) ?: DEFAULT_CONFIG_URL

    fun setConfigUrl(url: String) {
        prefs.edit().putString(KEY_CONFIG_URL, url).apply()
    }

    fun getLastKeywords(): String = prefs.getString(KEY_LAST_KEYWORDS, "早安,今天是个好日子,加油") ?: ""

    fun setLastKeywords(keywords: String) {
        prefs.edit().putString(KEY_LAST_KEYWORDS, keywords).apply()
    }

    /**
     * 获取兜底关键词（当后台配置不可用时）
     */
    fun getFallbackKeywords(): KeywordConfig {
        return KeywordConfig(
            keywords = listOf("早安", "加油", "今天是个好日子"),
            prompt_template = "请根据以下关键词生成一段自然流畅的中文语音播报内容，长度控制在50字以内，语气亲切自然：{keywords}"
        )
    }
}
