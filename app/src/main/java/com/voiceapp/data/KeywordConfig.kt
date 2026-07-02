package com.voiceapp.data

/**
 * 关键词配置数据类 - 对应后台下发的JSON结构
 */
data class KeywordConfig(
    val keywords: List<String>,
    val prompt_template: String = "请根据以下关键词生成一段自然流畅的中文语音播报内容，长度控制在50字以内，语气亲切自然：{keywords}",
    val tts_engine: String = "system",      // system / custom
    val tts_voice: String = "",              // 自定义语音包ID
    val tts_speed: Float = 1.0f,
    val tts_pitch: Float = 1.0f
)
