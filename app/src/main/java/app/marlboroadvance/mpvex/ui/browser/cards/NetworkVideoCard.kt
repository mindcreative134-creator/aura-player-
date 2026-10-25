package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.domain.network.NetworkConnection
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NetworkVideoCard(
  file: NetworkFile,
  connection: NetworkConnection,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showSizeChip by browserPreferences.showSizeChip.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  val thumbSizeDp = 54.dp

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .combinedClickable(
          onClick = onClick,
          onLongClick = onLongClick,
        ),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) AuraTheme.selectedContainer else AuraTheme.cardBackground,
    ),
    border = BorderStroke(
      1.dp,
      if (isSelected) AuraTheme.accent.copy(alpha = 0.5f) else AuraTheme.cardBorder,
    ),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(thumbSizeDp)
            .clip(RoundedCornerShape(12.dp))
            .background(AuraTheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.PlayArrow,
          contentDescription = "Play",
          modifier = Modifier.size(28.dp),
          tint = AuraTheme.accent,
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          file.name,
          style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
          fontWeight = FontWeight.SemiBold,
          color = AuraTheme.textPrimary,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
          horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
          verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
        ) {
          if (showSizeChip && file.size > 0) {
            Text(
              formatFileSize(file.size),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              modifier =
                Modifier
                  .background(
                    AuraTheme.chipBackground,
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 7.dp, vertical = 3.dp),
              color = AuraTheme.chipContent,
            )
          }
          if (file.lastModified > 0) {
            Text(
              formatDate(file.lastModified),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              modifier =
                Modifier
                  .background(
                    AuraTheme.chipBackground,
                    RoundedCornerShape(6.dp),
                  )
                  .padding(horizontal = 7.dp, vertical = 3.dp),
              color = AuraTheme.chipContent,
            )
          }
        }
      }
    }
  }
}

private fun formatFileSize(bytes: Long): String {
  return when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
  }
}

private fun formatDate(timestamp: Long): String {
  val date = Date(timestamp)
  val format = SimpleDateFormat("MMM dd", Locale.getDefault())
  return format.format(date)
}
