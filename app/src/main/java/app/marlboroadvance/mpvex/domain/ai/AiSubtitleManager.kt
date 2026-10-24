package app.marlboroadvance.mpvex.domain.ai

import android.content.Context
import android.util.Log
import app.marlboroadvance.mpvex.utils.security.SecureKeyStoreHelper
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
        val targetLang = prefs.getString(KEY_TARGET_LANG, "Hindi") ?: "Hindi"

        // Decrypt API key from AndroidKeyStore with migration fallback
        val encryptedKey = prefs.getString(KEY_API_KEY_ENCRYPTED, null)
        val apiKey = if (!encryptedKey.isNullOrBlank()) {
            SecureKeyStoreHelper.decrypt(KEYSTORE_ALIAS, encryptedKey) ?: ""
        } else {
            // Legacy migration: check if plaintext key exists
            val plaintextKey = prefs.getString(KEY_API_KEY_PLAINTEXT, "") ?: ""
            if (plaintextKey.isNotBlank()) {
                val encrypted = SecureKeyStoreHelper.encrypt(KEYSTORE_ALIAS, plaintextKey)
                if (encrypted != null) {
                    prefs.edit()
                        .putString(KEY_API_KEY_ENCRYPTED, encrypted)
                        .remove(KEY_API_KEY_PLAINTEXT)
                        .apply()
                }
            }
            plaintextKey
        }

        return AiConfig(provider, apiKey, targetLang)
    }

    fun saveConfig(config: AiConfig): Result<Unit> {
        val trimmedKey = config.apiKey.trim()
        val editor = prefs.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_TARGET_LANG, config.targetLanguage.trim())

        if (trimmedKey.isNotBlank()) {
            val encrypted = SecureKeyStoreHelper.encrypt(KEYSTORE_ALIAS, trimmedKey)
            if (encrypted != null) {
                editor.putString(KEY_API_KEY_ENCRYPTED, encrypted)
                editor.remove(KEY_API_KEY_PLAINTEXT)
                editor.apply()
                return Result.success(Unit)
            } else {
                // Strictly refuse plaintext fallback storage for privacy compliance
                Log.e(TAG, "Failed to encrypt API key using Android KeyStore; refusing to store plaintext")
                editor.apply() // Provider and language changes are saved
                return Result.failure(IllegalStateException("Could not securely encrypt API key using Android KeyStore"))
            }
        } else {
            editor.remove(KEY_API_KEY_ENCRYPTED)
            editor.remove(KEY_API_KEY_PLAINTEXT)
            editor.apply()
            return Result.success(Unit)
        }
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
            return@withContext Result.failure(IllegalStateException("AI API Key required. Please configure your key in settings."))
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
     * Uploads the selected audio file to the configured third-party Whisper endpoint.
     */
    suspend fun transcribeAudioToSrt(
        audioFile: File,
        language: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val config = getConfig()
        if (config.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("AI API Key required. Please configure your key in settings."))
        }

        val endpoint = when (config.provider) {
            AiProvider.GROQ -> "https://api.groq.com/openai/v1/audio/transcriptions"
            AiProvider.OPENAI -> "https://api.openai.com/v1/audio/transcriptions"
            AiProvider.ANTHROPIC -> return@withContext Result.failure(
                UnsupportedOperationException("Anthropic does not support Whisper speech-to-text. Please select Groq or OpenAI.")
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
            if (!response.isSuccessful) {
                val safeError = sanitizeError(response.code, "Whisper transcription")
                return@withContext Result.failure(Exception(safeError))
            }

            val body = response.body.use { it.string() }
            val outDir = File(context.cacheDir, "ai_subtitles").apply { mkdirs() }
            val outFile = File(outDir, "${audioFile.nameWithoutExtension}.whisper.ai.srt")
            outFile.writeText(body)

            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(Exception("Transcription network failure: ${e.message}"))
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
            if (!response.isSuccessful) {
                return Result.failure(Exception(sanitizeError(response.code, "Translation")))
            }

            val body = response.body.use { it.string() }
            val json = JSONObject(body)
            val content = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            Result.success(cleanSrtOutput(content))
        } catch (e: Exception) {
            Result.failure(Exception("AI request error: ${e.message}"))
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
            if (!response.isSuccessful) {
                return Result.failure(Exception(sanitizeError(response.code, "Anthropic translation")))
            }

            val body = response.body.use { it.string() }
            val json = JSONObject(body)
            val content = json.getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()
            Result.success(cleanSrtOutput(content))
        } catch (e: Exception) {
            Result.failure(Exception("AI request error: ${e.message}"))
        }
    }

    private fun sanitizeError(statusCode: Int, operation: String): String {
        return when (statusCode) {
            401 -> "$operation failed: Invalid API key (HTTP 401). Please check your key in settings."
            403 -> "$operation failed: Access denied or forbidden (HTTP 403)."
            429 -> "$operation failed: API rate limit or quota exceeded (HTTP 429). Please try again later."
            500, 502, 503, 504 -> "$operation failed: Provider service temporarily unavailable (HTTP $statusCode)."
            else -> "$operation request failed with HTTP status $statusCode."
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
        private const val TAG = "AiSubtitleManager"
        private const val KEYSTORE_ALIAS = "aura_ai_key"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_API_KEY_PLAINTEXT = "ai_api_key"
        private const val KEY_API_KEY_ENCRYPTED = "ai_api_key_enc"
        private const val KEY_TARGET_LANG = "ai_target_lang"

        @Volatile
        private var INSTANCE: AiSubtitleManager? = null

        fun getInstance(context: Context): AiSubtitleManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiSubtitleManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
