package com.cmstudio.apnaplayer.engine

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import java.io.File
import java.util.Locale
import kotlin.math.abs

object PlayerBridgeUtils {
  @JvmField
  val PROTOCOLS: Array<String> = arrayOf(
    "http",
    "https",
    "ftp",
    "ftps",
    "rtmp",
    "rtmps",
    "rtsp",
    "mms",
    "mmst",
    "mmsh",
    "smb",
    "sftp"
  )

  data class Versions(
    val playerEngine: String = "Apna Engine 1.3.1",
    val mediaPipeline: String = "Google Media3 ExoPlayer",
    val surfaceRenderer: String = "Hardware TextureSurface"
  )

  @JvmField
  val VERSIONS: Versions = Versions()

  @JvmStatic
  fun prettyTime(d: Int, isInverted: Boolean = false): String {
    val prefix = if (isInverted) "-" else ""
    val totalSeconds = abs(d)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
      String.format(Locale.US, "%s%d:%02d:%02d", prefix, hours, minutes, seconds)
    } else {
      String.format(Locale.US, "%s%02d:%02d", prefix, minutes, seconds)
    }
  }

  @JvmStatic
  fun findRealPath(fd: Int): String? {
    return runCatching {
      val link = File("/proc/self/fd/$fd")
      if (link.exists()) {
        link.canonicalPath
      } else {
        null
      }
    }.getOrNull()
  }

  @JvmStatic
  fun copyAssets(context: Context) {
    // Media3 ExoPlayer does not require native asset extraction
  }

  @JvmStatic
  inline fun <reified T : Parcelable> getParcelableArray(bundle: Bundle, key: String): Array<T> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      bundle.getParcelableArray(key, T::class.java) ?: emptyArray()
    } else {
      @Suppress("DEPRECATION")
      val raw = bundle.getParcelableArray(key)
      if (raw != null) {
        val typedList = mutableListOf<T>()
        for (item in raw) {
          if (item is T) typedList.add(item)
        }
        typedList.toTypedArray()
      } else {
        emptyArray()
      }
    }
  }
}
