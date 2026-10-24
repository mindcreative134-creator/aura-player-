package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.media.MediaScannerConnection
import android.os.Environment
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Timer
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.anime4k.Anime4KManager
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import com.cmstudio.apnaplayer.engine.ExoPlayerHolder
import com.cmstudio.apnaplayer.engine.PlayerBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MoreSheet(
  remainingTime: Int,
  onStartTimer: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  onEnterFiltersPanel: () -> Unit,
  onOpenEqualizer: (() -> Unit)? = null,
  onOpenFrameNavigation: (() -> Unit)? = null,
  onOpenAudioDelay: (() -> Unit)? = null,
  onOpenSubtitleDelay: (() -> Unit)? = null,
  onOpenSubtitleSettings: (() -> Unit)? = null,
  onOpenOnlineSubtitles: (() -> Unit)? = null,
  onOpenABLoop: (() -> Unit)? = null,
  onOpenDiagnostics: (() -> Unit)? = null,
  onAnime4KChanged: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val playerPreferences = koinInject<PlayerPreferences>()
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val decoderPreferences = koinInject<DecoderPreferences>()
  val anime4kManager = koinInject<Anime4KManager>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  val isAmbientGlow by playerPreferences.isAmbientGlowEnabled.collectAsState()
  val isDialogueEnhancer by playerPreferences.dialogueEnhancer.collectAsState()
  
  val enableAnime4K by decoderPreferences.enableAnime4K.collectAsState()
  val anime4kMode by decoderPreferences.anime4kMode.collectAsState()
  val anime4kQuality by decoderPreferences.anime4kQuality.collectAsState()
  val gpuNext by decoderPreferences.gpuNext.collectAsState()
  val useVulkan by decoderPreferences.useVulkan.collectAsState()
  
  val context = LocalContext.current
val scope = rememberCoroutineScope()

  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(MaterialTheme.spacing.medium)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(id = R.string.player_sheets_more_title),
          style = MaterialTheme.typography.headlineMedium,
        )
      }

      var isSleepTimerDialogShown by remember { mutableStateOf(false) }

      Text(
        text = "Quick Controls",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Sleep Timer
        AssistChip(
          onClick = { isSleepTimerDialogShown = true },
          label = {
            Text(
              if (remainingTime == 0) {
                stringResource(R.string.timer_title)
              } else {
                DateUtils.formatElapsedTime(remainingTime.toLong())
              }
            )
          },
          leadingIcon = {
            Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          colors = if (remainingTime > 0) {
            AssistChipDefaults.assistChipColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          } else {
            AssistChipDefaults.assistChipColors()
          }
        )

        // Equalizer
        if (onOpenEqualizer != null) {
          AssistChip(
            onClick = onOpenEqualizer,
            label = { Text("Equalizer", maxLines = 1, softWrap = false) },
            leadingIcon = {
              Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp))
            }
          )
        }

        // Video Filters
        AssistChip(
          onClick = onEnterFiltersPanel,
          label = { Text(stringResource(id = R.string.player_sheets_filters_title), maxLines = 1, softWrap = false) },
          leadingIcon = {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        )

        // Screenshot
        AssistChip(
          onClick = {
            scope.launch(Dispatchers.IO) {
              val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
              val snapDir = File(picturesDir, "ApnaPlayerSnaps").apply { mkdirs() }
              val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
              val snapFile = File(snapDir, "SNAP_${timestamp}.png")
              val success = ExoPlayerHolder.captureFrame(snapFile)
              withContext(Dispatchers.Main) {
                if (success && snapFile.exists() && snapFile.length() > 0) {
                  MediaScannerConnection.scanFile(context, arrayOf(snapFile.absolutePath), arrayOf("image/png"), null)
                  Toast.makeText(context, "Screenshot saved to Pictures/ApnaPlayerSnaps", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Could not capture screenshot", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          label = { Text("Screenshot", maxLines = 1, softWrap = false) },
          leadingIcon = {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
          }
        )

        // Cinema Ambient Glow
        AssistChip(
          onClick = {
            playerPreferences.isAmbientGlowEnabled.set(!isAmbientGlow)
          },
          label = { Text(if (isAmbientGlow) "Ambient: On" else "Ambient Glow", maxLines = 1, softWrap = false) },
          leadingIcon = {
            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          colors = if (isAmbientGlow) {
            AssistChipDefaults.assistChipColors(
              containerColor = MaterialTheme.colorScheme.tertiaryContainer,
              labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
              leadingIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          } else {
            AssistChipDefaults.assistChipColors()
          }
        )

        // Dialogue Enhancer / Night Mode
        AssistChip(
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
          label = { Text(if (isDialogueEnhancer) "Dialogue: On" else "Night Mode", maxLines = 1, softWrap = false) },
          leadingIcon = {
            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          colors = if (isDialogueEnhancer) {
            AssistChipDefaults.assistChipColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
              leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          } else {
            AssistChipDefaults.assistChipColors()
          }
        )
      }

      if (isSleepTimerDialogShown) {
        TimePickerDialog(
          remainingTime = remainingTime,
          onDismissRequest = { isSleepTimerDialogShown = false },
          onTimeSelect = onStartTimer,
        )
      }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .then(if (onOpenDiagnostics != null) Modifier.clickable { onOpenDiagnostics() } else Modifier),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.small),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "⚡ Apna Player Native Engine (Google Media3 / HW+)",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            if (onOpenDiagnostics != null) {
              Icon(
                Icons.Default.Info,
                contentDescription = "Open Diagnostics",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          val (vWidth, vHeight) = ExoPlayerHolder.videoSize
          val resolutionStr = if (vWidth > 0 && vHeight > 0) "${vWidth}×${vHeight}" else "Active Stream"
          Text(
            text = "Output: $resolutionStr · ${ExoPlayerHolder.videoFormatInfo}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "Audio: ${ExoPlayerHolder.audioFormatInfo} · Hardware Session #${ExoPlayerHolder.audioSessionId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Text(
        text = stringResource(R.string.player_sheets_stats_page_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        items(8) { page ->
          FilterChip(
            label = {
              Text(
                text = when (page) {
                  0 -> stringResource(R.string.player_sheets_tracks_off)
                  7 -> "Console"
                  else -> "Page $page"
                },
                maxLines = 1,
                softWrap = false,
              )
            },
            onClick = {
              if ((page == 0) xor (statisticsPage == 0)) PlayerBridge.command("script-binding", "stats/display-stats-toggle")
              if (page in 1..6) PlayerBridge.command("script-binding", "stats/display-page-$page")
              if (page == 7) PlayerBridge.command("script-binding", "stats/display-stats-toggle")
              advancedPreferences.enabledStatisticsPage.set(page)
            },
            selected = statisticsPage == page,
            leadingIcon = null,
          )
        }
      }
      
      // Shaders Controls
      if (enableAnime4K && (!gpuNext || useVulkan)) {
        // Auto-detect resolution to disable for 4K+
        val width = PlayerBridge.getPropertyInt("video-params/w") ?: 0
        val height = PlayerBridge.getPropertyInt("video-params/h") ?: 0
        val isHighRes = width >= 3840 || height >= 2160

        // Presets (Mode) - Now on Top
        Text(
            text = stringResource(R.string.anime4k_mode_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (isHighRes) {
            Text(
                text = "Not available for 4K/8K video",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          items(Anime4KManager.Mode.entries) { mode ->
            FilterChip(
              label = { Text(stringResource(mode.titleRes)) },
              selected = anime4kMode == mode.name,
              enabled = !isHighRes,
              leadingIcon = null,
              onClick = {
                decoderPreferences.anime4kMode.set(mode.name)
                
                // Apply shaders immediately (runtime change)
                scope.launch(Dispatchers.IO) {
                  runCatching {
                    val qualityStr = decoderPreferences.anime4kQuality.get()
                    val quality = try {
                      Anime4KManager.Quality.valueOf(qualityStr)
                    } catch (e: IllegalArgumentException) {
                      Anime4KManager.Quality.BALANCED
                    }
                    val currentMode = try {
                        Anime4KManager.Mode.valueOf(mode.name)
                    } catch (e: IllegalArgumentException) {
                        Anime4KManager.Mode.OFF
                    }

                    val shaderChain = anime4kManager.getShaderChain(currentMode, quality)

                    // Use setPropertyString for runtime changes
                    PlayerBridge.setPropertyString("glsl-shaders", if (shaderChain.isNotEmpty()) shaderChain else "")
                    onAnime4KChanged()
                  }
                }
              }
            )
          }
        }

        Text(
            text = stringResource(R.string.anime4k_quality_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          items(Anime4KManager.Quality.entries) { quality ->
             FilterChip(
              label = { Text(stringResource(quality.titleRes)) },
              selected = anime4kQuality == quality.name,
              enabled = anime4kMode != "OFF" && !isHighRes,
              leadingIcon = null,
              onClick = {
                decoderPreferences.anime4kQuality.set(quality.name)

                // Apply shaders immediately (runtime change)
                scope.launch(Dispatchers.IO) {
                  runCatching {
                    val modeStr = decoderPreferences.anime4kMode.get()
                    val modeEnum = try {
                      Anime4KManager.Mode.valueOf(modeStr)
                    } catch (e: IllegalArgumentException) {
                      Anime4KManager.Mode.OFF
                    }
                    val currentQuality = try {
                        Anime4KManager.Quality.valueOf(quality.name)
                    } catch (e: IllegalArgumentException) {
                        Anime4KManager.Quality.BALANCED
                    }

                    val shaderChain = anime4kManager.getShaderChain(modeEnum, currentQuality)

                    // Use setPropertyString for runtime changes
                    PlayerBridge.setPropertyString("glsl-shaders", if (shaderChain.isNotEmpty()) shaderChain else "")
                    onAnime4KChanged()
                  }
                }
              }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimePickerDialog(
  onDismissRequest: () -> Unit,
  onTimeSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  remainingTime: Int = 0,
) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      modifier = modifier
          .width(360.dp) // Fixed wide width to fit presets
          .padding(MaterialTheme.spacing.medium),
    ) {
      Column(
        modifier =
          Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
              text = stringResource(R.string.timer_title), // "Sleep Timer"
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.timer_picker_enter_timer),
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
        }

        val state =
          rememberTimePickerState(
            remainingTime / 3600,
            (remainingTime % 3600) / 60,
            is24Hour = true,
          )

        TimeInput(state = state)
        
        // Quick Presets
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Quick Presets",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val presets = listOf(15, 30, 45, 60)
                presets.forEach { minutes ->
                    FilterChip(
                        selected = false,
                        onClick = { 
                            onTimeSelect(minutes * 60)
                            onDismissRequest()
                        },
                        label = { Text("${minutes}m") },
                        leadingIcon = null,
                    )
                }
            }
        }

        // Actions
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          TextButton(onClick = {
             onTimeSelect(0)
             onDismissRequest()
          }) {
              Text(stringResource(id = R.string.generic_reset))
          }
          Spacer(Modifier.weight(1f))
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TextButton(onClick = onDismissRequest) {
              Text(stringResource(id = R.string.generic_cancel))
            }
            Button(
              onClick = {
                onTimeSelect(state.hour * 3600 + state.minute * 60)
                onDismissRequest()
              },
            ) {
              Text(stringResource(id = R.string.generic_ok))
            }
          }
        }
      }
    }
  }
  }




