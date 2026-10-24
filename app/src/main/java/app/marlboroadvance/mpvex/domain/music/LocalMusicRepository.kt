package app.marlboroadvance.mpvex.domain.music

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val artUri: Uri
)

/** MediaStore local audio (.mp3 .m4a .flac .wav .ogg .opus) scanner. */
class LocalMusicRepository(private val context: Context) {

    private val allowedExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "opus")

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= 29)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        // Skip ringtone/notification/alarm, ignore clips shorter than 30s
        val selection = "${MediaStore.Audio.Media.DURATION} >= 30000 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0"

        val songs = mutableListOf<Song>()
        try {
            context.contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val ext = c.getString(nameCol).orEmpty().substringAfterLast('.', "").lowercase()
                    if (ext !in allowedExtensions) continue
                    val id = c.getLong(idCol)
                    val albumId = c.getLong(albumIdCol)
                    songs += Song(
                        id = id,
                        title = c.getString(titleCol).orEmpty().ifBlank { "Unknown" },
                        artist = c.getString(artistCol).orEmpty().cleanUnknown("Unknown artist"),
                        album = c.getString(albumCol).orEmpty().cleanUnknown("Unknown album"),
                        albumId = albumId,
                        durationMs = c.getLong(durCol),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalMusicRepo", "Error scanning local songs", e)
        }
        songs
    }

    private fun String.cleanUnknown(fallback: String) =
        if (isBlank() || this == "<unknown>") fallback else this

    companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

/** Album art loader + in-memory cache for fast Compose UI rendering. */
class AlbumArtCache(private val context: Context) {
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 16).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun load(songUri: Uri?, albumId: Long, sizePx: Int): Bitmap? {
        if (songUri == null || songUri.scheme != "content") return null
        val key = (if (albumId > 0) "a$albumId" else songUri.toString()) + "@$sizePx"
        cache.get(key)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    context.contentResolver.loadThumbnail(songUri, Size(sizePx, sizePx), null)
                } else {
                    val art = ContentUris.withAppendedId(LocalMusicRepository.ALBUM_ART_URI, albumId)
                    context.contentResolver.openInputStream(art)?.use { BitmapFactory.decodeStream(it) }
                }
            }.getOrNull()
        }
        if (bitmap != null) cache.put(key, bitmap)
        return bitmap
    }
}

/** SharedPreferences JSON playlist store: { "name": [songId, ...] } */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("apna_playlists", Context.MODE_PRIVATE)

    fun load(): Map<String, List<Long>> {
        val json = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        val result = linkedMapOf<String, List<Long>>()
        json.keys().forEach { name ->
            val arr = json.getJSONArray(name)
            result[name] = List(arr.length()) { arr.getLong(it) }
        }
        return result
    }

    fun save(playlists: Map<String, List<Long>>) {
        val json = JSONObject()
        playlists.forEach { (name, ids) -> json.put(name, JSONArray(ids)) }
        prefs.edit().putString(KEY, json.toString()).apply()
    }

    private companion object {
        const val KEY = "playlists_json"
    }
}
