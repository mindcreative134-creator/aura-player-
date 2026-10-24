package app.marlboroadvance.mpvex.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens

/**
 * Signature Aura Player Buffering Indicator.
 * Delivers a crisp, lightweight circular ring with smooth indeterminate rotation
 * and soft ambient cyan glow without unnecessary visual clutter.
 */
@Composable
fun AuraPlayerBufferingIndicator(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  ringWidth: Dp = 3.5.dp,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "aura_player_buffering")

  // Smooth rotation
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "buffering_rotation",
  )

  // Gentle central pulse
  val centerPulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "center_pulse",
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val accentColor = AuraTokens.AuraCyan

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    // Semi-transparent frosted circular backdrop
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(AuraTokens.ObsidianSurface.copy(alpha = 0.75f)),
    )

    // Smooth spinning clean arc
    Canvas(
      modifier = Modifier
        .size(size - 18.dp)
        .rotate(rotation),
    ) {
      val strokeWidthPx = ringWidth.toPx()
      val arcSize = Size(size.toPx() - 18.dp.toPx() - strokeWidthPx, size.toPx() - 18.dp.toPx() - strokeWidthPx)
      val topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

      // Background subtle track
      drawArc(
        color = Color.White.copy(alpha = 0.10f),
        startAngle = 0f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
      )

      // Active spinning gradient arc
      drawArc(
        brush = Brush.sweepGradient(
          0.0f to primaryColor.copy(alpha = 0.05f),
          0.6f to accentColor,
          1.0f to primaryColor,
        ),
        startAngle = 0f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
      )
    }

    // Elegant minimal center dot
    Box(
      modifier = Modifier
        .size(8.dp * centerPulse)
        .clip(CircleShape)
        .background(accentColor),
    )
  }
}

/**
 * Backward compatibility wrapper for existing call-sites
 */
@Composable
fun ApnaPlayerBufferingIndicator(
  modifier: Modifier = Modifier,
  size: Dp = 64.dp,
  ringWidth: Dp = 3.5.dp,
) {
  AuraPlayerBufferingIndicator(
    modifier = modifier,
    size = size,
    ringWidth = ringWidth,
  )
}

