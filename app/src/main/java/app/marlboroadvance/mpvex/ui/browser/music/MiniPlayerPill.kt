package app.marlboroadvance.mpvex.ui.browser.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager

/**
 * Modern floating mini-player pill inspired by Void-Player's dynamic island.
 * Docks smoothly right above the bottom navigation bar.
 */
@UnstableApi
@Composable
fun MiniPlayerPill(
  playerManager: ApnaAudioPlayerManager,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val state by playerManager.state.collectAsState()
  val currentPos by playerManager.position.collectAsState()

  val progress = remember(currentPos, state.durationMs) {
    if (state.durationMs > 0L) (currentPos.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
  }

  AnimatedVisibility(
    visible = state.hasMedia,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    modifier = modifier,
  ) {
    if (state.hasMedia) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 6.dp)
          .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00F2FE).copy(alpha = 0.35f))
          .clip(RoundedCornerShape(22.dp))
          .border(
            width = 1.dp,
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF00F2FE).copy(alpha = 0.40f),
                Color(0xFF4FACFE).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.12f),
              )
            ),
            shape = RoundedCornerShape(22.dp),
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onClick()
            },
          ),
        color = Color(0xF212151F), // Obsidian Cinema Deep Glass
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 6.dp,
      ) {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Album Art
            AlbumArt(
              songUri = state.songUri,
              albumId = state.albumId,
              sizePx = 128,
              modifier = Modifier.size(46.dp),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = state.title.ifBlank { "Playing audio" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = if (state.isLive) "● LIVE RADIO" else "${state.artist} • ${state.album}",
                fontSize = 12.sp,
                color = if (state.isLive) Color(0xFFFF5252) else Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            // Play / Pause Button
            IconButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                playerManager.togglePlayPause()
              },
              modifier = Modifier.size(40.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(Color(0xFF00F2FE).copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = if (state.isPlaying) "Pause" else "Play",
                  tint = Color(0xFF00F2FE),
                  modifier = Modifier.size(22.dp),
                )
              }
            }

            // Next Track Button
            if (!state.isLive) {
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  playerManager.next()
                },
                modifier = Modifier.size(40.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.SkipNext,
                  contentDescription = "Next Track",
                  tint = Color.White.copy(alpha = 0.85f),
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }

          // Live glowing timeline bar
          if (!state.isLive && state.durationMs > 0L) {
            LinearProgressIndicator(
              progress = { progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp),
              color = Color(0xFF00F2FE),
              trackColor = Color.White.copy(alpha = 0.08f),
            )
          }
        }
      }
    }
  }
}
