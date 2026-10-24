package app.marlboroadvance.mpvex.domain.subtitle

import kotlinx.serialization.Serializable

@Serializable
data class OnlineSubtitle(
  val id: String = "",
  val displayName: String = "",
  val displayLanguage: String = "English",
  val language: String = "en",
  val url: String = "",
  val format: String = "srt",
  val source: String? = null,
  val fileName: String? = null,
  val release: String? = null,
  val isHashMatch: Boolean = false,
  val isHearingImpaired: Boolean = false,
  val downloadCount: Int = 0,
  val metadata: Map<String, String> = emptyMap(),
)

data class OnlineSubtitleSearchRequest(
  val query: String,
  val season: Int? = null,
  val episode: Int? = null,
  val movieHash: String? = null,
)

