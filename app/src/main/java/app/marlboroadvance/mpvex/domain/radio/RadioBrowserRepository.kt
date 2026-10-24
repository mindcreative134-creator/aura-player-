package app.marlboroadvance.mpvex.domain.radio

import app.marlboroadvance.mpvex.domain.common.Http
import org.json.JSONArray
import java.net.URLEncoder

/** Radio Browser API (https://www.radio-browser.info) - free, legal community radio directory. */
class RadioBrowserRepository {

    // Public mirrors; if one fails, the next is tried.
    private val servers = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    /** tag = "bollywood", "retro", "lofi" ... ; query empty searches by tag only. */
    suspend fun search(query: String = "", tag: String? = null, limit: Int = 50): List<RadioStation> {
        val params = buildString {
            if (query.isNotBlank()) append("name=").append(enc(query)).append('&')
            if (!tag.isNullOrBlank()) append("tag=").append(enc(tag)).append('&')
            append("limit=").append(limit)
            append("&hidebroken=true&order=clickcount&reverse=true")
        }
        return request("/json/stations/search?$params")
    }

    suspend fun topStations(limit: Int = 50): List<RadioStation> =
        request("/json/stations/topclick/$limit")

    suspend fun registerClick(stationId: String) {
        if (stationId.isBlank()) return
        Http.get("${servers.first()}/json/url/$stationId")
    }

    /**
     * Resolves .m3u / .pls links to actual stream URLs (ExoPlayer cannot directly parse plain M3U/PLS files).
     * .m3u8 (HLS) and direct stream URLs are returned as-is.
     */
    suspend fun resolveStreamUrl(input: String): String {
        val url = input.trim()
        val path = url.substringBefore('?').lowercase()
        if (!(path.endsWith(".m3u") || path.endsWith(".pls"))) return url
        val body = Http.get(url) ?: return url
        val resolved = if (path.endsWith(".pls")) {
            body.lineSequence().map { it.trim() }
                .firstOrNull { it.startsWith("File1=", ignoreCase = true) }
                ?.substringAfter('=')
        } else {
            body.lineSequence().map { it.trim() }
                .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        }
        return resolved?.takeIf { it.isNotBlank() } ?: url
    }

    private suspend fun request(pathAndQuery: String): List<RadioStation> {
        for (server in servers) {
            val body = Http.get(server + pathAndQuery, timeoutMs = 12_000) ?: continue
            val parsed = runCatching { parse(body) }.getOrNull()
            if (parsed != null) return parsed
        }
        return emptyList()
    }

    private fun parse(body: String): List<RadioStation> {
        val arr = JSONArray(body)
        val out = mutableListOf<RadioStation>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val url = o.optString("url_resolved").ifBlank { o.optString("url") }
            if (url.isBlank()) continue
            out += RadioStation(
                id = o.optString("stationuuid"),
                name = o.optString("name").trim().ifBlank { "Unnamed station" },
                url = url,
                tags = o.optString("tags").split(',').filter { it.isNotBlank() }.take(3).joinToString(" • "),
                country = o.optString("country")
            )
        }
        return out.distinctBy { it.url }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
