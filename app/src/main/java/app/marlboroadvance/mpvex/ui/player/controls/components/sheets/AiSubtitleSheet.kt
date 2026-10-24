package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.ai.AiConfig
import app.marlboroadvance.mpvex.domain.ai.AiProvider
import app.marlboroadvance.mpvex.domain.ai.AiSubtitleManager
import kotlinx.coroutines.launch
import java.io.File

private val popularLanguages = listOf(
    "Hindi",
    "English",
    "Hinglish",
    "Spanish",
    "French",
    "Japanese",
    "German",
    "Korean",
    "Arabic"
)

@Composable
fun AiSubtitleSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    currentSubtitleFile: File?,
    currentAudioFile: File?,
    onSubtitleGenerated: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiManager = remember { AiSubtitleManager.getInstance(context) }

    var config by remember { mutableStateOf(aiManager.getConfig()) }
    var apiKeyInput by remember { mutableStateOf(config.apiKey) }
    var selectedLang by remember { mutableStateOf(config.targetLanguage) }
    var selectedProvider by remember { mutableStateOf(config.provider) }

    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AI Subtitles & Translation",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Multi-provider AI subtitle engine (Groq Llama 3.3, Whisper & OpenAI)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Provider selection
            Text("Select AI Provider", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiProvider.entries.forEach { p ->
                    FilterChip(
                        selected = selectedProvider == p,
                        onClick = {
                            selectedProvider = p
                            config = config.copy(provider = p)
                            aiManager.saveConfig(config)
                        },
                        label = { Text(p.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // API Key Input
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    config = config.copy(apiKey = it)
                    aiManager.saveConfig(config)
                },
                label = { Text("API Key (${selectedProvider.name})") },
                placeholder = { Text("gsk_... or sk-...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Key, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Target Language Chips
            Text("Target Language", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(popularLanguages) { lang ->
                    FilterChip(
                        selected = selectedLang.equals(lang, ignoreCase = true),
                        onClick = {
                            selectedLang = lang
                            config = config.copy(targetLanguage = lang)
                            aiManager.saveConfig(config)
                        },
                        label = { Text(lang) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress or Error state
            if (isLoading) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF00F2FE)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Text(
                text = "Privacy Notice: Subtitle text or audio file will be sent directly from your device to the selected AI provider using your personal API key under their terms of service.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Translate Subtitles Button
                Button(
                    onClick = {
                        val subFile = currentSubtitleFile
                        if (subFile == null) {
                            errorMessage = "Pehle koi subtitle track ya external .srt file load karein."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        statusText = "Translating subtitles to $selectedLang..."
                        progress = 0f
                        scope.launch {
                            val res = aiManager.translateSubtitleFile(subFile, selectedLang) {
                                progress = it
                            }
                            isLoading = false
                            res.fold(
                                onSuccess = { file ->
                                    statusText = "Completed: ${file.name}"
                                    onSubtitleGenerated(file)
                                    onDismissRequest()
                                },
                                onFailure = {
                                    errorMessage = "Translation failed: ${it.message}"
                                }
                            )
                        }
                    },
                    enabled = !isLoading && apiKeyInput.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Translate, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Translate")
                }

                // Whisper Speech-to-Sub Button
                OutlinedButton(
                    onClick = {
                        val audioFile = currentAudioFile
                        if (audioFile == null) {
                            errorMessage = "Audio stream / video file not accessible for Whisper."
                            return@OutlinedButton
                        }
                        isLoading = true
                        errorMessage = null
                        statusText = "Transcribing with Whisper AI..."
                        progress = 0f
                        scope.launch {
                            val res = aiManager.transcribeAudioToSrt(audioFile)
                            isLoading = false
                            res.fold(
                                onSuccess = { file ->
                                    statusText = "Transcribed: ${file.name}"
                                    onSubtitleGenerated(file)
                                    onDismissRequest()
                                },
                                onFailure = {
                                    errorMessage = "Whisper failed: ${it.message}"
                                }
                            )
                        }
                    },
                    enabled = !isLoading && apiKeyInput.isNotBlank() && selectedProvider != AiProvider.ANTHROPIC,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Whisper Sub")
                }
            }
        }
    }
}
