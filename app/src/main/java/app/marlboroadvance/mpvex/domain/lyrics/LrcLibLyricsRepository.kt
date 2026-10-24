package app.marlboroadvance.mpvex.domain.lyrics

import android.util.LruCache
import app.marlboroadvance.mpvex.domain.common.Http
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

sealed interface LyricsState {
    data object Idle : LyricsState
    data object Loading : LyricsState
    data object NotFound : LyricsState
    data class Synced(val lines: List<LrcLine>) : LyricsState
    data class Plain(val text: String) : LyricsState
}

/** LRCLIB (https://lrclib.net) synced lyrics repository. Free, no API key needed. */
class LrcLibLyricsRepository {

    private val cache = LruCache<String, LyricsState>(50)

    suspend fun fetch(title: String, artist: String, album: String, durationMs: Long): LyricsState {
        val cleanArtist = artist.takeUnless { it.startsWith("Unknown") }.orEmpty()
        val cleanAlbum = album.takeUnless { it.startsWith("Unknown") }.orEmpty()
        val key = "$title|$cleanArtist|$cleanAlbum"
        cache.get(key)?.let { return it }

        // 1) Exact match (duration with track name & artist)
        val getUrl = buildString {
            append("https://lrclib.net/api/get?track_name=").append(enc(title))
            if (cleanArtist.isNotBlank()) append("&artist_name=").append(enc(cleanArtist))
            if (cleanAlbum.isNotBlank()) append("&album_name=").append(enc(cleanAlbum))
            if (durationMs > 0) append("&duration=").append(durationMs / 1000)
        }
        var result = Http.get(getUrl)?.let { runCatching { toState(JSONObject(it)) }.getOrNull() }

        // 2) Fallback: search
        if (result == null || result == LyricsState.NotFound) {
            val searchUrl = buildString {
                append("https://lrclib.net/api/search?track_name=").append(enc(title))
                if (cleanArtist.isNotBlank()) append("&artist_name=").append(enc(cleanArtist))
            }
            result = Http.get(searchUrl)?.let { body ->
                runCatching {
                    val arr = JSONArray(body)
                    val items = List(arr.length()) { arr.getJSONObject(it) }
                    val best = items.firstOrNull { it.optString("syncedLyrics").isNotBlank() }
                        ?: items.firstOrNull { it.optString("plainLyrics").isNotBlank() }
                    best?.let { toState(it) }
                }.getOrNull()
            }
        }

        val final = result ?: LyricsState.NotFound
        if (final is LyricsState.Synced || final is LyricsState.Plain) cache.put(key, final)
        return final
    }

    private fun toState(json: JSONObject): LyricsState {
        if (json.optBoolean("instrumental", false)) return LyricsState.NotFound
        val synced = json.optString("syncedLyrics")
        if (synced.isNotBlank() && synced != "null") {
            val lines = LrcParser.parse(synced)
            if (lines.isNotEmpty()) return LyricsState.Synced(lines)
        }
        val plain = json.optString("plainLyrics")
        if (plain.isNotBlank() && plain != "null") return LyricsState.Plain(plain)
        return LyricsState.NotFound
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
