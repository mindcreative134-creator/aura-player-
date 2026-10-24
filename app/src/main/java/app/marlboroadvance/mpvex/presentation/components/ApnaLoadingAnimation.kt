package app.marlboroadvance.mpvex.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Clean, simple, and modern animated loading indicator for Apna Player.
 * Features a minimalist gradient spinner ring with clear, elegant typography.
 */
@Composable
fun ApnaLoadingAnimation(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  title: String? = null,
  subtitle: String? = null,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "apna_clean_loading")

  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "loading_rotation"
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.tertiary

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier.size(size),
      contentAlignment = Alignment.Center
    ) {
      Canvas(
        modifier = Modifier
          .size(size)
          .rotate(rotation)
      ) {
        val strokeWidthPx = 4.dp.toPx()
        val arcSize = Size(size.toPx() - strokeWidthPx, size.toPx() - strokeWidthPx)
        val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

        // Subtle background track
        drawArc(
          color = primaryColor.copy(alpha = 0.15f),
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Active spinning gradient arc
        drawArc(
          brush = Brush.sweepGradient(
            0.0f to primaryColor.copy(alpha = 0.05f),
            0.5f to secondaryColor,
            1.0f to primaryColor
          ),
          startAngle = 0f,
          sweepAngle = 280f,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
      }
    }

    if (title != null) {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
