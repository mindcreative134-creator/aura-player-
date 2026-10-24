package app.marlboroadvance.mpvex.preferences

import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore

class AdvancedPreferences(
  preferenceStore: PreferenceStore,
) {
  val mpvConfStorageUri = preferenceStore.getString("backup_storage_location_uri")
  val engineConfig = preferenceStore.getString(
    "engine_config",
    "# Google Media3 ExoPlayer Configuration\nmedia3.exoplayer.hardware_acceleration=true\nmedia3.exoplayer.tunneling=auto\nmedia3.exoplayer.buffer.min_ms=15000\nmedia3.exoplayer.buffer.max_ms=50000\n",
  )
  val telemetryConfig = preferenceStore.getString(
    "telemetry_config",
    "# Media3 Telemetry & Playback Diagnostics\ndiagnostics.enabled=true\ntelemetry.level=standard\n",
  )

  // Backward-compatible delegates for clean migration
  val mpvConf get() = engineConfig
  val inputConf get() = telemetryConfig

  val verboseLogging = preferenceStore.getBoolean("verbose_logging", BuildConfig.BUILD_TYPE != "release")

  val enabledStatisticsPage = preferenceStore.getInt("enabled_stats_page", 0)

  val enableRecentlyPlayed = preferenceStore.getBoolean("enable_recently_played", true)

}
