package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import com.cmstudio.apnaplayer.engine.PlayerBridge
import org.koin.compose.koinInject

@Composable
fun VideoQualitySheet(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val decoderPreferences = koinInject<DecoderPreferences>()
  val videoTracks by viewModel.videoTracks.collectAsState()
  var smoothMotionEnabled by remember { mutableStateOf(decoderPreferences.smoothMotion.get()) }
  var targetFps by remember { mutableStateOf(decoderPreferences.smoothMotionTargetFps.get()) }
  var selectedScaler by remember { mutableStateOf(decoderPreferences.videoScalingProfile.get()) }
  var selectedToneMapping by remember { mutableStateOf(decoderPreferences.hdrToneMapping.get()) }

  // Query live video playback metrics
  val videoWidth = remember { PlayerBridge.getPropertyInt("video-params/w") ?: 0 }
  val videoHeight = remember { PlayerBridge.getPropertyInt("video-params/h") ?: 0 }
  val fps = remember { PlayerBridge.getPropertyDouble("video-params/fps") ?: 0.0 }
  val codec = remember { PlayerBridge.getPropertyString("video-codec") ?: "Unknown" }
  val hwdec = remember { PlayerBridge.getPropertyString("hwdec-current") ?: "None" }

  val resolutionLabel = when {
    videoHeight >= 2160 -> "4K UHD"
    videoHeight >= 1440 -> "2K QHD"
    videoHeight >= 1080 -> "1080p FHD"
    videoHeight >= 720 -> "720p HD"
    videoHeight >= 480 -> "480p SD"
    videoHeight > 0 -> "${videoHeight}p"
    else -> "HD Auto"
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Icon(
            imageVector = Icons.Outlined.Hd,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
          Column {
            Text(
              text = "Video Quality & Display",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Stream resolution, 120 FPS motion & scalers",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Realtime Video Stream Details Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Active Stream Info",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text = resolutionLabel,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("Resolution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              Text(
                text = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x$videoHeight" else "Adaptive Stream",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
              )
            }
            Column {
              Text("Codec & Engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              Text(
                text = "$codec (${if (hwdec != "no" && hwdec != "None") "HW: $hwdec" else "SW"})",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
              )
            }
            Column {
              Text("Frame Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              Text(
                text = if (fps > 0) String.format("%.2f FPS", fps) else "Variable",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }

      // Smooth Motion (Frame Interpolation - MPV Infinity v2.2.0)
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                Icons.Default.Speed,
                contentDescription = null,
                tint = if (smoothMotionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
              )
              Column {
                Text(
                  text = "Smooth Motion (Frame Interpolation)",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "GPU real-time frame generation up to 120 FPS",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Switch(
              checked = smoothMotionEnabled,
              onCheckedChange = { enabled ->
                smoothMotionEnabled = enabled
                decoderPreferences.smoothMotion.set(enabled)
                if (enabled) {
                  PlayerBridge.setPropertyString("video-sync", "display-resample")
                  PlayerBridge.setPropertyString("interpolation", "yes")
                  PlayerBridge.setPropertyString("tscale", "oversample")
                } else {
                  PlayerBridge.setPropertyString("interpolation", "no")
                }
              },
            )
          }

          if (smoothMotionEnabled) {
            Text(
              text = "Target Refresh Rate:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.outline,
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
              listOf(60, 90, 120).forEach { rate ->
                FilterChip(
                  selected = targetFps == rate,
                  onClick = {
                    targetFps = rate
                    decoderPreferences.smoothMotionTargetFps.set(rate)
                  },
                  label = { Text("${rate} FPS") },
                  leadingIcon = if (targetFps == rate) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                  } else null,
                )
              }
            }
          }
        }
      }

      // Stream Quality & Resolution Presets
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
          ) {
            Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = "Stream Quality / Resolution",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }

          if (videoTracks.size > 1) {
            Text(
              text = "Available Video Tracks:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline,
            )
            videoTracks.forEach { track ->
              val isSelected = track.isSelected
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    runCatching {
                      PlayerBridge.setPropertyInt("vid", track.id)
                      Toast.makeText(context, "Selected Video Track #${track.id}", Toast.LENGTH_SHORT).show()
                    }
                  }
                  .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "Track #${track.id} • ${track.demuxW ?: videoWidth}x${track.demuxH ?: videoHeight} (${track.codec ?: codec})",
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          } else {
            // Resolution stream presets (HLS / DASH / Web Video / YouTube)
            val streamResolutions = listOf(
              "Auto" to "Best Available Quality",
              "2160p" to "4K Ultra HD",
              "1080p" to "Full HD (Recommended)",
              "720p" to "High Definition",
              "480p" to "Standard Definition",
              "360p" to "Data Saver",
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              streamResolutions.forEach { (resKey, resTitle) ->
                val isSelected = (resKey == "Auto" && videoHeight == 0) ||
                  (resKey == "1080p" && videoHeight in 1000..1200) ||
                  (resKey == "2160p" && videoHeight >= 2000) ||
                  (resKey == "720p" && videoHeight in 700..800) ||
                  (resKey == "480p" && videoHeight in 400..500) ||
                  (resKey == "360p" && videoHeight in 300..400)

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      runCatching {
                        val format = when (resKey) {
                          "2160p" -> "bestvideo[height<=2160]+bestaudio/best[height<=2160]"
                          "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]"
                          "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]"
                          "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]"
                          "360p" -> "bestvideo[height<=360]+bestaudio/best[height<=360]"
                          else -> "bestvideo+bestaudio/best"
                        }
                        PlayerBridge.setOptionString("ytdl-format", format)
                        Toast.makeText(context, "Stream Quality: $resTitle ($resKey)", Toast.LENGTH_SHORT).show()
                      }
                      onDismissRequest()
                    }
                  .padding(vertical = 6.dp, horizontal = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(
                      text = resTitle,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      }

      // Scaling Profile & Video Enhancements
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(
          modifier = Modifier.padding(MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
          ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text = "Scaler & Visual Filters",
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }

          Text(
            text = "Video Scaler Algorithm:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
          ) {
            listOf(
              "high_quality" to "EWA Lanczos",
              "spline36" to "Spline36",
              "bilinear" to "Bilinear",
            ).forEach { (key, name) ->
              FilterChip(
                selected = selectedScaler == key,
                onClick = {
                  selectedScaler = key
                  decoderPreferences.videoScalingProfile.set(key)
                  val mpvScale = when (key) {
                    "high_quality" -> "ewa_lanczos"
                    "spline36" -> "spline36"
                    else -> "bilinear"
                  }
                  PlayerBridge.setPropertyString("scale", mpvScale)
                },
                label = { Text(name) },
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "HDR Tone-mapping Profile:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
          ) {
            listOf(
              "auto" to "Auto",
              "bt.2390" to "BT.2390",
              "reinhard" to "Reinhard",
              "clip" to "Clip",
            ).forEach { (modeKey, modeName) ->
              FilterChip(
                selected = selectedToneMapping == modeKey,
                onClick = {
                  selectedToneMapping = modeKey
                  decoderPreferences.hdrToneMapping.set(modeKey)
                  PlayerBridge.setPropertyString("tone-mapping", modeKey)
                },
                label = { Text(modeName) },
              )
            }
          }
        }
      }
    }
  }
}
