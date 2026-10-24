package app.marlboroadvance.mpvex.domain.download

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
  PENDING,
  DOWNLOADING,
  PAUSED,
  COMPLETED,
  FAILED,
  CANCELLED
}

@Serializable
data class DownloadTask(
  val id: String,
  val url: String,
  val fileName: String,
  val filePath: String,
  val totalBytes: Long = -1L,
  val downloadedBytes: Long = 0L,
  val speedBytesPerSec: Long = 0L,
  val status: DownloadStatus = DownloadStatus.PENDING,
  val errorMessage: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
) {
  val progressFraction: Float
    get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

  val progressPercent: Int
    get() = (progressFraction * 100).toInt()
}

fun formatDownloadBytes(bytes: Long): String {
  if (bytes <= 0L) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
  val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
  return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}

fun formatDownloadSpeed(bytesPerSec: Long): String {
  return "${formatDownloadBytes(bytesPerSec)}/s"
}
