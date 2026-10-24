package app.marlboroadvance.mpvex.ui.browser.music

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.music.AlbumArtCache
import kotlinx.coroutines.delay

/** Local song album art display with cached thumbnail loader; fallback to music note icon. */
@Composable
fun AlbumArt(
    songUri: Uri?,
    albumId: Long,
    modifier: Modifier = Modifier,
    sizePx: Int = 256,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val context = LocalContext.current
    val cache = remember(context) { AlbumArtCache(context.applicationContext) }
    val bitmap by produceState<Bitmap?>(initialValue = null, songUri, albumId) {
        value = cache.load(songUri, albumId, sizePx)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Animated 3-bar equalizer to indicate active playback. */
@Composable
fun EqualizerBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(420, 560, 350)
    val fractions = durations.mapIndexed { i, d ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(d, easing = LinearEasing), RepeatMode.Reverse),
            label = "bar$i"
        )
    }
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        fractions.forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(height * (if (playing) f.value else 0.25f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/** Real-time scrolling waveform HUD derived from LevelAudioProcessor amplitude. */
@Composable
fun WaveformHud(
    level: Float,
    playing: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 40,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val history = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0f) } } }
    val latestLevel by rememberUpdatedState(level)
    val latestPlaying by rememberUpdatedState(playing)
    LaunchedEffect(Unit) {
        while (true) {
            history.removeAt(0)
            history.add(if (latestPlaying) latestLevel else 0f)
            delay(60)
        }
    }
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        history.forEachIndexed { i, v ->
            val h = (size.height * v.coerceIn(0f, 1f)).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                color = color.copy(alpha = 0.35f + 0.65f * (i / barCount.toFloat())),
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
