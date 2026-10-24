package com.cmstudio.apnaplayer.engine

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Pure Kotlin media player bridge powered by Google Media3 ExoPlayer.
 * Completely replaces native C/C++ libmpv with zero JNI overhead.
 */
object PlayerBridge {

  interface EventObserver {
    fun eventProperty(property: String) {}
    fun eventProperty(property: String, value: Long) {}
    fun eventProperty(property: String, value: Boolean) {}
    fun eventProperty(property: String, value: String) {}
    fun eventProperty(property: String, value: Double) {}
    fun eventProperty(property: String, value: PlayerNode) {}
    fun event(eventId: Int, data: PlayerNode) {}
    fun event(eventId: Int) {}
  }

  object PlayerEvent {
    const val MPV_EVENT_NONE = 0
    const val MPV_EVENT_SHUTDOWN = 1
    const val MPV_EVENT_LOG_MESSAGE = 2
    const val MPV_EVENT_GET_PROPERTY_REPLY = 3
    const val MPV_EVENT_SET_PROPERTY_REPLY = 4
    const val MPV_EVENT_COMMAND_REPLY = 5
    const val MPV_EVENT_START_FILE = 6
    const val MPV_EVENT_END_FILE = 7
    const val MPV_EVENT_FILE_LOADED = 8
    const val MPV_EVENT_TRACKS_CHANGED = 9
    const val MPV_EVENT_TRACK_SWITCHED = 10
    const val MPV_EVENT_IDLE = 11
    const val MPV_EVENT_PAUSE = 12
    const val MPV_EVENT_UNPAUSE = 13
    const val MPV_EVENT_TICK = 14
    const val MPV_EVENT_SCRIPT_INPUT_DISPATCH = 15
    const val MPV_EVENT_CLIENT_MESSAGE = 16
    const val MPV_EVENT_VIDEO_RECONFIG = 17
    const val MPV_EVENT_AUDIO_RECONFIG = 18
    const val MPV_EVENT_METADATA_UPDATE = 19
    const val MPV_EVENT_SEEK = 20
    const val MPV_EVENT_PLAYBACK_RESTART = 21
    const val MPV_EVENT_PROPERTY_CHANGE = 22
    const val MPV_EVENT_QUEUE_OVERFLOW = 24
    const val MPV_EVENT_HOOK = 25
  }

  val MpvEvent = PlayerEvent

  object PlayerFormat {
    const val MPV_FORMAT_NONE = 0
    const val MPV_FORMAT_STRING = 1
    const val MPV_FORMAT_OSD_STRING = 2
    const val MPV_FORMAT_FLAG = 3
    const val MPV_FORMAT_INT64 = 4
    const val MPV_FORMAT_DOUBLE = 5
    const val MPV_FORMAT_NODE = 6
    const val MPV_FORMAT_NODE_ARRAY = 7
    const val MPV_FORMAT_NODE_MAP = 8
    const val MPV_FORMAT_BYTE_ARRAY = 9
  }

  val MpvFormat = PlayerFormat

  class PropertyMap<T>(private val defaultFactory: (String) -> T) {
    private val store = ConcurrentHashMap<String, MutableStateFlow<T>>()

    operator fun get(key: String): MutableStateFlow<T> {
      return store.computeIfAbsent(key) { MutableStateFlow(defaultFactory(key)) }
    }

    fun getOrNull(key: String): MutableStateFlow<T>? = store[key]

    fun put(key: String, value: T) {
      get(key).value = value
    }
  }

  val propBoolean = PropertyMap<Boolean> { key ->
    when (key) {
      "pause" -> false
      "paused-for-cache" -> false
      else -> false
    }
  }

  val propInt = PropertyMap<Int> { key ->
    when (key) {
      "volume" -> 100
      "aid", "sid" -> 1
      else -> 0
    }
  }

  val propFloat = PropertyMap<Float> { key ->
    when (key) {
      "speed" -> 1.0f
      else -> 0f
    }
  }

  val propDouble = PropertyMap<Double> { key ->
    when (key) {
      "speed" -> 1.0
      "video-params/aspect" -> 1.7777777777777777
      else -> 0.0
    }
  }

  val propString = PropertyMap<String> { key ->
    when (key) {
      "hwdec", "hwdec-current" -> "mediacodec"
      "sub-justify" -> "auto"
      "sub-border-style" -> "outline-and-shadow"
      else -> ""
    }
  }

  val propNode = PropertyMap<PlayerNode?> { PlayerNode(null, "[]") }

  private val observers = CopyOnWriteArrayList<EventObserver>()
  private val mainHandler: Handler? by lazy {
    runCatching { Handler(Looper.getMainLooper()) }.getOrNull()
  }
  private val options = ConcurrentHashMap<String, String>()

  fun addObserver(observer: EventObserver) {
    if (!observers.contains(observer)) {
      observers.add(observer)
    }
  }

  fun removeObserver(observer: EventObserver) {
    observers.remove(observer)
  }

  private inline fun dispatchToMain(crossinline action: () -> Unit) {
    val looper = runCatching { Looper.myLooper() }.getOrNull()
    val mainLooper = runCatching { Looper.getMainLooper() }.getOrNull()
    val handler = mainHandler
    if (looper != null && mainLooper != null && looper != mainLooper && handler != null) {
      handler.post { action() }
    } else {
      action()
    }
  }

  fun notifyProperty(property: String, value: Boolean) {
    dispatchToMain { observers.forEach { runCatching { it.eventProperty(property, value) } } }
  }

  fun notifyProperty(property: String, value: Long) {
    dispatchToMain { observers.forEach { runCatching { it.eventProperty(property, value) } } }
  }

  fun notifyProperty(property: String, value: Double) {
    dispatchToMain { observers.forEach { runCatching { it.eventProperty(property, value) } } }
  }

  fun notifyProperty(property: String, value: String) {
    dispatchToMain { observers.forEach { runCatching { it.eventProperty(property, value) } } }
  }

  fun notifyProperty(property: String, value: PlayerNode) {
    dispatchToMain { observers.forEach { runCatching { it.eventProperty(property, value) } } }
  }

  fun notifyEvent(eventId: Int) {
    dispatchToMain {
      observers.forEach {
        runCatching {
          it.event(eventId)
          it.event(eventId, PlayerNode())
        }
      }
    }
  }

  fun command(vararg args: String) {
    if (args.isEmpty()) return
    val cmd = args[0]
    when (cmd) {
      "seek" -> {
        if (args.size >= 2) {
          val amountStr = args[1]
          val mode = if (args.size >= 3) args[2] else "relative"
          val amount = amountStr.toDoubleOrNull() ?: return
          if (mode.contains("percent")) {
            val durSec = propDouble["duration"].value.takeIf { it > 0.0 }
              ?: propInt["duration"].value.toDouble()
            val targetSec = (amount / 100.0) * durSec
            ExoPlayerHolder.seekTo(targetSec, isRelative = false)
          } else {
            val isRelative = mode.contains("relative")
            ExoPlayerHolder.seekTo(amount, isRelative)
          }
        }
      }
      "no-osd" -> {
        if (args.size >= 2) {
          when (args[1]) {
            "frame-step" -> ExoPlayerHolder.stepFrame(forward = true)
            "frame-back-step" -> ExoPlayerHolder.stepFrame(forward = false)
          }
        }
      }
      "frame-step" -> ExoPlayerHolder.stepFrame(forward = true)
      "frame-back-step" -> ExoPlayerHolder.stepFrame(forward = false)
      "sub-remove" -> {
        if (args.size >= 2) {
          val id = args[1].toIntOrNull() ?: -1
          ExoPlayerHolder.removeSubtitle(id)
        }
      }
      "vf" -> {
        if (args.size >= 3) {
          val action = args[1]
          val filter = args[2]
          if (filter.contains("hflip")) {
            ExoPlayerHolder.isHorizontalFlipped = (action == "add")
            ExoPlayerHolder.applyVideoTransform()
          } else if (filter.contains("vflip")) {
            ExoPlayerHolder.isVerticalFlipped = (action == "add")
            ExoPlayerHolder.applyVideoTransform()
          }
        }
      }
      "screenshot-to-file" -> {
        if (args.size >= 2) {
          ExoPlayerHolder.takeScreenshot(args[1])
        }
      }
      "multiply" -> {
        if (args.size >= 3 && args[1] == "volume") {
          val mult = args[2].toFloatOrNull() ?: 1f
          val current = propInt["volume"].value
          val newVol = (current * mult).toInt().coerceIn(0, 200)
          setPropertyInt("volume", newVol)
        }
      }
      "loadfile" -> {
        if (args.size >= 2) {
          ExoPlayerHolder.playFile(pathOrUri = args[1])
        }
      }
      "stop" -> {
        ExoPlayerHolder.stop()
      }
      "quit" -> {
        ExoPlayerHolder.stop()
        ExoPlayerHolder.release()
      }
      "sub-add" -> {
        if (args.size >= 2) {
          val select = args.getOrNull(2) != "auto"
          val title = args.getOrNull(3)
          ExoPlayerHolder.addExternalSubtitle(args[1], title = title, select = select)
        }
      }
      "audio-add" -> {
        if (args.size >= 2) {
          val title = args.getOrNull(3)
          ExoPlayerHolder.addExternalAudio(args[1], title = title)
        }
      }
    }
  }

  fun setPropertyBoolean(name: String, value: Boolean) {
    propBoolean[name].value = value
    when (name) {
      "pause" -> if (value) ExoPlayerHolder.pause() else ExoPlayerHolder.resume()
    }
    notifyProperty(name, value)
  }

  fun getPropertyBoolean(name: String): Boolean? {
    return propBoolean.getOrNull(name)?.value
  }

  fun setPropertyInt(name: String, value: Int) {
    propInt[name].value = value
    when (name) {
      "volume" -> ExoPlayerHolder.setVolume(value)
      "aid" -> ExoPlayerHolder.selectTrack("audio", value)
      "sid" -> ExoPlayerHolder.selectTrack("sub", value)
      "vid" -> ExoPlayerHolder.selectTrack("video", value)
      "chapter" -> {
        val chapters = (propNode["chapter-list"].value?.value as? List<*>)
        val targetTime = (chapters?.getOrNull(value) as? app.marlboroadvance.mpvex.ui.player.ChapterNode)?.time?.toDouble()
        if (targetTime != null) {
          ExoPlayerHolder.seekTo(targetTime, isRelative = false)
        }
      }
    }
    notifyProperty(name, value.toLong())
  }

  fun getPropertyInt(name: String): Int? {
    return propInt.getOrNull(name)?.value
  }

  fun setPropertyFloat(name: String, value: Float) {
    propFloat[name].value = value
    when (name) {
      "speed" -> ExoPlayerHolder.setPlaybackSpeed(value)
      "video-zoom" -> ExoPlayerHolder.setVideoZoom(value)
      "video-pan-x" -> ExoPlayerHolder.setVideoPanX(value)
      "video-pan-y" -> ExoPlayerHolder.setVideoPanY(value)
    }
    notifyProperty(name, value.toDouble())
  }

  fun getPropertyFloat(name: String): Float? {
    return propFloat.getOrNull(name)?.value
  }

  fun setPropertyDouble(name: String, value: Double) {
    propDouble[name].value = value
    when (name) {
      "speed" -> ExoPlayerHolder.setPlaybackSpeed(value.toFloat())
      "video-zoom" -> ExoPlayerHolder.setVideoZoom(value.toFloat())
      "video-pan-x" -> ExoPlayerHolder.setVideoPanX(value.toFloat())
      "video-pan-y" -> ExoPlayerHolder.setVideoPanY(value.toFloat())
      "video-aspect-override" -> {
        if (value > 0.0) {
          ExoPlayerHolder.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL)
        } else {
          ExoPlayerHolder.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT)
        }
      }
      "panscan" -> {
        if (value >= 0.9) {
          ExoPlayerHolder.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
        } else {
          ExoPlayerHolder.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT)
        }
      }
    }
    notifyProperty(name, value)
  }

  fun getPropertyDouble(name: String): Double? {
    return propDouble.getOrNull(name)?.value
  }

  fun setPropertyString(name: String, value: String) {
    propString[name].value = value
    when (name) {
      "hwdec" -> {
        propString["hwdec-current"].value = value
        notifyProperty("hwdec-current", value)
      }
      "aid" -> {
        if (value == "no" || value == "-1") {
          ExoPlayerHolder.selectTrack("audio", -1)
        } else {
          value.toIntOrNull()?.let { ExoPlayerHolder.selectTrack("audio", it) }
        }
      }
      "sid" -> {
        if (value == "no") {
          ExoPlayerHolder.selectTrack("sub", -1)
        } else {
          value.toIntOrNull()?.let { ExoPlayerHolder.selectTrack("sub", it) }
        }
      }
      "vid" -> {
        if (value == "no") {
          ExoPlayerHolder.selectTrack("video", -1)
        } else {
          value.toIntOrNull()?.let { ExoPlayerHolder.selectTrack("video", it) }
        }
      }
    }
    notifyProperty(name, value)
  }

  fun getPropertyString(name: String): String? {
    return propString.getOrNull(name)?.value
  }

  fun setOptionString(name: String, value: String) {
    options[name] = value
    when (name) {
      "speed" -> value.toFloatOrNull()?.let { ExoPlayerHolder.setPlaybackSpeed(it) }
      "volume-max" -> {}
    }
  }

  fun observeProperty(name: String, format: Int) {
    // Registered for reactive updates
  }

  fun grabThumbnail(dimension: Int): Bitmap? {
    // Video frame thumbnail handled via MediaMetadataRetriever
    return null
  }

  fun destroy() {
    ExoPlayerHolder.stop()
    ExoPlayerHolder.release()
    notifyEvent(PlayerEvent.MPV_EVENT_SHUTDOWN)
  }
}
