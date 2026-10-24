package app.marlboroadvance.mpvex.domain.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

enum class AiProvider(val displayName: String, val translationModel: String, val whisperModel: String) {
    GROQ("Groq (Free & Ultra Fast)", "llama-3.3-70b-versatile", "whisper-large-v3"),
    OPENAI("OpenAI", "gpt-4o-mini", "whisper-1"),
    ANTHROPIC("Anthropic Claude", "claude-3-5-haiku-20241022", "")
}

data class AiConfig(
    val provider: AiProvider = AiProvider.GROQ,
    val apiKey: String = "",
    val targetLanguage: String = "Hindi"
)

class AiSubtitleManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("apna_ai_preferences", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getConfig(): AiConfig {
        val providerStr = prefs.getString(KEY_PROVIDER, AiProvider.GROQ.name) ?: AiProvider.GROQ.name
        val provider = runCatching { AiProvider.valueOf(providerStr) }.getOrDefault(AiProvider.GROQ)
        val apiKey = prefs.getString(KEY_API_KEY, "") ?: ""
        val targetLang = prefs.getString(KEY_TARGET_LANG, "Hindi") ?: "Hindi"
        return AiConfig(provider, apiKey, targetLang)
    }

    fun saveConfig(config: AiConfig) {
        prefs.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_TARGET_LANG, config.targetLanguage.trim())
            .apply()
    }

    /**
     * Translates an existing subtitle file (.srt, .vtt) into target language using selected LLM.
     * Preserves timestamps and formatting.
     */
    suspend fun translateSubtitleFile(
        subtitleFile: File,
        targetLanguage: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val config = getConfig()
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("AI API Key enter karein"))
        }

        val text = runCatching { subtitleFile.readText() }.getOrElse {
            return@withContext Result.failure(it)
        }

        val chunks = splitSrtIntoChunks(text, maxLinesPerChunk = 60)
        val translatedChunks = mutableListOf<String>()

        chunks.forEachIndexed { index, chunk ->
            onProgress((index.toFloat() / chunks.size.toFloat()))
            val translated = translateChunk(chunk, targetLanguage, config)
            if (translated.isFailure) {
                return@withContext Result.failure(translated.exceptionOrNull() ?: Exception("Translation failed"))
            }
            translatedChunks.add(translated.getOrNull().orEmpty())
        }

        onProgress(1f)
        val fullTranslatedSrt = translatedChunks.joinToString("\n\n")

        val outDir = File(context.cacheDir, "ai_subtitles").apply { mkdirs() }
        val outFile = File(outDir, "${subtitleFile.nameWithoutExtension}.${targetLanguage.lowercase()}.ai.srt")
        outFile.writeText(fullTranslatedSrt)

        Result.success(outFile)
    }

    /**
     * Transcribes audio directly to .srt subtitles using Whisper API.
     */
    suspend fun transcribeAudioToSrt(
        audioFile: File,
        language: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val config = getConfig()
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("AI API Key enter karein"))
        }

        val endpoint = when (config.provider) {
            AiProvider.GROQ -> "https://api.groq.com/openai/v1/audio/transcriptions"
            AiProvider.OPENAI -> "https://api.openai.com/v1/audio/transcriptions"
            AiProvider.ANTHROPIC -> return@withContext Result.failure(
                UnsupportedOperationException("Anthropic doesn't support Whisper speech-to-text. Use Groq or OpenAI.")
            )
        }

        val requestBodyBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/*".toMediaType()))
            .addFormDataPart("model", config.provider.whisperModel)
            .addFormDataPart("response_format", "srt")

        if (!language.isNullOrBlank()) {
            requestBodyBuilder.addFormDataPart("language", language)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(requestBodyBuilder.build())
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Whisper API error ${response.code}: $body"))
            }

            val outDir = File(context.cacheDir, "ai_subtitles").apply { mkdirs() }
            val outFile = File(outDir, "${audioFile.nameWithoutExtension}.whisper.ai.srt")
            outFile.writeText(body)

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun translateChunk(chunk: String, targetLanguage: String, config: AiConfig): Result<String> {
        val systemPrompt = "You are a professional subtitle translator. " +
            "Translate the provided SRT subtitle cues into $targetLanguage. " +
            "Strict rules:\n" +
            "1. Retain the exact subtitle cue numbers and timestamps.\n" +
            "2. Only translate the spoken text lines.\n" +
            "3. Do not add explanations, conversational comments, or code blocks. Output ONLY valid SRT."

        return when (config.provider) {
            AiProvider.GROQ -> callOpenAiCompatible(
                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = config.apiKey,
                model = config.provider.translationModel,
                systemPrompt = systemPrompt,
                userPrompt = chunk
            )
            AiProvider.OPENAI -> callOpenAiCompatible(
                endpoint = "https://api.openai.com/v1/chat/completions",
                apiKey = config.apiKey,
                model = config.provider.translationModel,
                systemPrompt = systemPrompt,
                userPrompt = chunk
            )
            AiProvider.ANTHROPIC -> callAnthropic(
                apiKey = config.apiKey,
                model = config.provider.translationModel,
                systemPrompt = systemPrompt,
                userPrompt = chunk
            )
        }
    }

    private fun callOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("temperature", 0.3)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("AI API error ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            Result.success(cleanSrtOutput(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callAnthropic(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("system", systemPrompt)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Anthropic API error ${response.code}: $body"))
            }

            val json = JSONObject(body)
            val content = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
            Result.success(cleanSrtOutput(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun splitSrtIntoChunks(srtText: String, maxLinesPerChunk: Int): List<String> {
        val blocks = srtText.split(Regex("(?m)^\\s*$"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var currentCount = 0

        for (block in blocks) {
            if (currentCount >= maxLinesPerChunk) {
                chunks.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
                currentCount = 0
            }
            currentChunk.append(block).append("\n\n")
            currentCount++
        }

        if (currentChunk.isNotBlank()) {
            chunks.add(currentChunk.toString().trim())
        }
        return chunks
    }

    private fun cleanSrtOutput(text: String): String {
        return text.removePrefix("```srt")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    companion object {
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_API_KEY = "ai_api_key"
        private const val KEY_TARGET_LANG = "ai_target_lang"

        @Volatile
        private var INSTANCE: AiSubtitleManager? = null

        fun getInstance(context: Context): AiSubtitleManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiSubtitleManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
