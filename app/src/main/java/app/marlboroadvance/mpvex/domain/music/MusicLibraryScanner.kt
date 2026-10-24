package app.marlboroadvance.mpvex.domain.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MusicLibraryScanner {
  private const val TAG = "MusicLibraryScanner"
  private val ALBUM_ART_BASE_URI = Uri.parse("content://media/external/audio/albumart")

  suspend fun scanSongs(context: Context): List<MusicSong> = withContext(Dispatchers.IO) {
    val songs = mutableListOf<MusicSong>()
    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.ALBUM_ID,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.DATA,
      MediaStore.Audio.Media.DATE_ADDED,
      MediaStore.Audio.Media.TRACK,
      MediaStore.Audio.Media.YEAR,
      MediaStore.Audio.Media.SIZE,
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 1000"
    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    try {
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        sortOrder,
      )?.use { cursor ->
        val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
        val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
        val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
        val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

        while (cursor.moveToNext()) {
          val id = if (idCol >= 0) cursor.getLong(idCol) else continue
          val path = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
          val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
          val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
          
          val file = path.takeIf { it.isNotBlank() }?.let(::File)
          val title = if (titleCol >= 0) cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
            ?: file?.nameWithoutExtension ?: "Track $id" else "Track $id"
          val artist = if (artistCol >= 0) cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Artist" else "Unknown Artist"
          val album = if (albumCol >= 0) cursor.getString(albumCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown Album" else "Unknown Album"
          val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
          val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L
          val track = if (trackCol >= 0) cursor.getInt(trackCol) else 0
          val year = if (yearCol >= 0) cursor.getInt(yearCol) else 0

          val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
          val albumArtUri = if (albumId > 0) ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId) else null

          songs.add(
            MusicSong(
              id = id,
              title = title,
              artist = artist,
              album = album,
              albumId = albumId,
              durationMs = duration,
              path = path,
              uri = contentUri,
              dateAdded = dateAdded,
              trackNumber = track,
              year = year,
              albumArtUri = albumArtUri,
              size = size,
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error scanning audio library", e)
    }

    songs
  }

  fun groupIntoAlbums(songs: List<MusicSong>): List<MusicAlbum> {
    return songs.groupBy { it.album }
      .map { (albumTitle, albumSongs) ->
        val first = albumSongs.first()
        MusicAlbum(
          id = first.albumId,
          title = albumTitle,
          artist = first.artist,
          songCount = albumSongs.size,
          year = albumSongs.maxOfOrNull { it.year } ?: 0,
          albumArtUri = first.albumArtUri,
        )
      }
      .sortedBy { it.title.lowercase() }
  }

  fun groupIntoArtists(songs: List<MusicSong>): List<MusicArtist> {
    return songs.groupBy { it.artist }
      .entries
      .mapIndexed { index, entry ->
        val artistName = entry.key
        val artistSongs = entry.value
        val albumCount = artistSongs.map { it.album }.distinct().size
        MusicArtist(
          id = index.toLong(),
          name = artistName,
          songCount = artistSongs.size,
          albumCount = albumCount,
        )
      }
      .sortedBy { it.name.lowercase() }
  }
}
