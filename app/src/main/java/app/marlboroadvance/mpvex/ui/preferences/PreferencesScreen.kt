package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.vault.PrivateVaultScreen
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
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
                tint = AuraTokens.AuraCyan,
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
              shape = RoundedCornerShape(18.dp),
              color = AuraTheme.cardBackground,
              border = BorderStroke(1.dp, AuraTheme.cardBorder),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = null,
                  tint = AuraTheme.accent,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Search settings, toggles & parameters...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = AuraTheme.textSecondary,
                )
              }
            }
          }

          // 1. Quick Essential Toggles
          item {
            PreferenceSectionHeader(title = "⚡ Essential Controls")
          }
          item {
            PreferenceCard {
              val tryHWDecoding by decoderPrefs.tryHWDecoding.collectAsState()
              SwitchPreference(
                value = tryHWDecoding,
                onValueChange = { decoderPrefs.tryHWDecoding.set(it) },
                title = { Text("Hardware Acceleration (MediaCodec)", color = AuraTheme.textPrimary) },
                summary = {
                  Text(
                    "Utilize GPU hardware decoding for ultra-smooth 4K/60fps playback",
                    color = AuraTheme.textSecondary,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Memory, contentDescription = null, tint = AuraTheme.accent)
                },
              )

              PreferenceDivider()

              val savePosition by playerPrefs.savePositionOnQuit.collectAsState()
              SwitchPreference(
                value = savePosition,
                onValueChange = { playerPrefs.savePositionOnQuit.set(it) },
                title = { Text("Auto-Resume Playback", color = AuraTheme.textPrimary) },
                summary = {
                  Text(
                    "Remember and automatically resume video from previous timestamp",
                    color = AuraTheme.textSecondary,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = AuraTheme.accent)
                },
              )

              PreferenceDivider()

              val backgroundPlayback by audioPrefs.automaticBackgroundPlayback.collectAsState()
              SwitchPreference(
                value = backgroundPlayback,
                onValueChange = { audioPrefs.automaticBackgroundPlayback.set(it) },
                title = { Text("Background Audio Playback", color = AuraTheme.textPrimary) },
                summary = {
                  Text(
                    "Continue soundtrack playback when app is minimized or screen locked",
                    color = AuraTheme.textSecondary,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Audiotrack, contentDescription = null, tint = AuraTheme.accent)
                },
              )
            }
          }

          // 2. Playback & Experience Categories
          item {
            PreferenceSectionHeader(title = "🎬 Playback & Player Experience")
          }
          item {
            PreferenceCard {
              CategoryTile(
                title = "Video Playback Engine",
                subtitle = "Resume, autoplay next video, picture-in-picture, orientation",
                icon = Icons.Outlined.PlayCircle,
                onClick = { backstack.add(PlayerPreferencesScreen) },
                tileColor = Color(0xFF00C853),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Touch Gestures & Controls",
                subtitle = "Brightness/volume swipes, double-tap seek, horizontal scrubbing",
                icon = Icons.Outlined.Gesture,
                onClick = { backstack.add(GesturePreferencesScreen) },
                tileColor = Color(0xFF2979FF),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Player HUD & Button Layout",
                subtitle = "Configure visible on-screen buttons for portrait and landscape",
                icon = Icons.AutoMirrored.Outlined.ViewQuilt,
                onClick = { backstack.add(PlayerControlsPreferencesScreen) },
                tileColor = Color(0xFFFF9100),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Audio & Music Engine",
                subtitle = "Volume normalization, pitch correction, 200% volume boost",
                icon = Icons.Outlined.Audiotrack,
                onClick = { backstack.add(AudioPreferencesScreen) },
                tileColor = Color(0xFFFFB300),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Subtitles & Captions",
                subtitle = "Autoload, custom font folder, font sizing, colors, encoding",
                icon = Icons.Outlined.Subtitles,
                onClick = { backstack.add(SubtitlesPreferencesScreen) },
                tileColor = Color(0xFF00BCD4),
              )
            }
          }

          // 3. UI, System & Storage
          item {
            PreferenceSectionHeader(title = "🎨 Visuals, Library & Storage")
          }
          item {
            PreferenceCard {
              CategoryTile(
                title = "Appearance & Theming",
                subtitle = "AMOLED black, 15+ curated color palettes, dynamic theme",
                icon = Icons.Outlined.Palette,
                onClick = { backstack.add(AppearancePreferencesScreen) },
                tileColor = Color(0xFF7C4DFF),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Folders & Exclusions",
                subtitle = "Manage scanned directories, hidden paths, storage filters",
                icon = Icons.Outlined.Folder,
                onClick = { backstack.add(FoldersPreferencesScreen) },
                tileColor = Color(0xFFFF5722),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Hardware Decoder & Filters",
                subtitle = "GPU video decoding, debanding, video filter presets",
                icon = Icons.Outlined.Memory,
                onClick = { backstack.add(DecoderPreferencesScreen) },
                tileColor = Color(0xFF00B4D8),
              )

              PreferenceDivider()

              CategoryTile(
                title = "Private Vault & Security",
                subtitle = "PIN protection, encrypted media storage, hidden safe",
                icon = Icons.Outlined.Lock,
                onClick = { backstack.add(PrivateVaultScreen) },
                tileColor = Color(0xFFE91E63),
              )
            }
          }

          // 4. Advanced & Information
          item {
            PreferenceSectionHeader(title = "ℹ️ System & Diagnostics")
          }
          item {
            PreferenceCard {
              CategoryTile(
                title = "Advanced Parameters & Cache",
                subtitle = "Buffer sizes, thumbnail cache, custom MPV parameters",
                icon = Icons.Outlined.Code,
                onClick = { backstack.add(AdvancedPreferencesScreen) },
                tileColor = Color(0xFF651FFF),
              )

              PreferenceDivider()

              CategoryTile(
                title = "About Aura Player",
                subtitle = "Version, device telemetry, licenses, and open source credits",
                icon = Icons.Outlined.Info,
                onClick = { backstack.add(AboutScreen) },
                tileColor = Color(0xFF00897B),
              )
            }
          }

          item {
            Spacer(modifier = Modifier.height(36.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  onClick: () -> Unit,
  tileColor: Color = AuraTheme.accent,
  iconTint: Color = Color.White,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(13.dp),
      color = tileColor,
      modifier = Modifier.size(42.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(
      modifier = Modifier.weight(1f),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AuraTheme.textPrimary,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = AuraTheme.textSecondary,
      )
    }
    Icon(
      imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
      contentDescription = null,
      tint = AuraTheme.textSecondary.copy(alpha = 0.5f),
      modifier = Modifier.size(18.dp),
    )
  }
}

