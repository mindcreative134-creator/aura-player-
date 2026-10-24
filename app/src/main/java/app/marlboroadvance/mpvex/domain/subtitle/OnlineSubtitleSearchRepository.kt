package app.marlboroadvance.mpvex.domain.subtitle

import android.content.Context
import android.util.Log
import app.marlboroadvance.mpvex.utils.media.MediaInfoParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class OnlineSubtitleSearchRepository(
  private val context: Context,
) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(25, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()

  private val userAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

  private val languageMap = mapOf(
    "en" to "English",
    "eng" to "English",
    "hi" to "Hindi",
    "hin" to "Hindi",
    "es" to "Spanish",
    "spa" to "Spanish",
    "fr" to "French",
    "fre" to "French",
    "fra" to "French",
    "de" to "German",
    "ger" to "German",
    "deu" to "German",
    "ar" to "Arabic",
    "ara" to "Arabic",
    "ja" to "Japanese",
    "jpn" to "Japanese",
    "ko" to "Korean",
    "kor" to "Korean",
    "ru" to "Russian",
    "rus" to "Russian",
    "zh" to "Chinese",
    "chi" to "Chinese",
    "zho" to "Chinese",
    "pt" to "Portuguese",
    "por" to "Portuguese",
    "it" to "Italian",
    "ita" to "Italian",
    "bn" to "Bengali",
    "ta" to "Tamil",
    "te" to "Telugu",
    "ur" to "Urdu",
    "tr" to "Turkish",
    "id" to "Indonesian",
    "vi" to "Vietnamese",
    "th" to "Thai",
    "nl" to "Dutch",
    "pl" to "Polish",
    "sv" to "Swedish"
  )

  suspend fun search(
    request: OnlineSubtitleSearchRequest,
  ): Result<List<OnlineSubtitle>> = withContext(Dispatchers.IO) {
    try {
      val rawQuery = request.query.trim()
      val parsedMedia = MediaInfoParser.parse(rawQuery)
      val cleanQuery = parsedMedia.title.ifBlank {
        rawQuery.substringBeforeLast(".")
          .replace(Regex("""[._-]"""), " ")
          .trim()
      }

      val allResults = mutableListOf<OnlineSubtitle>()

      // 1. Search SubtitleCat
      coroutineScope {
        val catDeferred = async {
          runCatching { searchSubtitleCat(cleanQuery) }.getOrDefault(emptyList())
        }
        val rtDeferred = async {
          runCatching { searchMovieSubtitlesRt(cleanQuery) }.getOrDefault(emptyList())
        }

        allResults.addAll(catDeferred.await())
        allResults.addAll(rtDeferred.await())
      }

      // If clean query yielded few results and rawQuery is distinct, try rawQuery
      if (allResults.isEmpty() && cleanQuery != rawQuery) {
        val fallbackResults = runCatching { searchSubtitleCat(rawQuery) }.getOrDefault(emptyList())
        allResults.addAll(fallbackResults)
      }

      val sorted = allResults
        .distinctBy { it.url.lowercase() }
        .sortedWith(
          compareByDescending<OnlineSubtitle> { it.downloadCount }
            .thenBy { it.displayLanguage }
            .thenBy { it.displayName }
        )

      Result.success(sorted)
    } catch (c: CancellationException) {
      throw c
    } catch (e: Exception) {
      Log.e("OnlineSubtitleSearch", "Subtitle search failed", e)
      Result.failure(e)
    }
  }

  private suspend fun searchSubtitleCat(query: String): List<OnlineSubtitle> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "https://www.subtitlecat.com/index.php?search=$encoded&show=100"

    val request = Request.Builder()
      .url(url)
      .header("User-Agent", userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .build()

    val html = client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return@withContext emptyList()
      response.body?.string() ?: return@withContext emptyList()
    }

    val searchResults = SubtitleCatHtmlParser.parseSearchResults(html).take(6)
    if (searchResults.isEmpty()) return@withContext emptyList()

    coroutineScope {
      searchResults.map { item ->
        async {
          runCatching {
            val detailsUrl = if (item.path.startsWith("http")) item.path else "https://www.subtitlecat.com/${item.path.trimStart('/')}"
            val detailReq = Request.Builder()
              .url(detailsUrl)
              .header("User-Agent", userAgent)
              .build()

            val detailHtml = client.newCall(detailReq).execute().use { it.body?.string() } ?: return@runCatching emptyList<OnlineSubtitle>()
            val links = SubtitleCatHtmlParser.parseDownloadLinks(detailHtml)

            links.map { link ->
              val downloadUrl = if (link.path.startsWith("http")) link.path else "https://www.subtitlecat.com/${link.path.trimStart('/')}"
              val langCode = link.languageCode.lowercase().trim()
              val langLabel = link.languageLabel ?: languageMap[langCode] ?: langCode.uppercase()

              OnlineSubtitle(
                id = "subtitlecat:${item.path}:$langCode",
                displayName = link.fileName ?: "${item.title} [$langLabel]",
                displayLanguage = langLabel,
                language = langCode,
                url = downloadUrl,
                format = "srt",
                source = "SubtitleCat",
                downloadCount = item.downloads,
                fileName = link.fileName,
                release = item.title,
              )
            }
          }.getOrDefault(emptyList())
        }
      }.awaitAll().flatten()
    }
  }

  private suspend fun searchMovieSubtitlesRt(query: String): List<OnlineSubtitle> = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "https://moviesubtitlesrt.com/?s=$encoded"

    val request = Request.Builder()
      .url(url)
      .header("User-Agent", userAgent)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .build()

    val html = client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) return@withContext emptyList()
      response.body?.string() ?: return@withContext emptyList()
    }

    val doc = Jsoup.parse(html, "https://moviesubtitlesrt.com")
    val pageLinks = doc.select("div.inside-article header h2 a[href], article h2 a[href]")
      .take(4)
      .mapNotNull {
        val title = it.text().trim()
        val href = it.absUrl("href")
        if (title.isNotBlank() && href.isNotBlank()) title to href else null
      }

    if (pageLinks.isEmpty()) return@withContext emptyList()

    coroutineScope {
      pageLinks.map { (title, pageUrl) ->
        async {
          runCatching {
            val pageReq = Request.Builder()
              .url(pageUrl)
              .header("User-Agent", userAgent)
              .build()

            val pageHtml = client.newCall(pageReq).execute().use { it.body?.string() } ?: return@runCatching emptyList<OnlineSubtitle>()
            val pageDoc = Jsoup.parse(pageHtml, pageUrl)

            val zipLink = pageDoc.selectFirst("a[href$=.zip], a[href*=.zip], a[href*=wp-content][href*=zip]")
              ?.absUrl("href")
              ?.takeIf { it.isNotBlank() } ?: return@runCatching emptyList()

            val langText = pageDoc.select("td:contains(Language) + td, tr:contains(Language) td:last-child")
              .firstOrNull()?.text()?.trim() ?: "English"
            val langCode = langText.take(2).lowercase()

            listOf(
              OnlineSubtitle(
                id = "moviesubtitlesrt:$pageUrl",
                displayName = title,
                displayLanguage = langText,
                language = langCode,
                url = zipLink,
                format = "zip",
                source = "MovieSubtitlesRT",
                downloadCount = 10,
                release = title,
              )
            )
          }.getOrDefault(emptyList())
        }
      }.awaitAll().flatten()
    }
  }

  suspend fun download(
    subtitle: OnlineSubtitle,
    mediaTitle: String,
  ): Result<File> = withContext(Dispatchers.IO) {
    try {
      val request = Request.Builder()
        .url(subtitle.url)
        .header("User-Agent", userAgent)
        .header("Accept", "text/plain, text/*, application/zip, application/octet-stream, */*")
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          return@withContext Result.failure(IOException("HTTP ${response.code} downloading subtitle"))
        }

        val rawBytes = response.body?.bytes()
          ?: return@withContext Result.failure(IOException("Empty response body"))

        // Check if HTML error response instead of subtitle
        val prefix = String(rawBytes.take(50).toByteArray(), Charsets.UTF_8).lowercase()
        if (prefix.contains("<html") || prefix.contains("<!doctype")) {
          return@withContext Result.failure(IOException("Server returned HTML page instead of subtitle file"))
        }

        // Unpack zip archive if payload is a ZIP (magic bytes PK\x03\x04 or 0x50 0x4B)
        var finalBytes = rawBytes
        var finalExt = if (subtitle.format.isNotBlank() && subtitle.format != "zip") subtitle.format else "srt"

        if (rawBytes.size >= 4 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
          var extracted: ByteArray? = null
          var extractedName: String? = null
          ZipInputStream(ByteArrayInputStream(rawBytes)).use { zis ->
            while (true) {
              val entry = zis.nextEntry ?: break
              val name = entry.name.lowercase()
              if (!entry.isDirectory && (name.endsWith(".srt") || name.endsWith(".vtt") || name.endsWith(".ass") || name.endsWith(".sub") || name.endsWith(".ssa"))) {
                extracted = zis.readBytes()
                extractedName = entry.name.substringAfterLast('/')
                break
              }
            }
          }
          if (extracted != null && extracted.isNotEmpty()) {
            finalBytes = extracted
            finalExt = extractedName?.substringAfterLast('.', "srt") ?: "srt"
          }
        }

        val cacheDir = File(context.cacheDir, "subtitles").apply { mkdirs() }
        val cleanTitle = mediaTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
        val targetFile = File(cacheDir, "${cleanTitle}_${subtitle.language}_${System.currentTimeMillis()}.$finalExt")

        FileOutputStream(targetFile).use { output ->
          output.write(finalBytes)
        }

        Result.success(targetFile)
      }
    } catch (c: CancellationException) {
      throw c
    } catch (e: Exception) {
      Log.e("OnlineSubtitleSearch", "Download failed", e)
      Result.failure(e)
    }
  }
}
