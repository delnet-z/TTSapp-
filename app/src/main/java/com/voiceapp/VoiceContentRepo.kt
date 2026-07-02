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
            "{greeting}！今日天气：{weather}"
        ) ?: "{greeting}！今日天气：{weather}"
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
        const val DEFAULT_KEYWORDS = "出发,元气满满,安全第一,今天也是超棒的一天"
    }
}
