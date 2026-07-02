package com.voiceapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voiceapp.ApiService
import com.voiceapp.LocationHelper
import com.voiceapp.WeatherService
import com.voiceapp.VoiceContentRepo
import com.voiceapp.WeatherData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isPlaying by remember { mutableStateOf(false) }
    var voiceText by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("正在初始化...") }
    var showSettings by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    val repo = remember { VoiceContentRepo() }
    val apiService = remember { ApiService() }
    val locationHelper = remember { LocationHelper(context) }
    val weatherService = remember { WeatherService() }

    // 必须先定义局部函数（Kotlin 不支持前向引用）
    suspend fun speakAndWait(text: String) {
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
            val id = "tts_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {
                    if (id == id) cont.resumeWith(Result.success(Unit))
                }
                override fun onError(id: String?) {
                    if (id == id) cont.resumeWith(Result.success(Unit))
                }
                @Deprecated("Deprecated")
                override fun onError(id: String?, code: Int) {
                    if (id == id) cont.resumeWith(Result.success(Unit))
                }
            })
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, hashMapOf(
                    TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to id
                ))
            }
            if (result == TextToSpeech.ERROR) cont.resumeWith(Result.success(Unit))
        }
    }

    suspend fun startAutoPlay() {
        statusText = "正在获取位置..."
        isPlaying = true
        voiceText = ""

        val location = locationHelper.getCurrentLocation()
        val weather: WeatherData? = location.getOrNull()?.let { loc ->
            statusText = "正在获取天气..."
            weatherService.getWeather(loc.latitude, loc.longitude).getOrNull()
        }

        statusText = "AI 正在生成语音..."
        val text = generateVoiceContent(weather, repo, apiService)
        voiceText = text

        statusText = "正在播放..."
        speakAndWait(text)

        isPlaying = false
        statusText = "播放完成"
    }

    suspend fun playWithoutLocation() {
        statusText = "AI 正在生成语音..."
        isPlaying = true
        voiceText = ""

        val text = generateVoiceContent(null, repo, apiService)
        voiceText = text

        statusText = "正在播放..."
        speakAndWait(text)

        isPlaying = false
        statusText = "播放完成"
    }

    // 定位权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch { startAutoPlay() }
        } else {
            statusText = "定位权限被拒绝，使用默认天气"
            scope.launch { playWithoutLocation() }
        }
    }

    // 初始化 TTS
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = java.util.Locale.CHINESE
            }
        }
    }

    // 自动播放入口：TTS 就绪后启动
    LaunchedEffect(ttsReady) {
        if (ttsReady) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startAutoPlay()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // 设置页
    if (showSettings) {
        SettingsScreen(
            onBack = { showSettings = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("语音播报") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // 状态图标
            Icon(
                imageVector = when {
                    isPlaying && voiceText.isBlank() -> Icons.Default.HourglassTop
                    isPlaying -> Icons.Default.VolumeUp
                    voiceText.isNotBlank() -> Icons.Default.CheckCircle
                    else -> Icons.Default.PlayCircle
                },
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (isPlaying && voiceText.isBlank()) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
            }

            // 播报内容卡片
            if (voiceText.isNotBlank()) {
                Spacer(Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "播报内容",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            voiceText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // 手动重新播放按钮
            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (voiceText.isBlank()) {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                startAutoPlay()
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } else {
                            isPlaying = true
                            statusText = "正在播放..."
                            speakAndWait(voiceText)
                            isPlaying = false
                            statusText = "播放完成"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPlaying
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text(if (voiceText.isBlank()) "重新生成" else "重新播放")
            }

            Spacer(Modifier.height(48.dp))

            // 使用说明
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "打开即自动播放，根据当前位置获取天气，AI 生成个性化语音。\n在设置中可自定义关键词，更换不同播报内容风格。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/** 生成播报内容的工具函数 */
private suspend fun generateVoiceContent(
    weather: WeatherData?,
    repo: VoiceContentRepo,
    api: ApiService
): String {
    val greeting = getTimeGreeting()
    val weatherDesc = weather?.let {
        "气温${it.temperature.toInt()}°C，湿度${it.humidity}%，${it.description}，风速${it.windSpeed.toInt()}km/h"
    } ?: "天气信息暂不可用"

    val prompt = repo.getPromptTemplate()
        .replace("{greeting}", greeting)
        .replace("{weather}", weatherDesc)
        .replace("{keywords}", repo.getKeywords())

    return try {
        api.generateText(prompt)
    } catch (e: Exception) {
        // GLM 失败时用模板直接拼接兜底
        "${greeting}！今日${weatherDesc}。${repo.getKeywords()}，祝您出行平安，一路顺风。"
    }
}

private fun getTimeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..5 -> "凌晨好"
        in 6..8 -> "早上好"
        in 9..11 -> "上午好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..21 -> "晚上好"
        else -> "夜深了"
    }
}
