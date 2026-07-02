package com.voiceapp

import android.content.Context
import android.content.SharedPreferences

/**
 * 本地存储 - 关键词、Prompt模板、历史内容
 */
class VoiceContentRepo {
    private val prefs: SharedPreferences by lazy {
        com.voiceapp.App.instance.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
    }

    fun getKeywords(): String {
        return prefs.getString("keywords", DEFAULT_KEYWORDS) ?: DEFAULT_KEYWORDS
    }

    fun getPromptTemplate(): String {
        return prefs.getString(
            "prompt_template",
            "时间：现在是{greeting}。{weather}。请根据以下关键词生成播报内容：{keywords}"
        ) ?: "时间：现在是{greeting}。{weather}。请根据以下关键词生成播报内容：{keywords}"
    }

    fun getBackendUrl(): String {
        return prefs.getString("backend_url", "") ?: ""
    }

    fun saveKeywords(keywords: String) {
        prefs.edit().putString("keywords", keywords).apply()
    }

    fun savePromptTemplate(template: String) {
        prefs.edit().putString("prompt_template", template).apply()
    }

    fun saveBackendUrl(url: String) {
        prefs.edit().putString("backend_url", url).apply()
    }

    fun saveLatestContent(content: String) {
        prefs.edit().putString("latest_content", content).apply()
    }

    fun getLatestContent(): String {
        return prefs.getString("latest_content", "") ?: ""
    }

    companion object {
        const val DEFAULT_KEYWORDS = "早安,今天是个好日子,加油"
    }
}
