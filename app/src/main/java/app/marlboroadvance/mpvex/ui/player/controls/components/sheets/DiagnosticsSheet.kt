package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import com.cmstudio.apnaplayer.engine.ExoPlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DiagnosticsSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var stats by remember { mutableStateOf(ExoPlayerHolder.getDiagnostics(context)) }

  // 500ms real-time refresh ticker
  LaunchedEffect(Unit) {
    while (isActive) {
      stats = ExoPlayerHolder.getDiagnostics(context)
      delay(500)
    }
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
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            tint = Color(0xFF00F2FE),
          )
          Text(
            text = "Playback Diagnostics",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        }
        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Engine status banner
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(RoundedCornerShape(5.dp))
              .background(if (stats.hardwareAccelerated) Color(0xFF10B981) else Color(0xFFF59E0B)),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Apna Hardware Engine (Google Media3 Core)",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = Color.White,
            )
            Text(
              text = if (stats.hardwareAccelerated) "Direct Surface · HW Accelerated" else "Software Fallback Rendering",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
            )
          }
          Surface(
            color = Color(0xFF334155),
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = stats.thermalState,
              style = MaterialTheme.typography.labelSmall,
              color = if (stats.thermalState == "Nominal" || stats.thermalState == "Normal") Color(0xFF10B981) else Color(0xFFEF4444),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      // Video Pipeline Section
      DiagnosticSectionCard(
        title = "VIDEO DECODER & DISPLAY",
        icon = Icons.Default.PlayCircle,
      ) {
        DiagnosticRow("Resolution", stats.resolution)
        DiagnosticRow("Color Space", stats.colorInfo)
        DiagnosticRow("Video Codec", stats.videoCodec)
        DiagnosticRow("Active Decoder", stats.activeVideoDecoder, isMonospace = true)
        DiagnosticRow(
          "Frame Rate",
          if (stats.videoFps > 0f) String.format("%.2f FPS", stats.videoFps) else "Auto / Stream",
        )
        DiagnosticRow(
          "Dropped Frames",
          "${stats.droppedFrames}",
          highlightColor = if (stats.droppedFrames > 0) Color(0xFFEF4444) else null,
        )
        if (stats.videoBitrateKbps > 0) {
          DiagnosticRow("Video Bitrate", "${stats.videoBitrateKbps} kbps")
        }
      }

      // Audio Pipeline Section
      DiagnosticSectionCard(
        title = "AUDIO PIPELINE",
        icon = Icons.Default.Audiotrack,
      ) {
        DiagnosticRow("Audio Codec", stats.audioCodec)
        DiagnosticRow("Active Decoder", stats.activeAudioDecoder, isMonospace = true)
        DiagnosticRow("Channels", stats.audioChannels)
        DiagnosticRow("Sample Rate", "${stats.audioSampleRate} Hz")
        DiagnosticRow("Audio Session", "#${ExoPlayerHolder.audioSessionId}")
        if (stats.audioBitrateKbps > 0) {
          DiagnosticRow("Audio Bitrate", "${stats.audioBitrateKbps} kbps")
        }
      }

      // Buffer & Network Section
      DiagnosticSectionCard(
        title = "BUFFER & MEMORY",
        icon = Icons.Default.Speed,
      ) {
        DiagnosticRow("Buffer Ahead", "${stats.bufferDurationMs} ms")
        DiagnosticRow("Cached Percentage", "${stats.bufferedPercentage}%")
        DiagnosticRow("Playback Speed", "${stats.playbackSpeed}x")
      }

      // Error Section if present
      stats.lastError?.let { err ->
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Recent Playback Alert",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = err,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
              fontFamily = FontFamily.Monospace,
            )
          }
        }
      }

      // Device Info
      Text(
        text = "Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 4.dp),
      )

      // Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            ExoPlayerHolder.resetFrameCounters()
            stats = ExoPlayerHolder.getDiagnostics(context)
            Toast.makeText(context, "Counters reset", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.weight(1f),
        ) {
          Text("Reset Counters")
        }

        Button(
          onClick = {
            val log = buildString {
              appendLine("=== MPV EX Playback Diagnostic Report ===")
              appendLine("App: MPV EX (Apna Player 1.3.1)")
              appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
              appendLine("Engine: Google Media3 ExoPlayer HW+")
              appendLine("Resolution: ${stats.resolution} (${stats.colorInfo})")
              appendLine("Video Codec: ${stats.videoCodec} via ${stats.activeVideoDecoder}")
              appendLine("Video FPS: ${stats.videoFps} | Dropped Frames: ${stats.droppedFrames}")
              appendLine("Audio: ${stats.audioCodec} (${stats.audioChannels}, ${stats.audioSampleRate}Hz) via ${stats.activeAudioDecoder}")
              appendLine("Buffer Ahead: ${stats.bufferDurationMs}ms (${stats.bufferedPercentage}%)")
              appendLine("Thermal State: ${stats.thermalState}")
              if (stats.lastError != null) {
                appendLine("Last Error: ${stats.lastError}")
              }
            }
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("MPV EX Diagnostics", log))
            Toast.makeText(context, "Diagnostic report copied to clipboard", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copy Report")
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
  }
}

@Composable
private fun DiagnosticSectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    shape = RoundedCornerShape(12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          letterSpacing = 1.sp,
        )
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      content()
    }
  }
}

@Composable
private fun DiagnosticRow(
  label: String,
  value: String,
  isMonospace: Boolean = false,
  highlightColor: Color? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
      color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
    )
  }
}
