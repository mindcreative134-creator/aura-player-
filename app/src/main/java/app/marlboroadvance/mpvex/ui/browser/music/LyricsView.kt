package app.marlboroadvance.mpvex.ui.browser.music

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.lyrics.LrcParser
import app.marlboroadvance.mpvex.domain.lyrics.LyricsState

/**
 * Synced lyrics: active line glow, auto-scroll, tap on line to seek.
 */
@Composable
fun LyricsView(
    state: LyricsState,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            LyricsState.Idle -> Unit
            LyricsState.Loading -> CircularProgressIndicator()
            LyricsState.NotFound -> Text(
                "Is gaane ke lyrics nahi mile",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is LyricsState.Plain -> Text(
                state.text,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            is LyricsState.Synced -> SyncedLyrics(state, positionMs, onSeek)
        }
    }
}

@Composable
private fun SyncedLyrics(state: LyricsState.Synced, positionMs: Long, onSeek: (Long) -> Unit) {
    val lines = state.lines
    val activeIndex by remember(lines) {
        derivedStateOf { LrcParser.activeIndex(lines, positionMs) }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            val offset = -(listState.layoutInfo.viewportSize.height / 3)
            listState.animateScrollToItem(activeIndex, offset)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 120.dp)
    ) {
        itemsIndexed(lines, key = { i, l -> "$i-${l.timeMs}" }) { index, line ->
            val active = index == activeIndex
            val glow = MaterialTheme.colorScheme.primary
            val color by animateColorAsState(
                if (active) glow else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                label = "lyricColor"
            )
            val scale by animateFloatAsState(if (active) 1.08f else 1f, label = "lyricScale")
            Text(
                text = line.text.ifBlank { "♪" },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeek(line.timeMs) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .scale(scale),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = color,
                    fontSize = 20.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    shadow = if (active) Shadow(glow.copy(alpha = 0.8f), Offset.Zero, 24f) else null
                )
            )
        }
    }
}
