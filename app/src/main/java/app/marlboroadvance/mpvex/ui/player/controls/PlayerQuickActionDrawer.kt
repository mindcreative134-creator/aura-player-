package app.marlboroadvance.mpvex.ui.player.controls

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.Decoder.Companion.getDecoderFromValue
import app.marlboroadvance.mpvex.ui.player.Panels
import app.marlboroadvance.mpvex.ui.player.PlayerActivity
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import app.marlboroadvance.mpvex.ui.player.Sheets
import com.cmstudio.apnaplayer.engine.PlayerBridge
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tactical Right-Edge Drawer Trigger Handle (`<` chevron).
 * Positioned on the right edge of the video screen.
 * Tapping or pulling this handle opens the Quick Action "More" Drawer with spring animation.
 */
@Composable
fun PlayerEdgeDrawerHandle(
  visible: Boolean,
  onOpen: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val pullThresholdPx = with(density) { 48.dp.toPx() }
  var pullDistancePx by remember { mutableFloatStateOf(0f) }

  val animatedPull by animateFloatAsState(
    targetValue = pullDistancePx,
    animationSpec = spring(dampingRatio = 0.85f, stiffness = 800f),
    label = "PlayerDrawerHandlePull",
  )

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(180)) + slideInHorizontally(tween(200)) { it },
    exit = fadeOut(tween(140)) + slideOutHorizontally(tween(160)) { it },
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .width(36.dp)
        .height(72.dp)
        .graphicsLayer {
          translationX = -animatedPull * 0.65f
        }
        .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
        .background(Color(0xDD141620))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpen()
          },
        )
        .pointerInput(Unit) {
          detectHorizontalDragGestures(
            onDragStart = { pullDistancePx = 0f },
            onDragCancel = { pullDistancePx = 0f },
            onDragEnd = {
              if (pullDistancePx >= pullThresholdPx) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onOpen()
              }
              pullDistancePx = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
              val directedDelta = -dragAmount
              if (directedDelta > 0f || pullDistancePx > 0f) {
                change.consume()
                pullDistancePx = (pullDistancePx + directedDelta).coerceIn(0f, pullThresholdPx * 1.5f)
              }
            },
          )
        },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = "<",
        color = Color.White.copy(alpha = 0.90f),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = Modifier.padding(end = 4.dp),
      )
    }
  }
}

/**
 * Premium Right-Side Sliding Panel Drawer for "More" Quick Controls.
 * Built with Obsidian Cinema & Glassmorphic visual discipline from StitchMCP.
 * Displays a 3-column grid of rich action tiles with micro-glow indicators.
 */
@Composable
fun PlayerQuickActionDrawer(
  isOpen: Boolean,
  onDismissRequest: () -> Unit,
  viewModel: PlayerViewModel,
  onOpenSheet: (Sheets) -> Unit,
  onOpenPanel: (Panels) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = isOpen, onBack = onDismissRequest)

  val configuration = LocalConfiguration.current
  val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
  val playerPreferences = koinInject<PlayerPreferences>()
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val activity = context as? PlayerActivity

  val isAmbientGlow by playerPreferences.isAmbientGlowEnabled.collectAsState()
  val isDialogueEnhancer by playerPreferences.dialogueEnhancer.collectAsState()
  val areControlsLocked by viewModel.areControlsLocked.collectAsState()
  val isABLoopExpanded by viewModel.isABLoopExpanded.collectAsState()
  val mpvDecoder by PlayerBridge.propString["hwdec-current"].collectAsState()
  val decoder by remember(mpvDecoder) {
    derivedStateOf {
      val decValue = mpvDecoder?.takeIf { it.isNotBlank() } ?: "mediacodec"
      getDecoderFromValue(decValue)
    }
  }

  AnimatedVisibility(
    visible = isOpen,
    enter = fadeIn(tween(180)),
    exit = fadeOut(tween(160)),
    modifier = modifier.fillMaxSize(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.50f))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest,
        ),
      contentAlignment = if (isPortrait) Alignment.BottomCenter else Alignment.CenterEnd,
    ) {
      Surface(
        modifier = Modifier
          .then(
            if (isPortrait) {
              Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
            } else {
              Modifier
                .width(380.dp)
                .fillMaxHeight()
            }
          )
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}, // Catch clicks inside panel
          ),
        shape = if (isPortrait) {
          RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        } else {
          RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
        },
        color = Color(0xFA11131B), // Obsidian Cinema Deep Glass
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
          // Drawer Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 8.dp, end = 2.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(Color(0xFF00F2FE), CircleShape),
              )
              Text(
                text = stringResource(R.string.player_sheets_more_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }

            IconButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDismissRequest()
              },
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White.copy(alpha = 0.85f),
              )
            }
          }

          // 3-Column Quick Actions Grid
          LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // 1. Video Color Looks (12 Cinema Presets)
            item {
              QuickActionCard(
                title = "Color Looks",
                icon = Icons.Default.AutoAwesome,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.FilterPresets)
                },
              )
            }

            // 2. Equalizer & Volume Boost
            item {
              QuickActionCard(
                title = "Equalizer",
                icon = Icons.Default.Equalizer,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.Equalizer)
                },
              )
            }

            // 3. Sleep Timer
            item {
              QuickActionCard(
                title = "Sleep Timer",
                icon = Icons.Outlined.Timer,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.More)
                },
              )
            }

            // 4. Cinema Ambient Glow
            item {
              QuickActionCard(
                title = if (isAmbientGlow) "Ambilight: On" else "Ambilight",
                icon = Icons.Default.LightMode,
                isActive = isAmbientGlow,
                onClick = {
                  playerPreferences.isAmbientGlowEnabled.set(!isAmbientGlow)
                },
              )
            }

            // 5. Night Mode / Dialogue Boost
            item {
              QuickActionCard(
                title = if (isDialogueEnhancer) "Night Mode: On" else "Night Mode",
                icon = Icons.Default.GraphicEq,
                isActive = isDialogueEnhancer,
                onClick = {
                  val newState = !isDialogueEnhancer
                  playerPreferences.dialogueEnhancer.set(newState)
                  runCatching {
                    if (newState) {
                      PlayerBridge.setPropertyString("af", "loudnorm=I=-16:TP=-1.5:LRA=11")
                    } else {
                      PlayerBridge.setPropertyString("af", "")
                    }
                  }
                },
              )
            }

            // 6. High-Res Screenshot
            item {
              QuickActionCard(
                title = "Screenshot",
                icon = Icons.Default.CameraAlt,
                onClick = {
                  scope.launch {
                    val picturesDir = File(
                      android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                      ),
                      "ApnaPlayerSnaps"
                    ).apply { mkdirs() }
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val outFile = File(picturesDir, "Snap_$timestamp.jpg")
                    PlayerBridge.command("screenshot-to-file", outFile.absolutePath)
                    android.widget.Toast.makeText(context, "Saved: ${outFile.name}", android.widget.Toast.LENGTH_SHORT).show()
                  }
                },
              )
            }

            // 7. Frame Navigation / Dial
            item {
              QuickActionCard(
                title = "Frame Step",
                icon = Icons.Default.MotionPhotosAuto,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.FrameNavigation)
                },
              )
            }

            // 8. Anime4K Upscaler
            item {
              QuickActionCard(
                title = "Anime4K",
                icon = Icons.Default.Tune,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.More)
                },
              )
            }

            // 9. Playback Diagnostics & Stats HUD
            item {
              QuickActionCard(
                title = "Diagnostics",
                icon = Icons.Default.Memory,
                onClick = {
                  onDismissRequest()
                  onOpenSheet(Sheets.Diagnostics)
                },
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Individual rounded card for the 3-column quick action grid.
 */
@Composable
private fun QuickActionCard(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isActive: Boolean = false,
) {
  val haptic = LocalHapticFeedback.current
  val containerColor by animateColorAsState(
    targetValue = if (isActive) Color(0xFF00557A) else Color(0x33252836),
    animationSpec = tween(180),
    label = "ActionCardBg",
  )
  val borderColor by animateColorAsState(
    targetValue = if (isActive) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.08f),
    animationSpec = tween(180),
    label = "ActionCardBorder",
  )
  val iconTint by animateColorAsState(
    targetValue = if (isActive) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.90f),
    animationSpec = tween(180),
    label = "ActionCardIconTint",
  )

  Surface(
    modifier = modifier
      .height(98.dp)
      .fillMaxWidth()
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onClick()
        },
      ),
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    tonalElevation = if (isActive) 3.dp else 0.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconTint,
          modifier = Modifier.size(26.dp),
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = if (isActive) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.85f),
        lineHeight = 13.sp,
      )
    }
  }
}
