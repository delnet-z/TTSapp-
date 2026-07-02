package com.voiceapp.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voiceapp.data.VoiceContentRepo
import com.voiceapp.tts.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsManager: TtsManager,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var glmApiKey by remember { mutableStateOf(VoiceContentRepo.getGlmApiKey()) }
    var configUrl by remember { mutableStateOf(VoiceContentRepo.getConfigUrl()) }
    var localKeywords by remember { mutableStateOf(VoiceContentRepo.getLastKeywords()) }

    var speechRate by remember { mutableStateOf(1.0f) }
    var pitch by remember { mutableStateOf(1.0f) }

    // 获取可用语音列表
    val availableVoices = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsManager.getAvailableVoices()
        } else {
            emptyList()
        }
    }
    var selectedVoiceName by remember { mutableStateOf("") }
    var showVoicePicker by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // GLM API Key 配置
            Text(
                text = "GLM API 配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = glmApiKey,
                onValueChange = { glmApiKey = it },
                label = { Text("GLM API Key") },
                placeholder = { Text("粘贴从智谱AI开放平台获取的API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "前往 open.bigmodel.cn 注册获取免费额度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 后台配置接口
            Text(
                text = "后台配置接口",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = configUrl,
                onValueChange = { configUrl = it },
                label = { Text("配置接口URL") },
                placeholder = { Text("https://your-server.com/api/voice-config") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "返回JSON: {\"success\":true,\"data\":{\"keywords\":[\"早安\",\"加油\"],\"prompt_template\":\"...\"}}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 本地兜底关键词
            Text(
                text = "本地兜底关键词",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = localKeywords,
                onValueChange = { localKeywords = it },
                label = { Text("关键词（逗号分隔）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // TTS 设置
            Text(
                text = "语音播报设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 语音包选择
            if (availableVoices.isNotEmpty()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showVoicePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("语音包", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (selectedVoiceName.isBlank()) "系统默认" else selectedVoiceName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 语速
            Text("语速: ${"%.1f".format(speechRate)}")
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                valueRange = 0.5f..2.0f,
                steps = 14
            )

            // 音调
            Text("音调: ${"%.1f".format(pitch)}")
            Slider(
                value = pitch,
                onValueChange = { pitch = it },
                valueRange = 0.5f..2.0f,
                steps = 14
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 保存按钮
            Button(
                onClick = {
                    VoiceContentRepo.setGlmApiKey(glmApiKey)
                    VoiceContentRepo.setConfigUrl(configUrl)
                    VoiceContentRepo.setLastKeywords(localKeywords)

                    ttsManager.setVoice(selectedVoiceName)
                    ttsManager.setSpeechRate(speechRate)
                    ttsManager.setPitch(pitch)

                    scope.launch {
                        snackbarHostState.showSnackbar("设置已保存")
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存设置")
            }
        }
    }

    // 语音包选择弹窗
    if (showVoicePicker && availableVoices.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showVoicePicker = false },
            title = { Text("选择语音包") },
            text = {
                Column {
                    availableVoices.forEach { voice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoiceName = voice.name
                                    showVoicePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVoiceName == voice.name,
                                onClick = {
                                    selectedVoiceName = voice.name
                                    showVoicePicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = voice.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${voice.locale.displayName} | 质量: ${voice.quality}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoicePicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}
