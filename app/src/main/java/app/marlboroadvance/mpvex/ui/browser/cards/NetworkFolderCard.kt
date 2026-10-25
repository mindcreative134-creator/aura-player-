package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
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
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.domain.network.NetworkFile
import androidx.compose.foundation.combinedClickable
import org.koin.compose.koinInject
import androidx.compose.foundation.BorderStroke
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun NetworkFolderCard(
  file: NetworkFile,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

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
            .size(48.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(AuraTheme.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.Filled.Folder,
          contentDescription = "Folder",
          modifier = Modifier.size(26.dp),
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
      }
    }
  }
}

