package com.voiceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.voiceapp.data.ApiService
import com.voiceapp.data.KeywordConfig
import com.voiceapp.data.VoiceContentRepo
import com.voiceapp.tts.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(false) }
    var currentText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("点击播放按钮开始") }
    var showSettings by remember { mutableStateOf(false) }

    val ttsManager = remember { TtsManager(context) }
    val apiService = remember { ApiService() }

    // 初始化TTS参数
    LaunchedEffect(Unit) {
        val lastKeywords = VoiceContentRepo.getLastKeywords()
        val fallbackConfig = VoiceContentRepo.getFallbackKeywords()
        // 先加载默认关键词
        currentText = "准备就绪，关键词：${lastKeywords}"
    }

    if (showSettings) {
        SettingsScreen(
            ttsManager = ttsManager,
            onBack = { showSettings = false }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("语音启动器") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 状态图标
            Icon(
                imageVector = if (isLoading) Icons.Default.HourglassTop
                    else if (currentText.isNotBlank()) Icons.Default.VolumeUp
                    else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 状态标题
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 当前语音文本展示
            if (currentText.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "当前语音内容",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 主按钮 - 生成并播放
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "正在获取配置..."
                        currentText = ""

                        val configUrl = VoiceContentRepo.getConfigUrl()
                        val glmKey = VoiceContentRepo.getGlmApiKey()

                        // Step 1: 拉取后台关键词配置，失败则用兜底
                        val config: KeywordConfig = try {
                            val result = apiService.fetchConfig(configUrl)
                            result.getOrElse {
                                VoiceContentRepo.getFallbackKeywords()
                            }
                        } catch (e: Exception) {
                            VoiceContentRepo.getFallbackKeywords()
                        }

                        val keywords = if (config.keywords.isNotEmpty()) {
                            config.keywords
                        } else {
                            VoiceContentRepo.getLastKeywords().split("、", "，", ",")
                        }

                        statusMessage = "AI 正在生成语音内容..."

                        // Step 2: 调用GLM生成文本（无API Key时用模板直接拼接降级）
                        val voiceText: String = if (glmKey.isNotBlank()) {
                            try {
                                val result = apiService.generateVoiceText(glmKey, keywords, config.prompt_template)
                                result.getOrElse {
                                    "关键词：${keywords.joinToString("、")}，${
                                        config.prompt_template.replace("{keywords}", keywords.joinToString("、"))
                                    }"
                                }
                            } catch (e: Exception) {
                                "关键词：${keywords.joinToString("、")}"
                            }
                        } else {
                            // 无GLM Key：直接用关键词拼接
                            "关键词：${keywords.joinToString("、")}"
                        }

                        currentText = voiceText
                        statusMessage = "正在播放..."

                        // Step 3: 配置TTS参数并播放
                        if (config.tts_voice.isNotBlank()) {
                            ttsManager.setVoice(config.tts_voice)
                        }
                        ttsManager.setSpeechRate(config.tts_speed)
                        ttsManager.setPitch(config.tts_pitch)

                        // Step 4: TTS播放
                        val speakResult = ttsManager.speak(voiceText)
                        if (speakResult.isSuccess) {
                            statusMessage = "播放完成"
                        } else {
                            statusMessage = "播放失败: ${speakResult.exceptionOrNull()?.message}"
                            snackbarHostState.showSnackbar("播放失败")
                        }

                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("处理中...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成并播放", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 仅播放（不重新生成）
            OutlinedButton(
                onClick = {
                    if (currentText.isNotBlank()) {
                        scope.launch {
                            statusMessage = "正在播放..."
                            ttsManager.speak(currentText)
                            statusMessage = "播放完成"
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("请先生成语音内容")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = currentText.isNotBlank() && !isLoading
            ) {
                Icon(Icons.Default.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("重新播放")
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "使用说明",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. 首次使用请在设置中填入 GLM API Key\n" +
                                "2. 配置后台接口地址以动态更新关键词\n" +
                                "3. 可在设置中选择不同的语音包（需系统支持）\n" +
                                "4. 点击「生成并播放」获取AI语音内容",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
