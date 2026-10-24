package app.marlboroadvance.mpvex.ui.player

import android.content.Context
import android.os.Environment
import android.util.AttributeSet

import android.view.KeyCharacterMap
import android.view.KeyEvent
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.ui.player.PlayerActivity.Companion.TAG
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.toColorHexString
import com.cmstudio.apnaplayer.engine.BasePlayerView
import com.cmstudio.apnaplayer.engine.keyMapping
import com.cmstudio.apnaplayer.engine.PlayerBridge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.reflect.KProperty

class ApnaPlayerView(
  context: Context,
  attributes: AttributeSet,
) : BasePlayerView(context, attributes),
  KoinComponent {
  private val audioPreferences: AudioPreferences by inject()
  private val playerPreferences: PlayerPreferences by inject()
  private val decoderPreferences: DecoderPreferences by inject()
  private val advancedPreferences: AdvancedPreferences by inject()
  private val subtitlesPreferences: SubtitlesPreferences by inject()
  private val anime4kManager: Anime4KManager by inject()

  fun getVideoOutAspect(): Double? {
    // Try to get aspect from video-params/aspect first
    val rawAspect = PlayerBridge.getPropertyDouble("video-params/aspect")
    val rotate = PlayerBridge.getPropertyInt("video-params/rotate") ?: 0

    // If aspect is not available or 0, calculate from width and height
    val finalAspect = if (rawAspect == null || rawAspect < 0.001) {
      val width = runCatching {
        PlayerBridge.getPropertyInt("width") ?: PlayerBridge.getPropertyInt("video-params/w") ?: 0
      }.getOrDefault(0)

      val height = runCatching {
        PlayerBridge.getPropertyInt("height") ?: PlayerBridge.getPropertyInt("video-params/h") ?: 0
      }.getOrDefault(0)

      if (width > 0 && height > 0) {
        width.toDouble() / height.toDouble()
      } else {
        null
      }
    } else {
      rawAspect
    }

    return finalAspect?.let { aspect ->
      if (aspect <= 0.001) {
        return null
      }
      val isRotated = (rotate % 180 == 90)
      val correctedAspect = if (isRotated) 1.0 / aspect else aspect
      correctedAspect
    }
  }

  class TrackDelegate(
    private val name: String,
  ) {
    operator fun getValue(
      thisRef: Any?,
      property: KProperty<*>,
    ): Int {
      val v = PlayerBridge.getPropertyString(name)
      // we can get null here for "no" or other invalid value
      return v?.toIntOrNull() ?: -1
    }

    operator fun setValue(
      thisRef: Any?,
      property: KProperty<*>,
      value: Int,
    ) {
      if (value <= 0) PlayerBridge.setPropertyString(name, "no") else PlayerBridge.setPropertyInt(name, value)
    }
  }

  var sid: Int by TrackDelegate("sid")
  var secondarySid: Int by TrackDelegate("secondary-sid")
  var aid: Int by TrackDelegate("aid")

  override fun initOptions() {
    val profile = decoderPreferences.profile.get()
    PlayerBridge.setOptionString("profile", profile)
    setVo(if (decoderPreferences.gpuNext.get()) "gpu-next" else "gpu")
    
    // Set GPU API context (Vulkan or OpenGL)
    if (decoderPreferences.useVulkan.get()) {
      PlayerBridge.setOptionString("gpu-context", "androidvk")
    }

    // Set hwdec with fallback order: HW+ (mediacodec) -> HW (mediacodec-copy) -> SW (no)
    PlayerBridge.setOptionString(
      "hwdec",
      if (decoderPreferences.tryHWDecoding.get()) "mediacodec,mediacodec-copy,no" else "no",
    )
    PlayerBridge.setOptionString("hwdec-codecs", "all")

    if (decoderPreferences.useYUV420P.get()) {
      PlayerBridge.setOptionString("vf", "format=yuv420p")
    }
    
    // Configurations: optimal demuxer cache and mobile hardware optimizations
    val cacheMegs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) 128 else 64
    PlayerBridge.setOptionString("demuxer-max-bytes", "${cacheMegs * 1024 * 1024}")
    PlayerBridge.setOptionString("demuxer-max-back-bytes", "${(cacheMegs / 2) * 1024 * 1024}")
    PlayerBridge.setOptionString("opengl-pbo", "yes")
    PlayerBridge.setOptionString("interpolation", "no")
    PlayerBridge.setOptionString("vd-lavc-dr", "yes")
    
    val logLevel = if (advancedPreferences.verboseLogging.get()) "v" else "warn"
    PlayerBridge.setOptionString("msg-level", "all=$logLevel")

    PlayerBridge.setPropertyBoolean("keep-open", true)
    PlayerBridge.setPropertyBoolean("input-default-bindings", true)

    PlayerBridge.setOptionString("tls-verify", "yes")
    PlayerBridge.setOptionString("tls-ca-file", "${context.filesDir.path}/cacert.pem")

    val screenshotDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    screenshotDir.mkdirs()
    PlayerBridge.setOptionString("screenshot-directory", screenshotDir.path)

    VideoFilters.entries.forEach {
      PlayerBridge.setOptionString(it.engineProperty, it.preference(decoderPreferences).get().toString())
    }

    PlayerBridge.setOptionString("speed", playerPreferences.defaultSpeed.get().toString())
    PlayerBridge.setOptionString("vd-lavc-film-grain", "cpu")

    val preciseSeek = playerPreferences.usePreciseSeeking.get()
    PlayerBridge.setOptionString("hr-seek", if (preciseSeek) "yes" else "no")
    PlayerBridge.setOptionString("hr-seek-framedrop", if (preciseSeek) "no" else "yes")

    // Anime4K shader initialization (MUST be in initOptions, not after file load!)
    applyAnime4KShaders()

    setupSubtitlesOptions()
    setupAudioOptions()
  }

  override fun observeProperties() {
    for ((name, format) in observedProps) PlayerBridge.observeProperty(name, format)
  }

  override fun postInitOptions() {
    when (decoderPreferences.debanding.get()) {
      Debanding.None -> {}
      Debanding.CPU -> PlayerBridge.command("vf", "add", "@deband:gradfun=radius=12")
      Debanding.GPU -> PlayerBridge.setOptionString("deband", "yes")
    }

    advancedPreferences.enabledStatisticsPage.get().let {
      if (it != 0) {
        PlayerBridge.command("script-binding", "stats/display-stats-toggle")
        PlayerBridge.command("script-binding", "stats/display-page-$it")
      }
    }
  }

  @Suppress("ReturnCount", "DEPRECATION")
  fun onKey(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_MULTIPLE || KeyEvent.isModifierKey(event.keyCode)) {
      return false
    }

    var mapped = keyMapping[event.keyCode]
    if (mapped == null) {
      // Fallback to produced glyph
      if (!event.isPrintingKey) {
        return false
      }

      val ch = event.unicodeChar
      if (ch.and(KeyCharacterMap.COMBINING_ACCENT) != 0) {
        return false // dead key
      }
      mapped = ch.toChar().toString()
    }

    if (event.repeatCount > 0) {
      return true
    }

    val mod: MutableList<String> = mutableListOf()
    event.isShiftPressed && mod.add("shift")
    event.isCtrlPressed && mod.add("ctrl")
    event.isAltPressed && mod.add("alt")
    event.isMetaPressed && mod.add("meta")

    val action = if (event.action == KeyEvent.ACTION_DOWN) "keydown" else "keyup"
    mod.add(mapped)
    PlayerBridge.command(action, mod.joinToString("+"))

    return true
  }

  private val observedProps =
    mapOf(
      "pause" to PlayerBridge.MpvFormat.MPV_FORMAT_FLAG,
      "paused-for-cache" to PlayerBridge.MpvFormat.MPV_FORMAT_FLAG,
      "video-params/aspect" to PlayerBridge.MpvFormat.MPV_FORMAT_DOUBLE,
      "video-params/w" to PlayerBridge.MpvFormat.MPV_FORMAT_INT64,
      "video-params/h" to PlayerBridge.MpvFormat.MPV_FORMAT_INT64,
      "eof-reached" to PlayerBridge.MpvFormat.MPV_FORMAT_FLAG,
      "user-data/mpvex/show_text" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_ui" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/show_panel" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/set_button_title" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/reset_button_title" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/toggle_button" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_by_with_text" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/seek_to_with_text" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
      "user-data/mpvex/software_keyboard" to PlayerBridge.MpvFormat.MPV_FORMAT_STRING,
    )

  private fun setupAudioOptions() {
    // Disable automatic audio selection
    // App handles track selection manually via TrackSelector to respect user choices
    PlayerBridge.setOptionString("alang", "")
    PlayerBridge.setOptionString("audio-delay", (audioPreferences.defaultAudioDelay.get() / 1000.0).toString())
    PlayerBridge.setOptionString("audio-pitch-correction", audioPreferences.audioPitchCorrection.get().toString())
    PlayerBridge.setOptionString("volume-max", (audioPreferences.volumeBoostCap.get() + 100).toString())
    
    // Volume normalization using dynamic audio normalization filter
    if (audioPreferences.volumeNormalization.get()) {
      PlayerBridge.setOptionString("af", "dynaudnorm")
    }
  }

  // Setup
  private fun setupSubtitlesOptions() {
    // Disable automatic subtitle selection
    // App handles track selection manually via TrackSelector to respect user choices
    PlayerBridge.setOptionString("slang", "")
    PlayerBridge.setOptionString("sub-auto", "no")
    PlayerBridge.setOptionString("sub-file-paths", "")
    PlayerBridge.setOptionString("subs-fallback", "no")

    val fontsDirPath = "${context.filesDir.path}/fonts/"
    PlayerBridge.setOptionString("sub-fonts-dir", fontsDirPath)
    
    // Delay and speed for both primary and secondary
    val subDelay = (subtitlesPreferences.defaultSubDelay.get() / 1000.0).toString()
    val subSpeed = subtitlesPreferences.defaultSubSpeed.get().toString()
    PlayerBridge.setOptionString("sub-delay", subDelay)
    PlayerBridge.setOptionString("sub-speed", subSpeed)
    PlayerBridge.setOptionString("secondary-sub-delay", subDelay)
    PlayerBridge.setOptionString("secondary-sub-speed", subSpeed)

    val preferredFont = subtitlesPreferences.font.get()
    if (preferredFont.isNotBlank()) {
      PlayerBridge.setOptionString("sub-font", preferredFont)
      PlayerBridge.setOptionString("secondary-sub-font", preferredFont)
    }

    if (subtitlesPreferences.overrideAssSubs.get()) {
      PlayerBridge.setOptionString("sub-ass-override", "force")
      PlayerBridge.setOptionString("sub-ass-justify", "yes")
      PlayerBridge.setOptionString("secondary-sub-ass-override", "force")
    } else {
      PlayerBridge.setOptionString("sub-ass-override", "no")
      PlayerBridge.setOptionString("secondary-sub-ass-override", "no")
    }

    // Typography and styling for both primary and secondary
    val fontSize = subtitlesPreferences.fontSize.get().toString()
    val bold = if (subtitlesPreferences.bold.get()) "yes" else "no"
    val italic = if (subtitlesPreferences.italic.get()) "yes" else "no"
    val justify = subtitlesPreferences.justification.get().value
    val textColor = subtitlesPreferences.textColor.get().toColorHexString()
    val backgroundColor = subtitlesPreferences.backgroundColor.get().toColorHexString()
    val borderColor = subtitlesPreferences.borderColor.get().toColorHexString()
    val borderSize = subtitlesPreferences.borderSize.get().toString()
    val borderStyle = subtitlesPreferences.borderStyle.get().value
    val shadowOffset = subtitlesPreferences.shadowOffset.get().toString()
    val subPos = subtitlesPreferences.subPos.get().toString()
    val subScale = subtitlesPreferences.subScale.get().toString()

    PlayerBridge.setOptionString("sub-font-size", fontSize)
    PlayerBridge.setOptionString("sub-bold", bold)
    PlayerBridge.setOptionString("sub-italic", italic)
    PlayerBridge.setOptionString("sub-justify", justify)
    PlayerBridge.setOptionString("sub-color", textColor)
    PlayerBridge.setOptionString("sub-back-color", backgroundColor)
    PlayerBridge.setOptionString("sub-border-color", borderColor)
    PlayerBridge.setOptionString("sub-border-size", borderSize)
    PlayerBridge.setOptionString("sub-border-style", borderStyle)
    PlayerBridge.setOptionString("sub-shadow-offset", shadowOffset)
    PlayerBridge.setOptionString("sub-scale", subScale)
    PlayerBridge.setOptionString("sub-pos", subPos)
    
    PlayerBridge.setOptionString("secondary-sub-font-size", fontSize)
    PlayerBridge.setOptionString("secondary-sub-bold", bold)
    PlayerBridge.setOptionString("secondary-sub-italic", italic)
    PlayerBridge.setOptionString("secondary-sub-justify", justify)
    PlayerBridge.setOptionString("secondary-sub-color", textColor)
    PlayerBridge.setOptionString("secondary-sub-back-color", backgroundColor)
    PlayerBridge.setOptionString("secondary-sub-border-color", borderColor)
    PlayerBridge.setOptionString("secondary-sub-border-size", borderSize)
    PlayerBridge.setOptionString("secondary-sub-border-style", borderStyle)
    PlayerBridge.setOptionString("secondary-sub-shadow-offset", shadowOffset)
    PlayerBridge.setOptionString("secondary-sub-scale", subScale)
    // Position secondary subtitle at top (10) instead of bottom to avoid overlap with primary
    PlayerBridge.setOptionString("secondary-sub-pos", "10")

    val scaleByWindow = if (subtitlesPreferences.scaleByWindow.get()) "yes" else "no"
    PlayerBridge.setOptionString("sub-scale-by-window", scaleByWindow)
    PlayerBridge.setOptionString("sub-use-margins", scaleByWindow)
    PlayerBridge.setOptionString("secondary-sub-scale-by-window", scaleByWindow)
    PlayerBridge.setOptionString("secondary-sub-use-margins", scaleByWindow)
  }

  fun applyAnime4KShaders() {
    runCatching {
      val enabled = decoderPreferences.enableAnime4K.get()
      if (!enabled) {
        return
      }
      
      val gpuNextActive = decoderPreferences.gpuNext.get()
      val useVulkan = decoderPreferences.useVulkan.get()
      if (gpuNextActive && !useVulkan) {
        return  // Abort shader loading to prevent incompatible state
      }
      
      // Initialize shader files if needed
      if (!anime4kManager.initialize()) {
        return
      }
      
      // Get preferences
      val modeStr = decoderPreferences.anime4kMode.get()
      
      // Check if mode is OFF - if so, don't apply any shaders
      if (modeStr == "OFF") {
        return  // Exit early - user wants it OFF
      }
      
      // Parse user's selected mode
      val mode = try {
          Anime4KManager.Mode.valueOf(modeStr)
      } catch (e: IllegalArgumentException) {
          Anime4KManager.Mode.OFF
      }
      
      val qualityStr = decoderPreferences.anime4kQuality.get()
      val quality = try {
        Anime4KManager.Quality.valueOf(qualityStr)
      } catch (e: IllegalArgumentException) {
        Anime4KManager.Quality.BALANCED
      }
      
      // Get shader chain from manager
      val shaderChain = anime4kManager.getShaderChain(mode, quality)
      
      if (shaderChain.isNotEmpty()) {
        // OpenGL-only tuning should not be pushed onto the Vulkan backend.
        if (!useVulkan) {
          PlayerBridge.setOptionString("opengl-pbo", "yes")
          PlayerBridge.setOptionString("opengl-early-flush", "no")
        }
        PlayerBridge.setOptionString("vd-lavc-dr", "yes")
        
        // Apply shaders (MUST use setOptionString in initOptions!)
        PlayerBridge.setOptionString("glsl-shaders", shaderChain)
      }
    }.onFailure {
      // Don't crash - just continue without shaders
    }
  }
}

typealias MPVView = ApnaPlayerView
