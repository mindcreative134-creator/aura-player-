package app.marlboroadvance.mpvex.ui.browser.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.marlboroadvance.mpvex.domain.lyrics.LrcLibLyricsRepository
import app.marlboroadvance.mpvex.domain.lyrics.LyricsState
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager

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
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album art or Synced Lyrics
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                if (showLyrics && !state.isLive) {
                    LyricsView(lyricsState, position, onSeek = { playerManager.seekTo(it) })
                } else {
                    AlbumArt(
                        songUri = state.songUri,
                        albumId = state.albumId,
                        sizePx = 640,
                        modifier = Modifier.size(280.dp)
                    )
                }
            }

            Spacer16()
            Text(
                state.title.ifBlank { "Kuch nahi chal raha" },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                state.error ?: state.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer16()
            WaveformHud(
                level = level,
                playing = state.isPlaying,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )

            // Seek bar (live radio shows LIVE badge)
            if (state.isLive) {
                Spacer16()
                Text("● LIVE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
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
                    valueRange = 0f..max
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(if (dragging) dragValue.toLong() else position), style = MaterialTheme.typography.labelMedium)
                    Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer16()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = playerManager::toggleShuffle, enabled = !state.isLive) {
                    Icon(
                        Icons.Rounded.Shuffle, "Shuffle",
                        tint = if (state.shuffle) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = playerManager::previous, enabled = !state.isLive) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(36.dp))
                }
                FilledIconButton(onClick = playerManager::togglePlayPause, modifier = Modifier.size(64.dp)) {
                    if (state.isBuffering && !state.isPlaying) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (state.isPlaying) "Pause" else "Play",
                            Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(onClick = playerManager::next, enabled = !state.isLive) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(36.dp))
                }
                IconButton(onClick = playerManager::cycleRepeatMode, enabled = !state.isLive) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!state.isLive) {
                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        Icons.Rounded.Lyrics, "Lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun Spacer16() = Box(Modifier.height(16.dp))
