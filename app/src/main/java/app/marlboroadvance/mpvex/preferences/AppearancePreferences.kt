package app.marlboroadvance.mpvex.preferences

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.preference.PreferenceStore
import app.marlboroadvance.mpvex.preferences.preference.getEnum
import app.marlboroadvance.mpvex.ui.theme.AppTheme
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

class AppearancePreferences(
  preferenceStore: PreferenceStore,
) {
  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val appTheme = preferenceStore.getEnum("app_theme", AppTheme.Dynamic)
  val amoledMode = preferenceStore.getBoolean("amoled_mode", false)
  val liquidGlassSurfaces = preferenceStore.getBoolean("liquid_glass_surfaces", false)
  val useSystemFont = preferenceStore.getBoolean("use_system_font", false)
  val unlimitedNameLines = preferenceStore.getBoolean("unlimited_name_lines", false)
  val hidePlayerButtonsBackground = preferenceStore.getBoolean("hide_player_buttons_background", false)
  val showUnplayedOldVideoLabel = preferenceStore.getBoolean("show_unplayed_old_video_label", true)
  val unplayedOldVideoDays = preferenceStore.getInt("unplayed_old_video_days", 7)
  val seekbarStyle = preferenceStore.getEnum("seekbar_style", SeekbarStyle.Thick)

  // Navigation tab visibility preferences
  val showHomeTab = preferenceStore.getBoolean("nav_home_tab", true)
  val showRecentsTab = preferenceStore.getBoolean("nav_recents_tab", true)
  val showPlaylistsTab = preferenceStore.getBoolean("nav_playlists_tab", true)
  val showNetworkTab = preferenceStore.getBoolean("nav_network_tab", true)
  val showMusicTab = preferenceStore.getBoolean("nav_music_tab", true)

  val topLeftControls =
    preferenceStore.getString(
      "top_left_controls",
      "BACK_ARROW,VIDEO_TITLE",
    )

  val topRightControls =
    preferenceStore.getString(
      "top_right_controls",
      "CAST,CURRENT_CHAPTER,MORE_OPTIONS",
    )

  val bottomRightControls =
    preferenceStore.getString(
      "bottom_right_controls",
      "PICTURE_IN_PICTURE,ASPECT_RATIO,VIDEO_ZOOM,FRAME_NAVIGATION,CLIP",
    )

  val bottomLeftControls =
    preferenceStore.getString(
      "bottom_left_controls",
      "LOCK_CONTROLS,SCREEN_ROTATION,DECODER,AUDIO_TRACK,SUBTITLES,PLAYBACK_SPEED,REPEAT_MODE,AB_LOOP",
    )

  val portraitBottomControls =
    preferenceStore.getString(
      "portrait_bottom_controls",
      "LOCK_CONTROLS,SCREEN_ROTATION,DECODER,AUDIO_TRACK,SUBTITLES,PLAYBACK_SPEED,ASPECT_RATIO,PICTURE_IN_PICTURE,BACKGROUND_PLAYBACK,AB_LOOP,MORE_OPTIONS",
    )

  init {
    val dedupKey = "player_controls_dedup_v1"
    val dedupPref = preferenceStore.getBoolean(dedupKey, false)
    if (!dedupPref.get()) {
      fun cleanList(csv: String): String =
        csv.split(',')
          .map { it.trim().uppercase() }
          .filter { it.isNotBlank() && it != "NONE" }
          .distinct()
          .joinToString(",")

      topRightControls.set(cleanList(topRightControls.get()))
      bottomRightControls.set(cleanList(bottomRightControls.get()))
      bottomLeftControls.set(cleanList(bottomLeftControls.get()))
      portraitBottomControls.set(cleanList(portraitBottomControls.get()))
      dedupPref.set(true)
    }
  }

  fun parseButtons(
    csv: String,
    usedButtons: MutableSet<PlayerButton>,
  ): List<PlayerButton> =
    csv
      .splitToSequence(',')
      .map { it.trim().uppercase() }
      .mapNotNull { name ->
        try {
          PlayerButton.valueOf(name)
        } catch (_: IllegalArgumentException) {
          null
        }
      }.filter { it != PlayerButton.NONE }
      .filter { usedButtons.add(it) }
      .toList()
}

@Composable
fun MultiChoiceSegmentedButton(
  choices: ImmutableList<String>,
  selectedIndices: ImmutableList<Int>,
  onClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  MultiChoiceSegmentedButtonRow(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
  ) {
    choices.forEachIndexed { index, choice ->
      SegmentedButton(
        checked = selectedIndices.contains(index),
        onCheckedChange = { onClick(index) },
        shape = SegmentedButtonDefaults.itemShape(index = index, count = choices.size),
      ) {
        Text(text = choice)
      }
    }
  }
}
