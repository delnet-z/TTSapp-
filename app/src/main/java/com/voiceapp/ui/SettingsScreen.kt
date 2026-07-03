package com.voiceapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voiceapp.VoiceContentRepo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { VoiceContentRepo() }

    var promptTemplate by remember { mutableStateOf(repo.getPromptTemplate()) }
    var sysPrompt by remember { mutableStateOf(repo.getSysPrompt()) }
    var nickname by remember { mutableStateOf(repo.getNickname()) }
    var gpsTimeout by remember { mutableStateOf(repo.getGpsTimeout().toString()) }
    var temperature by remember { mutableStateOf(String.format("%.1f", repo.getTemperature())) }
    var modelName by remember { mutableStateOf(repo.getModelName()) }
    var noWeatherFallback by remember { mutableStateOf(repo.getNoWeatherFallback()) }
    var glmErrorFallback by remember { mutableStateOf(repo.getGlmErrorFallback()) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // =============================================
            // 全局占位符/参数说明表
            // =============================================
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("可用的占位符变量", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "{name} → 称呼（你设置的 AI 对你的称呼）\n" +
                        "{greeting} → 时段问候（上午好、中午好、晚上好 ...）\n" +
                        "{weather} → 天气描述（晴天，当前28°C，最高31°C ...）",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // =============================================
            // AI 生成
            // =============================================
            SectionHeader("AI 生成")

            Spacer(Modifier.height(12.dp))

            Text("AI 对你的称呼", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = nickname, onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("称呼") }, singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("系统提示词 (System Prompt)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("AI 生成的风格与规则。{name} 代表称呼。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = sysPrompt, onValueChange = { sysPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("系统提示词") }, minLines = 4, maxLines = 12
            )

            Spacer(Modifier.height(16.dp))

            Text("GLM 模型", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("glm-4-flash（免费）/ glm-4（付费更强）/ glm-4-plus（旗舰）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = modelName, onValueChange = { modelName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("模型名称") }, singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("Temperature", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("0.0~1.0。越低越确定稳定，越高越随机创意。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = temperature, onValueChange = { temperature = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Temperature") }, singleLine = true
            )

            Spacer(Modifier.height(20.dp))

            // =============================================
            // 播报模板
            // =============================================
            SectionHeader("播报模板")
            Text("以下模板中可使用占位符 {name} {greeting} {weather}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))

            Text("Prompt 模板", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("生成引擎的输入模板。{greeting}=时段问候 {weather}=天气描述", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = promptTemplate, onValueChange = { promptTemplate = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Prompt 模板") }, minLines = 2, maxLines = 4
            )

            Spacer(Modifier.height(16.dp))

            Text("天气不可用时的兜底文案", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("适用于定位失败、API 不可达等场景。不调用大模型，直接朗读此文案。\n可用 {name} {greeting}，不可用 {weather}（因为没有天气数据）。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = noWeatherFallback, onValueChange = { noWeatherFallback = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("天气不可用兜底") }, minLines = 2, maxLines = 4
            )

            Spacer(Modifier.height(16.dp))

            Text("大模型异常时的兜底文案", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("适用于 GLM 调用失败（网络异常、额度不足等）。不调用大模型，直接朗读此文案。\n可用 {name} {greeting} {weather}。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = glmErrorFallback, onValueChange = { glmErrorFallback = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GLM 失败兜底") }, minLines = 2, maxLines = 4
            )

            Spacer(Modifier.height(20.dp))

            // =============================================
            // 定位
            // =============================================
            SectionHeader("定位")

            Spacer(Modifier.height(12.dp))

            Text("GPS 定位超时（秒）", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("GPS 的等待时间，超时则用 IP 城市定位兜底。建议 3~8 秒。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = gpsTimeout, onValueChange = { gpsTimeout = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GPS 超时（秒）") }, singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    repo.savePromptTemplate(promptTemplate)
                    repo.saveSysPrompt(sysPrompt)
                    repo.saveNickname(nickname)
                    repo.saveGpsTimeout(gpsTimeout)
                    repo.saveTemperature(temperature)
                    repo.saveModelName(modelName)
                    repo.saveNoWeatherFallback(noWeatherFallback)
                    repo.saveGlmErrorFallback(glmErrorFallback)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存所有设置")
            }

            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text("所有设置已保存", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(Modifier.padding(vertical = 4.dp))
}
