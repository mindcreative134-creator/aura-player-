package app.marlboroadvance.mpvex.domain.music

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MusicSong(
  val id: Long,
  val title: String,
  val artist: String,
  val album: String,
  val albumId: Long,
  val durationMs: Long,
  val path: String,
  val uri: Uri,
  val dateAdded: Long,
  val trackNumber: Int = 0,
  val year: Int = 0,
  val albumArtUri: Uri? = null,
  val size: Long = 0L,
)

@Immutable
data class MusicAlbum(
  val id: Long,
  val title: String,
  val artist: String,
  val songCount: Int,
  val year: Int = 0,
  val albumArtUri: Uri? = null,
)

@Immutable
data class MusicArtist(
  val id: Long,
  val name: String,
  val songCount: Int,
  val albumCount: Int = 0,
)
