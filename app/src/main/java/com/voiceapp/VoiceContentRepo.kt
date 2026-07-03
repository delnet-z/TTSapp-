package com.voiceapp

import android.content.Context
import android.content.SharedPreferences

/**
 * 本地存储 - Prompt模板、系统提示词、称呼、参数
 */
class VoiceContentRepo {
    private val prefs: SharedPreferences by lazy {
        com.voiceapp.App.instance.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
    }

    // ====== Prompt 模板 ======

    fun getPromptTemplate(): String {
        return prefs.getString("prompt_template", DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
    }

    fun savePromptTemplate(template: String) {
        prefs.edit().putString("prompt_template", template).apply()
    }

    // ====== 系统提示词 ======

    fun getSysPrompt(): String {
        return prefs.getString("sys_prompt", DEFAULT_SYS_PROMPT) ?: DEFAULT_SYS_PROMPT
    }

    fun saveSysPrompt(v: String) {
        prefs.edit().putString("sys_prompt", v).apply()
    }

    // ====== 称呼 ======

    fun getNickname(): String {
        return prefs.getString("nickname", "欣哥") ?: "欣哥"
    }

    fun saveNickname(v: String) {
        prefs.edit().putString("nickname", v).apply()
    }

    // ====== GPS 超时(秒) ======

    fun getGpsTimeout(): Int {
        return prefs.getString("gps_timeout", "3")?.toIntOrNull() ?: 3
    }

    fun saveGpsTimeout(v: String) {
        prefs.edit().putString("gps_timeout", v).apply()
    }

    // ====== GLM Temperature ======

    fun getTemperature(): Double {
        return prefs.getString("temperature", "0.7")?.toDoubleOrNull() ?: 0.7
    }

    fun saveTemperature(v: String) {
        prefs.edit().putString("temperature", v).apply()
    }

    // ====== GLM 模型名称 ======

    fun getModelName(): String {
        return prefs.getString("model_name", "glm-4-flash") ?: "glm-4-flash"
    }

    fun saveModelName(v: String) {
        prefs.edit().putString("model_name", v).apply()
    }

    // ====== 天气不可用兜底 ======

    fun getNoWeatherFallback(): String {
        return prefs.getString("no_weather_fallback", DEFAULT_NO_WEATHER) ?: DEFAULT_NO_WEATHER
    }

    fun saveNoWeatherFallback(v: String) {
        prefs.edit().putString("no_weather_fallback", v).apply()
    }

    // ====== GLM 异常兜底 ======

    fun getGlmErrorFallback(): String {
        return prefs.getString("glm_error_fallback", DEFAULT_GLM_ERROR) ?: DEFAULT_GLM_ERROR
    }

    fun saveGlmErrorFallback(v: String) {
        prefs.edit().putString("glm_error_fallback", v).apply()
    }

    // ====== 历史内容 ======

    fun saveLatestContent(content: String) {
        prefs.edit().putString("latest_content", content).apply()
    }

    fun getLatestContent(): String {
        return prefs.getString("latest_content", "") ?: ""
    }

    fun getBackendUrl(): String {
        return prefs.getString("backend_url", "") ?: ""
    }

    fun saveBackendUrl(url: String) {
        prefs.edit().putString("backend_url", url).apply()
    }

    companion object {
        const val DEFAULT_TEMPLATE = "{greeting}！今日天气：{weather}"
        const val DEFAULT_SYS_PROMPT = "你是{name}的车载语音助手，每次上车播报一句。称呼{name}并带时段问候（如上午好），然后说当前温度和天气，接着给一句驾驶建议，最后祝福结束。80-120字，纯文本。词汇黑名单：严禁输出「防晒」「护肤」「补水」「帽子」「雨伞」「衣物」「中暑」「涂抹」「皮肤」。驾驶建议规则：降水或有雨→减速慢行保持车距开雾灯；温度>30°C→开空调检查胎压；大风→握紧方向盘注意横风；有雾→开雾灯低速；正常→保持车速注意前车。"
        const val DEFAULT_NO_WEATHER = "{name}，{greeting}！天气信息暂不可用。请注意安全驾驶。祝您出行平安！"
        const val DEFAULT_GLM_ERROR = "{name}，{greeting}！今日{weather}。祝您出行平安！"
    }
}
