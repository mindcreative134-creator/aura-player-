package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.theme.spacing
import com.cmstudio.apnaplayer.engine.PlayerBridge
import com.cmstudio.apnaplayer.engine.PlayerBridgeUtils

@Composable
fun VideoClipSheet(
  viewModel: PlayerViewModel,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val position by PlayerBridge.propInt["time-pos"].collectAsState()
  val duration by PlayerBridge.propInt["duration"].collectAsState()
  val currentPos = (position ?: 0).coerceAtLeast(0)
  val totalDuration = (duration ?: 0).coerceAtLeast(0)

  var startSeconds by remember { mutableIntStateOf(currentPos) }
  var endSeconds by remember {
    mutableIntStateOf(
      if (totalDuration > 0) (currentPos + 30).coerceAtMost(totalDuration) else currentPos + 30
    )
  }

  val clipDuration = (endSeconds - startSeconds).coerceAtLeast(0)

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
            imageVector = Icons.Outlined.ContentCut,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
          )
          Column {
            Text(
              text = "Clip & Trim Video",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Set start and end bounds to trim or loop excerpts",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      // Clip Duration Status Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Clip Segment",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.outline,
            )
            Text(
              text = "${PlayerBridgeUtils.prettyTime(startSeconds)} ➔ ${PlayerBridgeUtils.prettyTime(endSeconds)}",
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = "Duration: ${PlayerBridgeUtils.prettyTime(clipDuration)}",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }

      // Start Time Adjuster
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
              text = "Start Point",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = PlayerBridgeUtils.prettyTime(startSeconds),
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
          ) {
            FilledTonalButton(
              onClick = { startSeconds = (startSeconds - 5).coerceAtLeast(0) },
              modifier = Modifier.weight(1f),
            ) { Text("-5s") }
            FilledTonalButton(
              onClick = { startSeconds = (startSeconds - 1).coerceAtLeast(0) },
              modifier = Modifier.weight(1f),
            ) { Text("-1s") }
            Button(
              onClick = { startSeconds = currentPos.coerceAtLeast(0) },
              modifier = Modifier.weight(2f),
            ) {
              Text("Set Current")
            }
            FilledTonalButton(
              onClick = { startSeconds = (startSeconds + 1).coerceAtMost(endSeconds) },
              modifier = Modifier.weight(1f),
            ) { Text("+1s") }
            FilledTonalButton(
              onClick = { startSeconds = (startSeconds + 5).coerceAtMost(endSeconds) },
              modifier = Modifier.weight(1f),
            ) { Text("+5s") }
          }
        }
      }

      // End Time Adjuster
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
              text = "End Point",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = PlayerBridgeUtils.prettyTime(endSeconds),
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
          ) {
            FilledTonalButton(
              onClick = { endSeconds = (endSeconds - 5).coerceAtLeast(startSeconds) },
              modifier = Modifier.weight(1f),
            ) { Text("-5s") }
            FilledTonalButton(
              onClick = { endSeconds = (endSeconds - 1).coerceAtLeast(startSeconds) },
              modifier = Modifier.weight(1f),
            ) { Text("-1s") }
            Button(
              onClick = { endSeconds = currentPos.coerceAtLeast(startSeconds) },
              modifier = Modifier.weight(2f),
            ) {
              Text("Set Current")
            }
            FilledTonalButton(
              onClick = {
                val maxLimit = if (totalDuration > 0) totalDuration else endSeconds + 1
                endSeconds = (endSeconds + 1).coerceAtMost(maxLimit)
              },
              modifier = Modifier.weight(1f),
            ) { Text("+1s") }
            FilledTonalButton(
              onClick = {
                val maxLimit = if (totalDuration > 0) totalDuration else endSeconds + 5
                endSeconds = (endSeconds + 5).coerceAtMost(maxLimit)
              },
              modifier = Modifier.weight(1f),
            ) { Text("+5s") }
          }
        }
      }

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        Button(
          onClick = {
            viewModel.seekTo(startSeconds)
            viewModel.unpause()
            onDismissRequest()
          },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("Preview Clip")
        }

        FilledTonalButton(
          onClick = {
            // Apply bounds directly to A-B loop via PlayerViewModel
            viewModel.setABLoopRange(startSeconds.toDouble(), endSeconds.toDouble())
            viewModel.seekTo(startSeconds)
            viewModel.unpause()
            Toast.makeText(context, "A-B Loop active: ${PlayerBridgeUtils.prettyTime(startSeconds)} ➔ ${PlayerBridgeUtils.prettyTime(endSeconds)}", Toast.LENGTH_SHORT).show()
            onDismissRequest()
          },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("A-B Loop Clip")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        OutlinedButton(
          onClick = {
            val title = viewModel.currentMediaTitle.ifBlank { "Video" }
            val clipText = "🎬 $title\n⏱️ Clip: ${PlayerBridgeUtils.prettyTime(startSeconds)} ➔ ${PlayerBridgeUtils.prettyTime(endSeconds)} (Length: ${PlayerBridgeUtils.prettyTime(clipDuration)})"
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Video Clip", clipText)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("Copy Info")
        }

        OutlinedButton(
          onClick = {
            runCatching {
              val title = viewModel.currentMediaTitle.ifBlank { "Video" }
              val clipShareText = "🎬 $title\n⏱️ Clip Segment: ${PlayerBridgeUtils.prettyTime(startSeconds)} ➔ ${PlayerBridgeUtils.prettyTime(endSeconds)} (${PlayerBridgeUtils.prettyTime(clipDuration)})\nSaved via Aura Player"
              val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, clipShareText)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Video Clip: $title")
                type = "text/plain"
              }
              val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Video Clip")
              context.startActivity(shareIntent)
            }.onFailure {
              Toast.makeText(context, "Unable to launch share dialog", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f),
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(4.dp))
          Text("Share Clip")
        }
      }

      FilledTonalButton(
        onClick = {
          viewModel.clearABLoop()
          Toast.makeText(context, "A-B Loop reset", Toast.LENGTH_SHORT).show()
          onDismissRequest()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
      ) {
        Text("Reset / Clear Loop")
      }
    }
  }
}
