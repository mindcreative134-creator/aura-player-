package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom A-B Loop icon rendering two overlapping outlined circles with letters 'A' and 'B' inside,
 * matching modern MPV Infinity player controls aesthetics. Pure Compose implementation for 100% crash safety.
 */
@Composable
fun AbLoopIcon(
  modifier: Modifier = Modifier,
  tint: Color = LocalContentColor.current,
  size: Dp = 24.dp,
) {
  val safeTint = if (tint.isSpecified) tint else MaterialTheme.colorScheme.onSurface
  val circleSize = size * 0.58f
  val strokeWidth = 1.4.dp
  val fontSize = (size.value * 0.32f).sp

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Circle A
      Box(
        modifier = Modifier
          .size(circleSize)
          .border(BorderStroke(strokeWidth, safeTint), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "A",
          color = safeTint,
          fontSize = fontSize,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          textAlign = TextAlign.Center,
          lineHeight = fontSize,
        )
      }

      // Circle B (slightly overlapping A)
      Box(
        modifier = Modifier
          .offset(x = -(size * 0.16f))
          .size(circleSize)
          .border(BorderStroke(strokeWidth, safeTint), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "B",
          color = safeTint,
          fontSize = fontSize,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.SansSerif,
          textAlign = TextAlign.Center,
          lineHeight = fontSize,
        )
      }
    }
  }
}

