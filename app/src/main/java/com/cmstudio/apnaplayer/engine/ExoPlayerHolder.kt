package com.cmstudio.apnaplayer.engine

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import app.marlboroadvance.mpvex.ui.player.TrackNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Core Google Media3 ExoPlayer Engine Holder.
 * Manages player lifecycle, hardware-accelerated playback, state sync,
 * track selection, and video aspect ratios.
 */
object ExoPlayerHolder {
  private const val TAG = "AuraPlayerEngine"

  private var exoPlayer: ExoPlayer? = null
  private var trackSelector: DefaultTrackSelector? = null
  private var activePlayerView: PlayerView? = null
  private var cachedMediaSourceFactory: DefaultMediaSourceFactory? = null
  private var progressJob: Job? = null
  private var bufferingJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Main + Job())
  private val mainHandler = Handler(Looper.getMainLooper())
  private var appContext: Context? = null
  private var hasDispatchedFileLoaded = false
  var currentSource: String? = null
    private set

  private object Media3DiskCache {
    @Volatile private var cache: SimpleCache? = null
    @Synchronized
    fun get(context: Context): SimpleCache {
      return cache ?: SimpleCache(
        File(context.applicationContext.cacheDir, "aura_media3_cache"),
        LeastRecentlyUsedCacheEvictor(512L * 1024L * 1024L),
        StandaloneDatabaseProvider(context.applicationContext),
      ).also { cache = it }
    }
  }

  @Volatile private var currentVideoZoom: Float = 0f
  @Volatile private var currentVideoPanX: Float = 0f
  @Volatile private var currentVideoPanY: Float = 0f
  @Volatile var isHorizontalFlipped: Boolean = false
  @Volatile var isVerticalFlipped: Boolean = false

  var currentPlayerView: PlayerView?
    get() = activePlayerView
    set(value) {
      activePlayerView = value
      exoPlayer?.let { player ->
        if (value != null && value.player !== player) {
          value.player = player
        }
      }
      applyVideoTransform()
    }

  fun getPlayer(): ExoPlayer? = exoPlayer

  val audioSessionId: Int
    get() = exoPlayer?.audioSessionId ?: 0

  val videoSize: Pair<Int, Int>
    get() {
      val format = exoPlayer?.videoFormat
      return if (format != null && format.width > 0 && format.height > 0) {
        Pair(format.width, format.height)
      } else {
        val w = PlayerBridge.getPropertyInt("video-params/w") ?: 0
        val h = PlayerBridge.getPropertyInt("video-params/h") ?: 0
        Pair(w, h)
      }
    }

  val videoFormatInfo: String
    get() {
      val format = exoPlayer?.videoFormat
      if (format != null) {
        val mime = format.sampleMimeType ?: "video"
        val codec = when {
          mime.contains("hevc") || mime.contains("h265") -> "HEVC / H.265"
          mime.contains("avc") || mime.contains("h264") -> "AVC / H.264"
          mime.contains("av01") || mime.contains("av1") -> "AV1"
          mime.contains("vp9") -> "VP9"
          else -> mime.substringAfterLast('/').uppercase()
        }
        val isHdr = format.colorInfo != null && (format.colorInfo?.colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_ST2084 || format.colorInfo?.colorTransfer == androidx.media3.common.C.COLOR_TRANSFER_HLG)
        val color = if (isHdr) "HDR10" else "SDR"
        return "$codec · $color"
      }
      val mpvCodec = PlayerBridge.getPropertyString("video-codec")?.uppercase() ?: "HW+"
      return "$mpvCodec · SDR"
    }

  val audioFormatInfo: String
    get() {
      val format = exoPlayer?.audioFormat
      if (format != null) {
        val channels = when (format.channelCount) {
          1 -> "Mono"
          2 -> "Stereo"
          6 -> "5.1 Surround"
          else -> "${format.channelCount}ch"
        }
        val mime = format.sampleMimeType?.substringAfterLast('/')?.uppercase() ?: "AAC"
        return "$mime · $channels"
      }
      return PlayerBridge.getPropertyString("audio-codec")?.uppercase() ?: "Stereo"
    }

  data class PlaybackDiagnostics(
    val resolution: String = "N/A",
    val videoCodec: String = "Unknown",
    val activeVideoDecoder: String = "MediaCodec (HW)",
    val videoFps: Float = 0f,
    val videoBitrateKbps: Int = 0,
    val audioCodec: String = "Unknown",
    val activeAudioDecoder: String = "MediaCodec",
    val audioChannels: String = "Stereo",
    val audioSampleRate: Int = 0,
    val audioBitrateKbps: Int = 0,
    val droppedFrames: Long = 0L,
    val bufferDurationMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val hardwareAccelerated: Boolean = true,
    val thermalState: String = "Normal",
    val colorInfo: String = "SDR",
    val lastError: String? = null,
  )

  @Volatile var activeVideoDecoder: String = "Auto MediaCodec"
  @Volatile var activeAudioDecoder: String = "Auto MediaCodec"
  @Volatile var totalDroppedFrames: Long = 0L
  @Volatile var lastPlaybackError: String? = null

  fun resetFrameCounters() {
    totalDroppedFrames = 0L
    lastPlaybackError = null
  }

  fun getDiagnostics(context: Context? = appContext): PlaybackDiagnostics {
    val player = exoPlayer
    val vFormat = player?.videoFormat
    val aFormat = player?.audioFormat
    val (w, h) = videoSize
    val res = if (w > 0 && h > 0) "${w}×${h}" else "Active Stream"
    val isHdr = vFormat?.colorInfo != null && (vFormat.colorInfo?.colorTransfer == C.COLOR_TRANSFER_ST2084 || vFormat.colorInfo?.colorTransfer == C.COLOR_TRANSFER_HLG)

    val thermal = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && context != null) {
      val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
      when (pm?.currentThermalStatus) {
        android.os.PowerManager.THERMAL_STATUS_NONE -> "Nominal"
        android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Light"
        android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
        android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Throttling"
        android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        else -> "Normal"
      }
    } else "Normal"

    val vBitrate = (vFormat?.bitrate?.takeIf { it > 0 } ?: 0) / 1000
    val aBitrate = (aFormat?.bitrate?.takeIf { it > 0 } ?: 0) / 1000

    val vCodec = if (vFormat != null) {
      val mime = vFormat.sampleMimeType ?: "video"
      when {
        mime.contains("hevc") || mime.contains("h265") -> "HEVC / H.265"
        mime.contains("avc") || mime.contains("h264") -> "AVC / H.264"
        mime.contains("av01") || mime.contains("av1") -> "AV1"
        mime.contains("vp9") -> "VP9"
        else -> mime.substringAfterLast('/').uppercase()
      }
    } else "MediaCodec HW+"

    val aCodec = if (aFormat != null) {
      val mime = aFormat.sampleMimeType ?: "audio"
      when {
        mime.contains("aac") -> "AAC"
        mime.contains("mp3") || mime.contains("mpeg") -> "MP3"
        mime.contains("opus") -> "Opus"
        mime.contains("flac") -> "FLAC"
        mime.contains("ac3") || mime.contains("eac3") -> "Dolby Digital (AC-3)"
        mime.contains("dts") -> "DTS Audio"
        else -> mime.substringAfterLast('/').uppercase()
      }
    } else "PCM / Native"

    val channels = when (aFormat?.channelCount) {
      1 -> "Mono (1.0)"
      2 -> "Stereo (2.0)"
      6 -> "5.1 Surround"
      8 -> "7.1 Surround"
      else -> if (aFormat?.channelCount != null) "${aFormat.channelCount} Channels" else "Stereo"
    }

    return PlaybackDiagnostics(
      resolution = res,
      videoCodec = vCodec,
      activeVideoDecoder = activeVideoDecoder,
      videoFps = if (vFormat != null && vFormat.frameRate > 0) vFormat.frameRate else 0f,
      videoBitrateKbps = vBitrate,
      audioCodec = aCodec,
      activeAudioDecoder = activeAudioDecoder,
      audioChannels = channels,
      audioSampleRate = aFormat?.sampleRate ?: 48000,
      audioBitrateKbps = aBitrate,
      droppedFrames = totalDroppedFrames,
      bufferDurationMs = player?.totalBufferedDuration ?: 0L,
      bufferedPercentage = player?.bufferedPercentage ?: 0,
      playbackSpeed = player?.playbackParameters?.speed ?: 1.0f,
      hardwareAccelerated = !activeVideoDecoder.contains("sw", ignoreCase = true) && !activeVideoDecoder.contains("software", ignoreCase = true),
      thermalState = thermal,
      colorInfo = if (isHdr) "HDR10 / HLG Wide Color" else "SDR (BT.709)",
      lastError = lastPlaybackError,
    )
  }

  @Synchronized
  fun getOrCreatePlayer(context: Context): ExoPlayer {
    val appCtx = context.applicationContext
    appContext = appCtx
    val existing = exoPlayer
    if (existing != null) return existing

    val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appCtx)
    val tryHw = prefs.getBoolean("try_hw_dec", true)
    val fallback = prefs.getBoolean("decoder_fallback_enabled", true)
    val tunneling = prefs.getBoolean("video_tunneling_enabled", false)

    val minBuffer = prefs.getInt("buffer_min_ms", 15000)
    val maxBuffer = prefs.getInt("buffer_max_ms", 120000)
    val playbackBuffer = prefs.getInt("buffer_playback_ms", 500)
    val rebufferMs = prefs.getInt("buffer_rebuffer_ms", 1000)

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
      .setAllowCrossProtocolRedirects(true)
      .setConnectTimeoutMs(15_000)
      .setReadTimeoutMs(30_000)

    val cacheDataSourceFactory = try {
      CacheDataSource.Factory()
        .setCache(Media3DiskCache.get(appCtx))
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setCacheReadDataSourceFactory(FileDataSource.Factory())
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to initialize Media3DiskCache: ${e.message}")
      null
    }

    val dataSourceFactory = DefaultDataSource.Factory(appCtx, cacheDataSourceFactory ?: httpDataSourceFactory)
    val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
    cachedMediaSourceFactory = mediaSourceFactory

    val renderersFactory = DefaultRenderersFactory(appCtx).apply {
      setEnableDecoderFallback(fallback)
      if (tryHw) {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
      } else {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
      }
    }

    val selector = DefaultTrackSelector(appCtx).apply {
      parameters = buildUponParameters()
        .setTunnelingEnabled(tunneling)
        .setPreferredTextLanguage(null)
        .setSelectUndeterminedTextLanguage(true)
        .build()
    }
    trackSelector = selector

    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        minBuffer.coerceIn(15_000, 30_000),
        maxBuffer.coerceIn(50_000, 120_000),
        playbackBuffer.coerceIn(500, 1500),
        rebufferMs.coerceIn(1000, 2000)
      )
      .setBackBuffer(15000, true)
      .setPrioritizeTimeOverSizeThresholds(true)
      .build()

    val player = ExoPlayer.Builder(appCtx, renderersFactory)
      .setTrackSelector(selector)
      .setLoadControl(loadControl)
      .setMediaSourceFactory(mediaSourceFactory)
      .setSeekBackIncrementMs(10000)
      .setSeekForwardIncrementMs(10000)
      .build()

    val audioAttributes = AudioAttributes.Builder()
      .setUsage(C.USAGE_MEDIA)
      .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
      .build()
    player.setAudioAttributes(audioAttributes, false)

    val hwdecVal = if (tryHw) "mediacodec" else "no"
    PlayerBridge.propString["hwdec"].value = hwdecVal
    PlayerBridge.propString["hwdec-current"].value = hwdecVal
    PlayerBridge.notifyProperty("hwdec", hwdecVal)
    PlayerBridge.notifyProperty("hwdec-current", hwdecVal)

    setupPlayerListener(player)
    exoPlayer = player
    startProgressTicker()

    return player
  }

  private fun setupPlayerListener(player: ExoPlayer) {
    player.addListener(object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
          Player.STATE_BUFFERING -> {
            bufferingJob?.cancel()
            bufferingJob = scope.launch {
              delay(150)
              if (player.playbackState == Player.STATE_BUFFERING) {
                PlayerBridge.propBoolean["paused-for-cache"].value = true
                PlayerBridge.notifyProperty("paused-for-cache", true)
              }
            }
          }
          Player.STATE_READY -> {
            bufferingJob?.cancel()
            PlayerBridge.propBoolean["paused-for-cache"].value = false
            PlayerBridge.notifyProperty("paused-for-cache", false)

            val durationSec = (player.duration.coerceAtLeast(0) / 1000).toInt()
            PlayerBridge.propInt["duration"].value = durationSec
            PlayerBridge.propDouble["duration"].value = player.duration.toDouble() / 1000.0
            PlayerBridge.notifyProperty("duration", durationSec.toLong())

            syncTracks(player.currentTracks)

            if (!hasDispatchedFileLoaded) {
              hasDispatchedFileLoaded = true
              PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_FILE_LOADED)
            }
            PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_PLAYBACK_RESTART)
          }
          Player.STATE_ENDED -> {
            PlayerBridge.propBoolean["eof-reached"].value = true
            PlayerBridge.notifyProperty("eof-reached", true)
            PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_END_FILE)
          }
          Player.STATE_IDLE -> {
            PlayerBridge.propBoolean["paused-for-cache"].value = false
          }
        }
      }

      override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        val isPaused = !playWhenReady
        PlayerBridge.propBoolean["pause"].value = isPaused
        PlayerBridge.notifyProperty("pause", isPaused)
      }

      override fun onVideoSizeChanged(videoSize: VideoSize) {
        if (videoSize.width > 0 && videoSize.height > 0) {
          val w = videoSize.width
          val h = videoSize.height
          val pixelRatio = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio.toDouble() else 1.0
          val aspect = (w.toDouble() / h.toDouble()) * pixelRatio

          PlayerBridge.propInt["video-params/w"].value = w
          PlayerBridge.propInt["video-params/h"].value = h
          PlayerBridge.propDouble["video-params/aspect"].value = aspect

          PlayerBridge.notifyProperty("video-params/w", w.toLong())
          PlayerBridge.notifyProperty("video-params/h", h.toLong())
          PlayerBridge.notifyProperty("video-params/aspect", aspect)
          applyVideoTransform()
        }
      }

      override fun onTracksChanged(tracks: Tracks) {
        syncTracks(tracks)
      }

      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        PlayerBridge.propFloat["speed"].value = playbackParameters.speed
        PlayerBridge.propDouble["speed"].value = playbackParameters.speed.toDouble()
        PlayerBridge.notifyProperty("speed", playbackParameters.speed.toDouble())
      }

      override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        Log.e(TAG, "ExoPlayer playback error: ${error.errorCodeName} (${error.errorCode})", error)
        bufferingJob?.cancel()
        PlayerBridge.propBoolean["paused-for-cache"].value = false
        PlayerBridge.notifyProperty("paused-for-cache", false)

        val current = currentSource
        if (current != null && !current.startsWith("content://") && (current.startsWith("file://") || current.startsWith("/"))) {
          val appC = appContext
          if (appC != null) {
            val resolved = resolvePlayableUri(appC, current)
            if (resolved.scheme == "content" && resolved.toString() != current) {
              Log.i(TAG, "Retrying playback with resolved MediaStore URI: $resolved")
              mainHandler.post {
                playFile(appC, resolved.toString())
              }
              return
            }
          }
        }
        lastPlaybackError = "${error.errorCodeName} (${error.errorCode}): ${error.message}"
        PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_END_FILE)
      }
    })

    player.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
      override fun onDroppedVideoFrames(
        eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
      ) {
        totalDroppedFrames += droppedFrames
      }

      override fun onVideoDecoderInitialized(
        eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
      ) {
        activeVideoDecoder = decoderName
      }

      override fun onAudioDecoderInitialized(
        eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
      ) {
        activeAudioDecoder = decoderName
      }
    })
  }

  private fun syncTracks(tracks: Tracks) {
    var trackCount = 0
    var audioCount = 0
    var subCount = 0
    val trackNodes = mutableListOf<TrackNode>()

    for (group in tracks.groups) {
      val type = group.type
      val trackTypeStr = when (type) {
        C.TRACK_TYPE_AUDIO -> "audio"
        C.TRACK_TYPE_TEXT -> "sub"
        C.TRACK_TYPE_VIDEO -> "video"
        else -> continue
      }

      for (i in 0 until group.length) {
        val format = group.getTrackFormat(i)
        val isSelected = group.isTrackSelected(i)
        val trackId = trackCount + 1

        val rawLang = format.language?.lowercase()?.trim()
        val lang = if (rawLang.isNullOrBlank() || rawLang == "und") "" else rawLang

        when (type) {
          C.TRACK_TYPE_AUDIO -> {
            audioCount++
            if (isSelected) {
              PlayerBridge.propInt["aid"].value = trackId
              PlayerBridge.propString["aid"].value = trackId.toString()
              PlayerBridge.notifyProperty("aid", trackId.toLong())
            }
          }
          C.TRACK_TYPE_TEXT -> {
            subCount++
            if (isSelected) {
              PlayerBridge.propInt["sid"].value = trackId
              PlayerBridge.propString["sid"].value = trackId.toString()
              PlayerBridge.notifyProperty("sid", trackId.toLong())
            }
          }
          C.TRACK_TYPE_VIDEO -> {
            if (isSelected) {
              PlayerBridge.propInt["vid"].value = trackId
              PlayerBridge.propString["vid"].value = trackId.toString()
              PlayerBridge.notifyProperty("vid", trackId.toLong())
            }
          }
        }

        val displayTitle = when {
          !format.label.isNullOrBlank() -> format.label!!
          lang.isNotBlank() -> Locale(lang).getDisplayLanguage(Locale.getDefault()).ifBlank { lang.uppercase(Locale.US) }
          type == C.TRACK_TYPE_AUDIO -> "Audio Track $audioCount"
          type == C.TRACK_TYPE_TEXT -> "Subtitle Track $subCount"
          else -> "$trackTypeStr #$trackId"
        }

        PlayerBridge.propInt["track-list/$trackCount/id"].value = trackId
        PlayerBridge.propString["track-list/$trackCount/type"].value = trackTypeStr
        PlayerBridge.propString["track-list/$trackCount/lang"].value = lang
        PlayerBridge.propString["track-list/$trackCount/title"].value = displayTitle
        PlayerBridge.propBoolean["track-list/$trackCount/selected"].value = isSelected

        trackNodes.add(
          TrackNode(
            id = trackId,
            type = trackTypeStr,
            title = displayTitle,
            lang = lang,
            selected = isSelected,
            codec = format.sampleMimeType,
            demuxW = format.width.takeIf { it > 0 }?.toLong(),
            demuxH = format.height.takeIf { it > 0 }?.toLong(),
            demuxFps = format.frameRate.takeIf { it > 0f }?.toDouble(),
            demuxBitrate = format.bitrate.takeIf { it > 0 }?.toLong(),
            demuxChannelCount = format.channelCount.takeIf { it > 0 }?.toLong(),
            demuxSampleRate = format.sampleRate.takeIf { it > 0 }?.toLong(),
            audioChannels = format.channelCount.takeIf { it > 0 }?.toLong(),
          )
        )
        trackCount++
      }
    }

    if (audioCount > 0 && PlayerBridge.propInt["aid"].value == null) {
      val firstAudio = trackNodes.firstOrNull { it.type == "audio" }
      if (firstAudio != null) {
        PlayerBridge.propInt["aid"].value = firstAudio.id
        PlayerBridge.propString["aid"].value = firstAudio.id.toString()
        PlayerBridge.notifyProperty("aid", firstAudio.id.toLong())
      }
    }

    PlayerBridge.propInt["track-list/count"].value = trackCount
    val jsonStr = try {
      kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(TrackNode.serializer()),
        trackNodes
      )
    } catch (_: Exception) {
      "[]"
    }
    PlayerBridge.propNode["track-list"].value = PlayerNode(trackNodes, jsonStr)
    PlayerBridge.notifyProperty("track-list", jsonStr)
    PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_TRACKS_CHANGED)
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob = scope.launch {
      while (isActive) {
        exoPlayer?.let { player ->
          if (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING) {
            val posMs = player.currentPosition.coerceAtLeast(0)
            val posSec = (posMs / 1000).toInt()
            val durMs = player.duration.coerceAtLeast(0)
            val durSec = (durMs / 1000).toInt()

            PlayerBridge.propInt["time-pos"].value = posSec
            PlayerBridge.propDouble["time-pos"].value = posMs.toDouble() / 1000.0

            if (durSec > 0) {
              PlayerBridge.propInt["duration"].value = durSec
              PlayerBridge.propDouble["duration"].value = durMs.toDouble() / 1000.0
            }

            // Automatic A-B loop repetition (safe guard against invalid/cleared loop)
            val loopAStr = PlayerBridge.propString["ab-loop-a"].value
            val loopBStr = PlayerBridge.propString["ab-loop-b"].value
            val loopA = PlayerBridge.propDouble["ab-loop-a"].value
            val loopB = PlayerBridge.propDouble["ab-loop-b"].value
            if (loopAStr != "no" && loopBStr != "no" && loopA >= 0.0 && loopB > loopA) {
              val currentSec = posMs.toDouble() / 1000.0
              if (currentSec >= loopB) {
                player.seekTo((loopA * 1000).toLong())
              }
            }
          }
        }
        delay(200)
      }
    }
  }

  fun resolvePlayableUri(context: Context, pathOrUri: String): Uri {
    val trimmed = pathOrUri.trim()
    if (trimmed.startsWith("content://") ||
      trimmed.startsWith("http://") ||
      trimmed.startsWith("https://") ||
      trimmed.startsWith("rtsp://") ||
      trimmed.startsWith("rtmp://")
    ) {
      return Uri.parse(trimmed)
    }

    val rawPath = when {
      trimmed.startsWith("file://") -> Uri.decode(trimmed.removePrefix("file://"))
      else -> Uri.decode(trimmed)
    }

    // Try finding the MediaStore content:// Uri for this filesystem path
    val contentUri = runCatching {
      val projection = arrayOf(MediaStore.Video.Media._ID)
      val selection = "${MediaStore.Video.Media.DATA} = ?"
      val selectionArgs = arrayOf(rawPath)
      context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
          ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        } else {
          null
        }
      }
    }.getOrNull()

    if (contentUri != null) {
      Log.d(TAG, "Resolved local path to MediaStore content URI: $contentUri")
      return contentUri
    }

    // Also check Audio if not a video
    val audioContentUri = runCatching {
      val projection = arrayOf(MediaStore.Audio.Media._ID)
      val selection = "${MediaStore.Audio.Media.DATA} = ?"
      val selectionArgs = arrayOf(rawPath)
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
          ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        } else {
          null
        }
      }
    }.getOrNull()

    if (audioContentUri != null) {
      Log.d(TAG, "Resolved local audio path to MediaStore content URI: $audioContentUri")
      return audioContentUri
    }

    val f = File(rawPath)
    return if (f.exists()) Uri.fromFile(f) else Uri.parse(trimmed)
  }

  fun createHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
    val customUserAgent = PlayerBridge.propString["user-agent"]?.value?.takeIf { it.isNotBlank() }
      ?: "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 AuraPlayer/1.0"

    val factory = DefaultHttpDataSource.Factory()
      .setUserAgent(customUserAgent)
      .setAllowCrossProtocolRedirects(true)
      .setConnectTimeoutMs(20_000)
      .setReadTimeoutMs(30_000)

    val headersProp = PlayerBridge.propString["http-header-fields"]?.value
    if (!headersProp.isNullOrBlank()) {
      val map = mutableMapOf<String, String>()
      headersProp.split(",").forEach { field ->
        val parts = field.split(":", limit = 2)
        if (parts.size == 2) {
          map[parts[0].trim()] = parts[1].trim().replace("\\,", ",")
        }
      }
      if (map.isNotEmpty()) {
        factory.setDefaultRequestProperties(map)
      }
    }
    return factory
  }

  fun playFile(context: Context? = null, pathOrUri: String) {
    currentSource = pathOrUri
    hasDispatchedFileLoaded = false
    mainHandler.post {
      val ctx = context ?: activePlayerView?.context ?: appContext ?: return@post
      val player = getOrCreatePlayer(ctx)

      val targetUri = resolvePlayableUri(ctx, pathOrUri)
      Log.i(TAG, "ExoPlayer playing target URI: $targetUri (from $pathOrUri)")

      val uriStr = targetUri.toString().lowercase()
      val mediaItemBuilder = MediaItem.Builder().setUri(targetUri)
      if (uriStr.contains(".m3u8") || uriStr.contains("application/x-mpegurl") || uriStr.contains("application/vnd.apple.mpegurl")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
      } else if (uriStr.contains(".mpd") || uriStr.contains("application/dash+xml")) {
        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
      }
      val mediaItem = mediaItemBuilder.build()

      val isNetwork = targetUri.scheme?.startsWith("http") == true
      val httpFactory = createHttpDataSourceFactory()

      val dataSourceFactory = if (isNetwork) {
        // Direct HTTP datasource for online streaming to prevent cache lockup and buffer underruns
        DefaultDataSource.Factory(ctx, httpFactory)
      } else {
        DefaultDataSource.Factory(ctx)
      }
      val mediaSource = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)

      player.setMediaSource(mediaSource)
      resetVideoTransform()
      player.prepare()
      player.playWhenReady = true
      player.play()

      PlayerBridge.propBoolean["pause"].value = false
      PlayerBridge.propBoolean["eof-reached"].value = false
      PlayerBridge.notifyProperty("pause", false)
    }
  }

  fun pause() {
    mainHandler.post {
      exoPlayer?.pause()
      PlayerBridge.propBoolean["pause"].value = true
      PlayerBridge.notifyProperty("pause", true)
    }
  }

  fun resume() {
    mainHandler.post {
      exoPlayer?.play()
      PlayerBridge.propBoolean["pause"].value = false
      PlayerBridge.notifyProperty("pause", false)
    }
  }

  fun seekTo(seconds: Double, isRelative: Boolean = false) {
    mainHandler.post {
      exoPlayer?.let { player ->
        val currentMs = player.currentPosition
        val targetMs = if (isRelative) {
          (currentMs + (seconds * 1000).toLong()).coerceIn(0, player.duration.coerceAtLeast(0))
        } else {
          (seconds * 1000).toLong().coerceIn(0, player.duration.coerceAtLeast(0))
        }
        player.seekTo(targetMs)

        val newSec = (targetMs / 1000).toInt()
        PlayerBridge.propInt["time-pos"].value = newSec
        PlayerBridge.propDouble["time-pos"].value = targetMs.toDouble() / 1000.0
        PlayerBridge.notifyProperty("time-pos", newSec.toLong())
      }
    }
  }

  fun stepFrame(forward: Boolean) {
    mainHandler.post {
      val player = exoPlayer ?: return@post
      val vFormat = player.videoFormat
      val fps = if (vFormat != null && vFormat.frameRate > 0f) vFormat.frameRate else 30f
      val deltaSec = (1.0 / fps.toDouble()) * (if (forward) 1.0 else -1.0)
      seekTo(deltaSec, isRelative = true)
    }
  }

  fun setPlaybackSpeed(speed: Float) {
    mainHandler.post {
      exoPlayer?.setPlaybackSpeed(speed)
      PlayerBridge.propFloat["speed"].value = speed
      PlayerBridge.propDouble["speed"].value = speed.toDouble()
      PlayerBridge.notifyProperty("speed", speed.toDouble())
    }
  }

  fun setVolume(volumeInt: Int) {
    mainHandler.post {
      exoPlayer?.let { player ->
        // Standard ExoPlayer volume is 0.0 to 1.0 (0-100%).
        // Above 100%, clamped to 1.0; system or equalizer boost handles extras.
        val volFraction = (volumeInt.coerceIn(0, 200) / 100f).coerceAtMost(1.0f)
        player.volume = volFraction
        PlayerBridge.propInt["volume"].value = volumeInt
        PlayerBridge.notifyProperty("volume", volumeInt.toLong())
      }
    }
  }

  fun setResizeMode(mode: Int) {
    mainHandler.post {
      activePlayerView?.resizeMode = mode
      applyVideoTransform()
    }
  }

  fun setVideoZoom(zoom: Float) {
    currentVideoZoom = zoom
    applyVideoTransform()
  }

  fun setVideoPanX(panX: Float) {
    currentVideoPanX = panX
    applyVideoTransform()
  }

  fun setVideoPanY(panY: Float) {
    currentVideoPanY = panY
    applyVideoTransform()
  }

  fun setVideoPan(panX: Float, panY: Float) {
    currentVideoPanX = panX
    currentVideoPanY = panY
    applyVideoTransform()
  }

  fun resetVideoTransform() {
    currentVideoZoom = 0f
    currentVideoPanX = 0f
    currentVideoPanY = 0f
    isHorizontalFlipped = false
    isVerticalFlipped = false
    applyVideoTransform()
  }

  fun applyVideoTransform() {
    mainHandler.post {
      val pv = activePlayerView ?: return@post
      val contentFrame = pv.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
      val surfaceView = pv.videoSurfaceView

      // Ensure clipping is disabled across the whole hierarchy so zoomed video isn't constrained
      pv.clipChildren = false
      pv.clipToPadding = false
      contentFrame?.clipChildren = false
      contentFrame?.clipToPadding = false
      (pv.parent as? ViewGroup)?.let { parent ->
        parent.clipChildren = false
        parent.clipToPadding = false
        (parent.parent as? ViewGroup)?.let { grandParent ->
          grandParent.clipChildren = false
          grandParent.clipToPadding = false
        }
      }

      val targetView: View = contentFrame ?: surfaceView ?: pv
      val width = targetView.width.toFloat()
      val height = targetView.height.toFloat()

      if (width <= 0f || height <= 0f) {
        targetView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
          override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
          ) {
            targetView.removeOnLayoutChangeListener(this)
            applyVideoTransform()
          }
        })
        return@post
      }

      // Zoom is in powers of 2 (0.0 -> 1.0x, 1.0 -> 2.0x, -1.0 -> 0.5x)
      val baseScale = Math.pow(2.0, currentVideoZoom.toDouble()).toFloat()
      val scaleX = baseScale * (if (isHorizontalFlipped) -1f else 1f)
      val scaleY = baseScale * (if (isVerticalFlipped) -1f else 1f)
      val transX = currentVideoPanX * width * baseScale
      val transY = currentVideoPanY * height * baseScale

      targetView.pivotX = width / 2f
      targetView.pivotY = height / 2f
      targetView.scaleX = scaleX
      targetView.scaleY = scaleY
      targetView.translationX = transX
      targetView.translationY = transY

      // Avoid double scaling if target is contentFrame
      if (targetView === contentFrame && surfaceView != null) {
        surfaceView.scaleX = 1f
        surfaceView.scaleY = 1f
        surfaceView.translationX = 0f
        surfaceView.translationY = 0f
      }

      targetView.invalidate()
    }
  }

  fun selectTrack(typeStr: String, id: Int) {
    mainHandler.post {
      val player = exoPlayer ?: return@post
      val trackType = when (typeStr) {
        "audio" -> C.TRACK_TYPE_AUDIO
        "video" -> C.TRACK_TYPE_VIDEO
        else -> C.TRACK_TYPE_TEXT
      }

      if (id <= 0) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
          .setTrackTypeDisabled(trackType, true)
          .clearOverridesOfType(trackType)
          .build()
        when (typeStr) {
          "sub" -> {
            PlayerBridge.propInt["sid"].value = -1
            PlayerBridge.propString["sid"].value = "no"
          }
          "audio" -> {
            PlayerBridge.propInt["aid"].value = -1
            PlayerBridge.propString["aid"].value = "no"
          }
          "video" -> {
            PlayerBridge.propInt["vid"].value = -1
            PlayerBridge.propString["vid"].value = "no"
          }
        }
        return@post
      }

      val tracks = player.currentTracks
      var targetGroup: Tracks.Group? = null
      var targetTrackIndex = 0
      var currentGlobalId = 0

      // First pass: match by global track ID where group type strictly matches trackType
      for (group in tracks.groups) {
        val type = group.type
        if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT && type != C.TRACK_TYPE_VIDEO) continue
        for (i in 0 until group.length) {
          currentGlobalId++
          if (type == trackType && currentGlobalId == id) {
            targetGroup = group
            targetTrackIndex = i
            break
          }
        }
        if (targetGroup != null) break
      }

      // Second pass: fallback match by type-specific 1-based index (e.g. aid=1 means 1st audio track)
      if (targetGroup == null) {
        var currentTypeIdx = 0
        for (group in tracks.groups) {
          if (group.type != trackType) continue
          for (i in 0 until group.length) {
            currentTypeIdx++
            if (currentTypeIdx == id) {
              targetGroup = group
              targetTrackIndex = i
              break
            }
          }
          if (targetGroup != null) break
        }
      }

      if (targetGroup != null) {
        val actualType = targetGroup.type
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
          .setTrackTypeDisabled(actualType, false)
          .clearOverridesOfType(actualType)
          .setOverrideForType(
            TrackSelectionOverride(targetGroup.mediaTrackGroup, targetTrackIndex)
          )
          .build()

        when (typeStr) {
          "sub" -> {
            PlayerBridge.propInt["sid"].value = id
            PlayerBridge.propString["sid"].value = id.toString()
            PlayerBridge.notifyProperty("sid", id.toLong())
          }
          "audio" -> {
            PlayerBridge.propInt["aid"].value = id
            PlayerBridge.propString["aid"].value = id.toString()
            PlayerBridge.notifyProperty("aid", id.toLong())
          }
          "video" -> {
            PlayerBridge.propInt["vid"].value = id
            PlayerBridge.propString["vid"].value = id.toString()
            PlayerBridge.notifyProperty("vid", id.toLong())
          }
        }
        syncTracks(player.currentTracks)
      }
    }
  }

  fun removeSubtitle(id: Int) {
    mainHandler.post {
      val player = exoPlayer ?: return@post
      val currentSid = PlayerBridge.propInt["sid"].value
      if (currentSid == id || id <= 0) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
          .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
          .clearOverridesOfType(C.TRACK_TYPE_TEXT)
          .build()
        PlayerBridge.propInt["sid"].value = -1
        PlayerBridge.propString["sid"].value = "no"
      }
    }
  }

  fun addExternalSubtitle(pathOrUri: String, title: String? = null, select: Boolean = true) {
    mainHandler.post {
      val player = exoPlayer ?: return@post
      val uri = when {
        pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") ||
        pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://") -> Uri.parse(pathOrUri)
        else -> Uri.fromFile(File(pathOrUri))
      }
      val mimeType = when {
        pathOrUri.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
        pathOrUri.endsWith(".ass", ignoreCase = true) || pathOrUri.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
      }
      val displayName = title ?: pathOrUri.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
      val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(mimeType)
        .setLabel(displayName)
        .setLanguage("en")
        .setSelectionFlags(if (select) C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_FORCED else 0)
        .build()

      val currentItem = player.currentMediaItem ?: return@post
      val currentPos = player.currentPosition
      val isPlaying = player.isPlaying

      val existingSubs = currentItem.localConfiguration?.subtitleConfigurations.orEmpty()
      if (existingSubs.any { it.uri == uri }) {
        if (select) {
          player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
        }
        return@post
      }

      val updatedItem = currentItem.buildUpon()
        .setSubtitleConfigurations(existingSubs + subConfig)
        .build()

      if (select) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
          .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
          .build()
      }

      val mediaSource = cachedMediaSourceFactory?.createMediaSource(updatedItem)
      if (mediaSource != null) {
        player.setMediaSource(mediaSource, currentPos)
      } else {
        player.setMediaItem(updatedItem, currentPos)
      }
      player.prepare()
      if (isPlaying) player.play()
    }
  }

  fun addExternalAudio(pathOrUri: String, title: String? = null) {
    mainHandler.post {
      val player = exoPlayer ?: return@post
      val uri = when {
        pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://") ||
        pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://") -> Uri.parse(pathOrUri)
        else -> Uri.fromFile(File(pathOrUri))
      }
      val audioMediaItem = MediaItem.Builder()
        .setUri(uri)
        .setMediaMetadata(
          androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title ?: pathOrUri.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.'))
            .build()
        )
        .build()

      val currentItem = player.currentMediaItem ?: return@post
      val currentPos = player.currentPosition
      val isPlaying = player.isPlaying

      val videoSource = cachedMediaSourceFactory?.createMediaSource(currentItem)
      val audioSource = cachedMediaSourceFactory?.createMediaSource(audioMediaItem)

      if (videoSource != null && audioSource != null) {
        val mergedSource = androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
        player.setMediaSource(mergedSource, currentPos)
        player.prepare()
        if (isPlaying) player.play()
      }
    }
  }

  suspend fun captureFrame(destinationFile: File): Boolean = kotlinx.coroutines.withContext(Dispatchers.Main) {
    try {
      val player = exoPlayer ?: return@withContext false
      val appCtx = appContext ?: return@withContext false
      var bitmap: Bitmap? = null

      // Method 1: PixelCopy from SurfaceView or direct from TextureView
      val view = (currentPlayerView?.videoSurfaceView as? android.view.TextureView)
      if (view != null) {
        bitmap = view.bitmap
      } else {
        val surfView = currentPlayerView?.videoSurfaceView
        if (surfView is android.view.SurfaceView && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          val surface = surfView.holder.surface
          if (surface != null && surface.isValid) {
            val width = surfView.width.takeIf { it > 0 } ?: 1280
            val height = surfView.height.takeIf { it > 0 } ?: 720
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val copyResult = runCatching {
              kotlinx.coroutines.withTimeoutOrNull(2000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<Int> { cont ->
                  android.view.PixelCopy.request(surface, bmp, { result ->
                    if (cont.isActive) cont.resume(result, null)
                  }, mainHandler)
                }
              }
            }.getOrNull()
            if (copyResult == android.view.PixelCopy.SUCCESS) {
              bitmap = bmp
            }
          }
        }
      }

      // Method 2: MediaMetadataRetriever fallback
      if (bitmap == null) {
        withContext(Dispatchers.IO) {
          val source = currentSource ?: player.currentMediaItem?.localConfiguration?.uri?.toString()
          if (!source.isNullOrEmpty()) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
              val posUs = player.currentPosition * 1000L
              if (source.startsWith("content://")) {
                val uri = Uri.parse(source)
                try {
                  appCtx.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                  }
                } catch (e: Exception) {
                  retriever.setDataSource(appCtx, uri)
                }
              } else if (source.startsWith("http://") || source.startsWith("https://")) {
                retriever.setDataSource(source, HashMap())
              } else {
                val f = File(source)
                if (f.exists()) {
                  retriever.setDataSource(f.absolutePath)
                }
              }
              bitmap = retriever.getFrameAtTime(posUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST)
                ?: retriever.getFrameAtTime(posUs, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } catch (e: Exception) {
              Log.w(TAG, "MediaMetadataRetriever fallback error: ${e.message}")
            } finally {
              runCatching { retriever.release() }
            }
          }
        }
      }

      // Method 3: Fallback from PlayerView drawing cache
      if (bitmap == null && currentPlayerView != null) {
        val pv = currentPlayerView!!
        val w = pv.width.takeIf { it > 0 } ?: 1280
        val h = pv.height.takeIf { it > 0 } ?: 720
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        pv.draw(canvas)
        bitmap = bmp
      }

      if (bitmap != null) {
        withContext(Dispatchers.IO) {
          destinationFile.parentFile?.mkdirs()
          FileOutputStream(destinationFile).use { out ->
            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
          }
        }
        true
      } else {
        false
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to capture frame screenshot", e)
      false
    }
  }

  fun takeScreenshot(destinationPath: String): Boolean {
    return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
      captureFrame(File(destinationPath))
    }
  }

  fun stop() {
    progressJob?.cancel()
    bufferingJob?.cancel()
    mainHandler.post {
      exoPlayer?.let { player ->
        player.stop()
        player.clearMediaItems()
      }
      PlayerBridge.propBoolean["pause"].value = true
      PlayerBridge.propBoolean["paused-for-cache"].value = false
      PlayerBridge.notifyProperty("pause", true)
      PlayerBridge.notifyProperty("paused-for-cache", false)
      PlayerBridge.notifyEvent(PlayerBridge.MpvEvent.MPV_EVENT_IDLE)
    }
  }

  fun release() {
    progressJob?.cancel()
    bufferingJob?.cancel()
    progressJob = null
    bufferingJob = null
    mainHandler.post {
      exoPlayer?.let { player ->
        player.stop()
        player.clearMediaItems()
        player.release()
      }
      exoPlayer = null
      activePlayerView = null
      trackSelector = null
    }
  }
}
