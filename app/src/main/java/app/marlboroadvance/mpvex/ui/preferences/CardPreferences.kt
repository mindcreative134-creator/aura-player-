package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens

/**
 * Signature Aura preference card container for grouping related settings.
 * Designed with carbon elevation and refined hairline borders.
 */
@Composable
fun PreferenceCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = AuraTokens.CarbonCard,
    ),
    border = BorderStroke(1.dp, AuraTokens.CarbonBorder),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 0.dp,
    ),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
      content()
    }
  }
}

/**
 * A divider to separate preferences within a card.
 */
@Composable
fun PreferenceDivider(
  modifier: Modifier = Modifier,
) {
  HorizontalDivider(
    modifier = modifier.padding(horizontal = 16.dp),
    color = AuraTokens.CarbonBorder.copy(alpha = 0.6f),
  )
}

/**
 * A section header for preferences, displayed outside cards.
 */
@Composable
fun PreferenceSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall.copy(
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.3.sp,
    ),
    color = AuraTokens.AuraCyan,
    modifier = modifier.padding(horizontal = 24.dp, vertical = 14.dp),
  )
}

