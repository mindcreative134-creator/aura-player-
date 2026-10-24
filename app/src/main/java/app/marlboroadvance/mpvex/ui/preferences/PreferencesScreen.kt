package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current

    val decoderPrefs = koinInject<DecoderPreferences>()
    val playerPrefs = koinInject<PlayerPreferences>()
    val audioPrefs = koinInject<AudioPreferences>()
    val subPrefs = koinInject<SubtitlesPreferences>()
    val appearancePrefs = koinInject<AppearancePreferences>()
    val browserPrefs = koinInject<BrowserPreferences>()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Settings & Controls",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
          actions = {
            IconButton(onClick = { backstack.add(SettingsSearchScreen) }) {
              Icon(
                Icons.Outlined.Search,
                contentDescription = "Search Settings",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {

          // Search Bar Shortcut
          item {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { backstack.add(SettingsSearchScreen) },
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Search settings, toggles & parameters...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.outline,
                )
              }
            }
          }

          // 1. Playback Engine Chart
          item {
            PreferenceSectionHeader(title = "⚡ Playback Engine & Decoder")
          }
          item {
            PreferenceCard {
              val tryHWDecoding by decoderPrefs.tryHWDecoding.collectAsState()
              SwitchPreference(
                value = tryHWDecoding,
                onValueChange = { decoderPrefs.tryHWDecoding.set(it) },
                title = { Text("Hardware Decoding (MediaCodec)") },
                summary = {
                  Text(
                    "Utilize GPU hardware acceleration for smooth 4K/60fps playback",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val savePosition by playerPrefs.savePositionOnQuit.collectAsState()
              SwitchPreference(
                value = savePosition,
                onValueChange = { playerPrefs.savePositionOnQuit.set(it) },
                title = { Text("Auto-Resume Playback") },
                summary = {
                  Text(
                    "Automatically remember and resume video from previous timestamp",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val autoplayNext by playerPrefs.autoplayNextVideo.collectAsState()
              SwitchPreference(
                value = autoplayNext,
                onValueChange = { playerPrefs.autoplayNextVideo.set(it) },
                title = { Text("Autoplay Next Video") },
                summary = {
                  Text(
                    "Seamlessly play the next video in folder or queue when current ends",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val autoPiP by playerPrefs.autoPiPOnNavigation.collectAsState()
              SwitchPreference(
                value = autoPiP,
                onValueChange = { playerPrefs.autoPiPOnNavigation.set(it) },
                title = { Text("Background Picture-in-Picture") },
                summary = {
                  Text(
                    "Automatically enter PiP mini-player when minimizing or switching apps",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val keepScreenOn by playerPrefs.keepScreenOnWhenPaused.collectAsState()
              SwitchPreference(
                value = keepScreenOn,
                onValueChange = { playerPrefs.keepScreenOnWhenPaused.set(it) },
                title = { Text("Keep Screen On When Paused") },
                summary = {
                  Text(
                    "Prevent the device screen from sleeping while video is paused",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 2. Touch Gestures Chart
          item {
            PreferenceSectionHeader(title = "👆 Touch Gestures & Controls")
          }
          item {
            PreferenceCard {
              val brightnessGesture by playerPrefs.brightnessGesture.collectAsState()
              SwitchPreference(
                value = brightnessGesture,
                onValueChange = { playerPrefs.brightnessGesture.set(it) },
                title = { Text("Brightness Gesture") },
                summary = {
                  Text(
                    "Swipe up or down on the left side of the screen to adjust brightness",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Gesture, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val volumeGesture by playerPrefs.volumeGesture.collectAsState()
              SwitchPreference(
                value = volumeGesture,
                onValueChange = { playerPrefs.volumeGesture.set(it) },
                title = { Text("Volume Gesture") },
                summary = {
                  Text(
                    "Swipe up or down on the right side of the screen to adjust volume",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val swipeToSeek by playerPrefs.horizontalSwipeToSeek.collectAsState()
              SwitchPreference(
                value = swipeToSeek,
                onValueChange = { playerPrefs.horizontalSwipeToSeek.set(it) },
                title = { Text("Horizontal Swipe to Seek") },
                summary = {
                  Text(
                    "Swipe horizontally across the screen to quickly scrub timeline",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val pinchToZoom by playerPrefs.pinchToZoomGesture.collectAsState()
              SwitchPreference(
                value = pinchToZoom,
                onValueChange = { playerPrefs.pinchToZoomGesture.set(it) },
                title = { Text("Pinch to Zoom Video") },
                summary = {
                  Text(
                    "Pinch with two fingers to zoom, crop, and fill the display",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val doubleTapOvals by playerPrefs.showDoubleTapOvals.collectAsState()
              SwitchPreference(
                value = doubleTapOvals,
                onValueChange = { playerPrefs.showDoubleTapOvals.set(it) },
                title = { Text("Double-Tap 10s Seek Ripple") },
                summary = {
                  Text(
                    "Double-tap screen edges to skip 10 seconds with animated ripple",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 3. Audio & Volume Chart
          item {
            PreferenceSectionHeader(title = "🔊 Audio & Volume Boost")
          }
          item {
            PreferenceCard {
              val volumeNormalization by audioPrefs.volumeNormalization.collectAsState()
              SwitchPreference(
                value = volumeNormalization,
                onValueChange = { audioPrefs.volumeNormalization.set(it) },
                title = { Text("Volume Normalization") },
                summary = {
                  Text(
                    "Balance dialogue clarity and reduce jarring sudden loud sounds",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val audioPitchCorrection by audioPrefs.audioPitchCorrection.collectAsState()
              SwitchPreference(
                value = audioPitchCorrection,
                onValueChange = { audioPrefs.audioPitchCorrection.set(it) },
                title = { Text("Audio Pitch Correction") },
                summary = {
                  Text(
                    "Maintains natural human pitch when playing at slower or faster speeds",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val backgroundPlayback by audioPrefs.automaticBackgroundPlayback.collectAsState()
              SwitchPreference(
                value = backgroundPlayback,
                onValueChange = { audioPrefs.automaticBackgroundPlayback.set(it) },
                title = { Text("Background Audio Playback") },
                summary = {
                  Text(
                    "Continue playing soundtrack when the screen is locked or app is minimized",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val displayVolumeAsPercentage by playerPrefs.displayVolumeAsPercentage.collectAsState()
              SwitchPreference(
                value = displayVolumeAsPercentage,
                onValueChange = { playerPrefs.displayVolumeAsPercentage.set(it) },
                title = { Text("Display Volume as Percentage") },
                summary = {
                  Text(
                    "Show 0% - 100% (with software volume boost up to 200%)",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 4. Subtitles Chart
          item {
            PreferenceSectionHeader(title = "💬 Subtitles & Captions")
          }
          item {
            PreferenceCard {
              val autoloadSubs by subPrefs.autoloadMatchingSubtitles.collectAsState()
              SwitchPreference(
                value = autoloadSubs,
                onValueChange = { subPrefs.autoloadMatchingSubtitles.set(it) },
                title = { Text("Autoload Matching Subtitles") },
                summary = {
                  Text(
                    "Automatically discover and load local .srt and .ass subtitles",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Subtitles, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val boldSubs by subPrefs.bold.collectAsState()
              SwitchPreference(
                value = boldSubs,
                onValueChange = { subPrefs.bold.set(it) },
                title = { Text("Bold Subtitle Font") },
                summary = {
                  Text(
                    "Render subtitles with heavier font weight for improved legibility",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val scaleByWindow by subPrefs.scaleByWindow.collectAsState()
              SwitchPreference(
                value = scaleByWindow,
                onValueChange = { subPrefs.scaleByWindow.set(it) },
                title = { Text("Scale Subtitles with Window") },
                summary = {
                  Text(
                    "Automatically resize subtitles proportionally across portrait and landscape",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 5. UI & Appearance Chart
          item {
            PreferenceSectionHeader(title = "🎨 UI & AMOLED Display")
          }
          item {
            PreferenceCard {
              val amoledMode by appearancePrefs.amoledMode.collectAsState()
              SwitchPreference(
                value = amoledMode,
                onValueChange = { appearancePrefs.amoledMode.set(it) },
                title = { Text("Pure Black AMOLED Mode") },
                summary = {
                  Text(
                    "Deep #000000 background for infinite contrast and maximum battery savings",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val wavySeekbar by playerPrefs.useWavySeekbar.collectAsState()
              SwitchPreference(
                value = wavySeekbar,
                onValueChange = { playerPrefs.useWavySeekbar.set(it) },
                title = { Text("Fluid Wavy Seekbar") },
                summary = {
                  Text(
                    "Expressive wave animation along the playback scrub rail",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val showLoadingCircle by playerPrefs.showLoadingCircle.collectAsState()
              SwitchPreference(
                value = showLoadingCircle,
                onValueChange = { playerPrefs.showLoadingCircle.set(it) },
                title = { Text("Cinematic Loading Spinner") },
                summary = {
                  Text(
                    "Display clean buffering ring when loading or seeking media",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val unlimitedLines by appearancePrefs.unlimitedNameLines.collectAsState()
              SwitchPreference(
                value = unlimitedLines,
                onValueChange = { appearancePrefs.unlimitedNameLines.set(it) },
                title = { Text("Full Filename Display") },
                summary = {
                  Text(
                    "Do not truncate long filenames with ellipsis in the library",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 6. Media Library Display Chart
          item {
            PreferenceSectionHeader(title = "🗂️ Media Library & Badges")
          }
          item {
            PreferenceCard {
              val showThumbnails by browserPrefs.showVideoThumbnails.collectAsState()
              SwitchPreference(
                value = showThumbnails,
                onValueChange = { browserPrefs.showVideoThumbnails.set(it) },
                title = { Text("Hardware Video Thumbnails") },
                summary = {
                  Text(
                    "Extract fast previews using Android MediaMetadataRetriever",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val showSizeChip by browserPrefs.showSizeChip.collectAsState()
              SwitchPreference(
                value = showSizeChip,
                onValueChange = { browserPrefs.showSizeChip.set(it) },
                title = { Text("Show File Size Badges") },
                summary = {
                  Text(
                    "Display file size in MB/GB on video list items",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val showResolutionChip by browserPrefs.showResolutionChip.collectAsState()
              SwitchPreference(
                value = showResolutionChip,
                onValueChange = { browserPrefs.showResolutionChip.set(it) },
                title = { Text("Show Resolution Badges") },
                summary = {
                  Text(
                    "Display 4K, 1080p, and 720p resolution badges",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val showDateChip by browserPrefs.showDateChip.collectAsState()
              SwitchPreference(
                value = showDateChip,
                onValueChange = { browserPrefs.showDateChip.set(it) },
                title = { Text("Show Date Modified Badges") },
                summary = {
                  Text(
                    "Display file modification dates on media items",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // 7. Specialized Tools & Deep Sub-Menus (Quick Action Tiles)
          item {
            PreferenceSectionHeader(title = "🛠️ Specialized Tools & Configuration")
          }
          item {
            PreferenceCard {
              QuickActionTile(
                title = "Themes & Accent Colors",
                subtitle = "Choose from 15+ curated color palettes & dynamic theme",
                icon = Icons.Outlined.Palette,
                onClick = { backstack.add(AppearancePreferencesScreen) },
              )

              PreferenceDivider()

              QuickActionTile(
                title = "Player Controls Layout Editor",
                subtitle = "Rearrange on-screen HUD buttons for portrait & landscape",
                icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                onClick = { backstack.add(PlayerControlsPreferencesScreen) },
              )

              PreferenceDivider()

              QuickActionTile(
                title = "Folders & Exclusions",
                subtitle = "Manage scanned media directories and hidden storage paths",
                icon = Icons.Outlined.Folder,
                onClick = { backstack.add(FoldersPreferencesScreen) },
              )

              PreferenceDivider()

              QuickActionTile(
                title = "Advanced Parameters & Cache",
                subtitle = "Inspect low-level buffer parameters and clear thumbnail cache",
                icon = Icons.Outlined.Code,
                onClick = { backstack.add(AdvancedPreferencesScreen) },
              )

              PreferenceDivider()

              QuickActionTile(
                title = "About Apna Player",
                subtitle = "Version info, device hardware telemetry, and credits",
                icon = Icons.Outlined.Info,
                onClick = { backstack.add(AboutScreen) },
              )
            }
          }

          item {
            Spacer(modifier = Modifier.height(32.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun StatusBadge(
  text: String,
  active: Boolean = true,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun QuickActionTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
      )
    }
  }
}
