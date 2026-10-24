package app.marlboroadvance.mpvex.ui.browser.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.marlboroadvance.mpvex.domain.lyrics.LrcLibLyricsRepository
import app.marlboroadvance.mpvex.domain.lyrics.LyricsState
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens

/**
 * Aura Now Playing Sheet
 * High-fidelity, ambient music listening experience with synced lyrics, waveform audio meter,
 * and tactile controls.
 */
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    playerManager: ApnaAudioPlayerManager,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val haptic = LocalHapticFeedback.current
    val state by playerManager.state.collectAsState()
    val position by playerManager.position.collectAsState()
    val level by playerManager.level.collectAsState()

    val lyricsRepo = remember { LrcLibLyricsRepository() }
    var lyricsState by remember { mutableStateOf<LyricsState>(LyricsState.Idle) }
    var showLyrics by remember { mutableStateOf(false) }

    LaunchedEffect(state.mediaId) {
        if (!state.hasMedia || state.isLive || state.title.isBlank()) {
            lyricsState = LyricsState.Idle
        } else {
            lyricsState = LyricsState.Loading
            lyricsState = lyricsRepo.fetch(state.title, state.artist, state.album, state.durationMs)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = AuraTokens.ShapeSheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        },
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Dismiss and Mode Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (state.isLive) "LIVE BROADCAST" else "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = if (state.isLive) Color(0xFFFF5252) else AuraTokens.AuraCyan
                )

                if (!state.isLive) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showLyrics = !showLyrics
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) AuraTokens.AuraCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Album Art or Synced Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                contentAlignment = Alignment.Center
            ) {
                if (showLyrics && !state.isLive) {
                    LyricsView(lyricsState, position, onSeek = { playerManager.seekTo(it) })
                } else {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .shadow(24.dp, AuraTokens.ShapeCardLarge, spotColor = AuraTokens.AuraCyan.copy(alpha = 0.35f))
                            .clip(AuraTokens.ShapeCardLarge)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AuraTokens.AuraCyan.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.15f)
                                    )
                                ),
                                shape = AuraTokens.ShapeCardLarge
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArt(
                            songUri = state.songUri,
                            albumId = state.albumId,
                            sizePx = 640,
                            modifier = Modifier.size(260.dp),
                            shape = AuraTokens.ShapeCardLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Track Title & Artist
            Text(
                text = state.title.ifBlank { "No audio playing" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = state.error ?: (if (state.artist.isNotBlank()) "${state.artist} • ${state.album}" else state.album),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            // Waveform HUD
            WaveformHud(
                level = level,
                playing = state.isPlaying,
                color = AuraTokens.AuraCyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            )

            Spacer(Modifier.height(10.dp))

            // Seekbar
            if (state.isLive) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)))
                    Text("LIVE STREAM", color = Color(0xFFFF5252), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            } else {
                var dragging by remember { mutableStateOf(false) }
                var dragValue by remember { mutableFloatStateOf(0f) }
                val max = state.durationMs.coerceAtLeast(1L).toFloat()

                Slider(
                    value = (if (dragging) dragValue else position.toFloat()).coerceIn(0f, max),
                    onValueChange = { dragging = true; dragValue = it },
                    onValueChangeFinished = {
                        playerManager.seekTo(dragValue.toLong())
                        dragging = false
                    },
                    valueRange = 0f..max,
                    colors = SliderDefaults.colors(
                        thumbColor = AuraTokens.AuraCyan,
                        activeTrackColor = AuraTokens.AuraCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(if (dragging) dragValue.toLong() else position),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        formatTime(state.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Controls Row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerManager.toggleShuffle()
                    },
                    enabled = !state.isLive
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffle) AuraTokens.AuraCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Previous
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerManager.previous()
                    },
                    enabled = !state.isLive
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Central Hero Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AuraTokens.AuraCyan)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playerManager.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isBuffering && !state.isPlaying) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerManager.next()
                    },
                    enabled = !state.isLive
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerManager.cycleRepeatMode()
                    },
                    enabled = !state.isLive
                ) {
                    Icon(
                        imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) AuraTokens.AuraCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun NowPlayingSheet(vm: MusicViewModel, onDismiss: () -> Unit) {
    NowPlayingSheet(
        isOpen = true,
        onDismissRequest = onDismiss,
        playerManager = vm.player
    )
}
