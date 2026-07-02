package com.voiceapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voiceapp.ApiService
import com.voiceapp.LocationHelper
import com.voiceapp.WeatherService
import com.voiceapp.VoiceContentRepo
import com.voiceapp.WeatherData
import kotlinx.coroutines.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }

    var phase by remember { mutableStateOf(0) }
    var greetingText by remember { mutableStateOf("") }
    var fullContent by remember { mutableStateOf("") }
    var statusLine by remember { mutableStateOf("正在准备...") }
    var showSettings by remember { mutableStateOf(false) }

    val repo = remember { VoiceContentRepo() }
    val apiService = remember { ApiService() }
    val locationHelper = remember { LocationHelper(context) }
    val weatherService = remember { WeatherService() }

    // ====== 辅助函数（必须在调用方之前定义） ======

    fun getTimeGreeting(): String {
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

    fun buildGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeWord = when (hour) {
            in 0..5 -> "凌晨"
            in 6..8 -> "早上"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..21 -> "晚上"
            else -> "夜深了"
        }
        return "${timeWord}好！准备好出发了吗~"
    }

    suspend fun buildFullContent(weather: WeatherData?): String {
        val greeting = getTimeGreeting()
        val weatherDesc = weather?.let {
            buildString {
                append("${it.description}，当前${it.temperature.toInt()}°C")
                append("，最高${it.temperatureMax.toInt()}°C，最低${it.temperatureMin.toInt()}°C")
                if (it.precipitationProb > 0) append("，降水概率${it.precipitationProb}%")
                append("，湿度${it.humidity}%，风速${it.windSpeed.toInt()}km/h")
            }
        } ?: "天气信息暂不可用"

        val prompt = repo.getPromptTemplate()
            .replace("{greeting}", greeting)
            .replace("{weather}", weatherDesc)
            .replace("{keywords}", repo.getKeywords())

        return try {
            apiService.generateText(prompt)
        } catch (e: Exception) {
            "欣哥，${greeting}！今日${weatherDesc}。${repo.getKeywords()}，祝您出行平安，一路顺风！"
        }
    }

    /** 异步播放，不等待完成 */
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "greeting")
    }

    /** 同步播放，等待完成 */
    suspend fun speakAndWait(text: String) {
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
            val id = "tts_${System.currentTimeMillis()}"
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(s: String?) {}
                override fun onDone(s: String?) { cont.resumeWith(Result.success(Unit)) }
                override fun onError(s: String?) { cont.resumeWith(Result.success(Unit)) }
                @Deprecated("Deprecated")
                override fun onError(s: String?, code: Int) { cont.resumeWith(Result.success(Unit)) }
            })
            if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR)
                cont.resumeWith(Result.success(Unit))
        }
    }

    // ====== 核心流程 ======

    suspend fun startFlow() {
        phase = 1
        val greeting = buildGreeting()
        greetingText = greeting
        statusLine = "正在播报问候..."
        speak(greeting)

        phase = 2
        statusLine = "后台正在获取天气..."

        val weather = withTimeoutOrNull(4000L) {
            val loc = withTimeoutOrNull(3000L) {
                locationHelper.getCurrentLocation().getOrNull()
            }
            loc?.let { l ->
                withTimeoutOrNull(5000L) {
                    weatherService.getWeather(l.latitude, l.longitude).getOrNull()
                }
            }
        }

        val text = buildFullContent(weather)
        fullContent = text

        phase = 3
        statusLine = "正在播报..."
        speakAndWait(text)

        phase = 4
        statusLine = "播报完成"
    }

    // ====== 权限 & 启动 ======

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        scope.launch {
            if (granted) startFlow()
            else {
                statusLine = "定位权限被拒绝，使用默认天气"
                val text = buildFullContent(null)
                fullContent = text
                phase = 3
                speakAndWait(text)
                phase = 4
                statusLine = "播报完成"
            }
        }
    }

    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = java.util.Locale.CHINESE
        }
    }

    LaunchedEffect(ttsReady) {
        if (!ttsReady) return@LaunchedEffect
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) startFlow()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // ====== 设置页 ======

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    // ====== UI ======

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音播报") },
                actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "设置") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            val (icon, _) = when (phase) {
                0 -> Icons.Default.HourglassTop to "准备中"
                1 -> Icons.Default.PlayCircle to "播报中"
                2 -> Icons.Default.CloudDownload to "获取数据"
                3 -> Icons.Default.VolumeUp to "播放中"
                else -> Icons.Default.CheckCircle to "完成"
            }
            Icon(icon, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(16.dp))
            Text(statusLine, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (phase in 1..3) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(0.5f))
            }

            AnimatedVisibility(visible = greetingText.isNotBlank(), enter = fadeIn()) {
                Column { Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("即时问候", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                            Spacer(Modifier.height(4.dp))
                            Text(greetingText, style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = fullContent.isNotBlank(), enter = fadeIn()) {
                Column { Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("AI 生成播报", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(fullContent, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (phase == 4) {
                OutlinedButton(onClick = {
                    scope.launch { phase = 3; statusLine = "正在播报..."; speakAndWait(fullContent); phase = 4; statusLine = "播报完成" }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("重新播放")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    scope.launch { startFlow() }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("重新生成")
                }
            }
        }
    }
}
