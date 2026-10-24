package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore
import app.marlboroadvance.mpvex.preferences.preference.getEnum
import app.marlboroadvance.mpvex.ui.player.Debanding

class DecoderPreferences(
  preferenceStore: PreferenceStore,
) {
  // Google Media3 ExoPlayer Hardware Decoders & Pipeline
  val tryHWDecoding = preferenceStore.getBoolean("try_hw_dec", true)
  val decoderFallback = preferenceStore.getBoolean("decoder_fallback_enabled", true)
  val tunnelingEnabled = preferenceStore.getBoolean("video_tunneling_enabled", false)
  val audioOffload = preferenceStore.getBoolean("audio_offload_enabled", true)

  // Media3 LoadControl & Buffering parameters (High-performance, Zero-lag)
  val bufferMinMs = preferenceStore.getInt("buffer_min_ms", 25000)
  val bufferMaxMs = preferenceStore.getInt("buffer_max_ms", 60000)
  val bufferForPlaybackMs = preferenceStore.getInt("buffer_playback_ms", 2500)
  val bufferForPlaybackAfterRebufferMs = preferenceStore.getInt("buffer_rebuffer_ms", 5000)

  // Legacy compatibility mappings
  val profile = preferenceStore.getString("mpv_profile", "fast")
  val gpuNext = preferenceStore.getBoolean("gpu_next", false)
  val useVulkan = preferenceStore.getBoolean("use_vulkan", false)
  val useYUV420P = preferenceStore.getBoolean("use_yuv420p", false)

  val debanding = preferenceStore.getEnum("debanding", Debanding.None)
  val debandIterations = preferenceStore.getInt("deband_iterations", 1)
  val debandThreshold = preferenceStore.getInt("deband_threshold", 48)
  val debandRange = preferenceStore.getInt("deband_range", 16)
  val debandGrain = preferenceStore.getInt("deband_grain", 32)

  val brightnessFilter = preferenceStore.getInt("filter_brightness")
  val saturationFilter = preferenceStore.getInt("filter_saturation")
  val gammaFilter = preferenceStore.getInt("filter_gamma")
  val contrastFilter = preferenceStore.getInt("filter_contrast")
  val hueFilter = preferenceStore.getInt("filter_hue")
  val sharpnessFilter = preferenceStore.getInt("filter_sharpness")

  val enableAnime4K = preferenceStore.getBoolean("enable_anime4k", false)
  val anime4kMode = preferenceStore.getString("anime4k_mode", "OFF")
  val anime4kQuality = preferenceStore.getString("anime4k_quality", "FAST")

  // MPV Infinity v2.2.0 Smooth Motion & Video Quality Settings
  val smoothMotion = preferenceStore.getBoolean("smooth_motion_interpolation", false)
  val smoothMotionTargetFps = preferenceStore.getInt("smooth_motion_target_fps", 120)
  val videoScalingProfile = preferenceStore.getString("video_scaling_profile", "high_quality")
  val hdrToneMapping = preferenceStore.getString("hdr_tone_mapping", "auto")
}
