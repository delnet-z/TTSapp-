package com.voiceapp.tts

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * TTS 管理器
 * - 支持系统内置语音引擎
 * - 支持自定义语音包（通过Voice选择）
 * - 可调节语速、音调
 */
class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.language = Locale.CHINESE
            }
        }
    }

    /**
     * 获取系统可用语音列表
     */
    fun getAvailableVoices(): List<Voice> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.voices?.filter { it.locale.language == Locale.CHINESE.language } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 设置语音包（通过Voice名称）
     */
    fun setVoice(voiceName: String) {
        if (voiceName.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voice = tts?.voices?.find { it.name == voiceName }
            if (voice != null) {
                tts?.voice = voice
            }
        }
    }

    /**
     * 设置语速 0.5 - 2.0
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    /**
     * 设置音调 0.5 - 2.0
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    /**
     * 播报文本，挂起等待播报完成
     */
    suspend fun speak(text: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        if (!isInitialized || tts == null) {
            cont.resume(Result.failure(Exception("TTS 未初始化")))
            return@suspendCancellableCoroutine
        }

        val utteranceId = "voice_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == utteranceId && cont.isActive) {
                    cont.resume(Result.success(Unit))
                }
            }

            override fun onError(utteranceId: String?) {
                if (utteranceId == utteranceId && cont.isActive) {
                    cont.resume(Result.failure(Exception("TTS 播报出错")))
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                if (utteranceId == utteranceId && cont.isActive) {
                    cont.resume(Result.failure(Exception("TTS 播报出错，错误码: $errorCode")))
                }
            }
        })

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, hashMapOf(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId
            ))
        }

        if (result == TextToSpeech.ERROR) {
            cont.resume(Result.failure(Exception("TTS 播报启动失败")))
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
