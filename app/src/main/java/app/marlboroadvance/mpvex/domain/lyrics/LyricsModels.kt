package app.marlboroadvance.mpvex.domain.lyrics

import androidx.compose.runtime.Immutable

/**
 * Single line in a time-synchronized lyric (.lrc).
 */
@Immutable
data class LyricLine(
  val timestampMs: Long,
  val text: String,
)

/**
 * Lyrics model containing both timestamp-synchronized lines and plain lyrics.
 */
@Immutable
data class SongLyrics(
  val trackName: String,
  val artistName: String,
  val syncedLyrics: List<LyricLine> = emptyList(),
  val plainLyrics: String? = null,
  val isSynced: Boolean = syncedLyrics.isNotEmpty(),
)
