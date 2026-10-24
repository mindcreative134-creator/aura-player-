package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VolumeUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object DecoderPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    val backstack = LocalBackStack.current

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Decoder & Hardware Pipeline",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
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
          // Status Header Card
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Outlined.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Apna Ultra-HD Hardware Acceleration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Direct GPU-accelerated video decoding engine for flawless 4K, 60fps, 120fps, and HDR playback with optimal battery efficiency.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  DecoderChip("H.264 (AVC)")
                  DecoderChip("HEVC (H.265)")
                  DecoderChip("VP9")
                  DecoderChip("AV1")
                  DecoderChip("HDR10+")
                  DecoderChip("Dolby Vision")
                }
              }
            }
          }

          // Hardware Video Decoders
          item {
            PreferenceSectionHeader(title = "Video Decoders & Acceleration")
          }

          item {
            PreferenceCard {
              val tryHWDecoding by preferences.tryHWDecoding.collectAsState()
              SwitchPreference(
                value = tryHWDecoding,
                onValueChange = { preferences.tryHWDecoding.set(it) },
                title = { Text("Hardware Decoding (MediaCodec)") },
                summary = {
                  Text(
                    "Utilize device GPU and hardware VPU for stutter-free 4K/60fps playback",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              val decoderFallback by preferences.decoderFallback.collectAsState()
              SwitchPreference(
                value = decoderFallback,
                onValueChange = { preferences.decoderFallback.set(it) },
                title = { Text("Automatic Decoder Fallback") },
                summary = {
                  Text(
                    "Seamlessly switch to software decoding if hardware decoder encounters unsupported or corrupted bitstreams",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val tunnelingEnabled by preferences.tunnelingEnabled.collectAsState()
              SwitchPreference(
                value = tunnelingEnabled,
                onValueChange = { preferences.tunnelingEnabled.set(it) },
                title = { Text("Video Tunneling & Direct Surface") },
                summary = {
                  Text(
                    "Direct hardware synchronization between video and audio for lowest latency and maximum battery efficiency",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // Audio Hardware Pipeline
          item {
            PreferenceSectionHeader(title = "Audio Engine & Processing")
          }

          item {
            PreferenceCard {
              val audioOffload by preferences.audioOffload.collectAsState()
              SwitchPreference(
                value = audioOffload,
                onValueChange = { preferences.audioOffload.set(it) },
                title = { Text("Audio Offload to DSP") },
                summary = {
                  Text(
                    "Direct audio bitstream to digital signal processor hardware to reduce CPU wakeups",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )
            }
          }

          // Adaptive Buffering Engine
          item {
            PreferenceSectionHeader(title = "Buffer & Streaming")
          }

          item {
            PreferenceCard {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Outlined.Speed,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text(
                    text = "Intelligent Adaptive Buffering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "Min Buffer: 15s • Max Buffer: 50s • Playback Start: 2.5s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
              }
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
private fun DecoderChip(name: String) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}
