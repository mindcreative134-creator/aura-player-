package app.marlboroadvance.mpvex.domain.radio

import androidx.compose.runtime.Immutable

/**
 * Live online radio station model from Radio Browser API.
 */
@Immutable
data class RadioStation(
  val id: String,
  val name: String,
  val url: String,
  val tags: String = "",
  val country: String = "",
  val favicon: String? = null,
  val bitrate: Int = 0,
  val codec: String = "",
) {
  val stationUuid: String get() = id
  val streamUrl: String get() = url
}
