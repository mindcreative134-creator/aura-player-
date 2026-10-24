package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.cmstudio.apnaplayer.engine.PlayerBridge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import com.cmstudio.apnaplayer.engine.ExoPlayerHolder
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import org.koin.compose.koinInject

@Composable
fun EqualizerSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sessionId = ExoPlayerHolder.audioSessionId
  var equalizer by remember { mutableStateOf<Equalizer?>(null) }
  var bassBoost by remember { mutableStateOf<BassBoost?>(null) }
  var loudnessEnhancer by remember { mutableStateOf<LoudnessEnhancer?>(null) }

  val playerPreferences = koinInject<PlayerPreferences>()
  val isDialogueEnhancerEnabled by playerPreferences.dialogueEnhancer.collectAsState()
  var isEnabled by remember { mutableStateOf(false) }
  var selectedPreset by remember { mutableIntStateOf(-1) }
  var bassStrength by remember { mutableIntStateOf(0) }
  var volumeBoostPercent by remember { mutableIntStateOf(0) }
  val bandLevels = remember { mutableStateListOf<Int>() }
  val bandFrequencies = remember { mutableStateListOf<Int>() }
  var minLevel by remember { mutableIntStateOf(-1500) }
  var maxLevel by remember { mutableIntStateOf(1500) }

  val presets = remember {
    listOf(
      "Normal", "Bass Boost", "Vocal", "Rock", "Pop", "Classical", "Custom"
    )
  }

  DisposableEffect(sessionId) {
    if (sessionId > 0) {
      runCatching {
        val eq = Equalizer(0, sessionId).apply { enabled = true }
        val bb = BassBoost(0, sessionId).apply { enabled = true }
        val le = runCatching { LoudnessEnhancer(sessionId).apply { enabled = true } }.getOrNull()
        equalizer = eq
        bassBoost = bb
        loudnessEnhancer = le
        isEnabled = eq.enabled

        val numBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange
        if (range.size >= 2) {
          minLevel = range[0].toInt()
          maxLevel = range[1].toInt()
        }

        bandLevels.clear()
        bandFrequencies.clear()
        for (i in 0 until numBands) {
          bandLevels.add(eq.getBandLevel(i.toShort()).toInt())
          bandFrequencies.add(eq.getCenterFreq(i.toShort()) / 1000)
        }
        bassStrength = bb.roundedStrength.toInt()
      }
    }

    onDispose {
      runCatching {
        equalizer?.release()
        bassBoost?.release()
        loudnessEnhancer?.release()
      }
    }
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Audio Equalizer",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = if (sessionId > 0) "Hardware Audio Processing" else "Audio session waiting",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
          )
        }
        Switch(
          checked = isEnabled,
          onCheckedChange = { checked ->
            isEnabled = checked
            equalizer?.enabled = checked
            bassBoost?.enabled = checked
          },
        )
      }

      if (isEnabled && bandLevels.isNotEmpty()) {
        Text(
          text = "Sound Presets",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(presets.indices.toList()) { index ->
            FilterChip(
              selected = selectedPreset == index,
              onClick = {
                selectedPreset = index
                when (index) {
                  0 -> { // Normal
                    bandLevels.indices.forEach { i ->
                      bandLevels[i] = 0
                      equalizer?.setBandLevel(i.toShort(), 0)
                    }
                    bassStrength = 0
                    bassBoost?.setStrength(0)
                  }
                  1 -> { // Bass Boost
                    bandLevels.indices.forEach { i ->
                      val lvl = if (i == 0) (maxLevel * 0.7f).toInt() else if (i == 1) (maxLevel * 0.4f).toInt() else 0
                      bandLevels[i] = lvl
                      equalizer?.setBandLevel(i.toShort(), lvl.toShort())
                    }
                    bassStrength = 800
                    bassBoost?.setStrength(800)
                  }
                  2 -> { // Vocal
                    bandLevels.indices.forEach { i ->
                      val lvl = if (i in 1..3) (maxLevel * 0.5f).toInt() else 0
                      bandLevels[i] = lvl
                      equalizer?.setBandLevel(i.toShort(), lvl.toShort())
                    }
                  }
                  3 -> { // Rock
                    bandLevels.indices.forEach { i ->
                      val lvl = if (i == 0 || i == bandLevels.lastIndex) (maxLevel * 0.6f).toInt() else 0
                      bandLevels[i] = lvl
                      equalizer?.setBandLevel(i.toShort(), lvl.toShort())
                    }
                  }
                  4 -> { // Pop
                    bandLevels.indices.forEach { i ->
                      val lvl = if (i in 1..3) (maxLevel * 0.3f).toInt() else 0
                      bandLevels[i] = lvl
                      equalizer?.setBandLevel(i.toShort(), lvl.toShort())
                    }
                  }
                  else -> {}
                }
              },
              label = { Text(presets[index]) },
            )
          }
        }

        // Bass Boost
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = "Bass Boost", style = MaterialTheme.typography.titleSmall)
            Text(text = "${(bassStrength / 10)}%", style = MaterialTheme.typography.bodySmall)
          }
          Slider(
            value = bassStrength.toFloat(),
            onValueChange = { value ->
              bassStrength = value.toInt()
              bassBoost?.setStrength(value.toInt().toShort())
              selectedPreset = 6 // Custom
            },
            valueRange = 0f..1000f,
          )
        }

        // Hardware Volume Boost (up to 200%)
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(text = "Volume Boost (Up to 200%)", style = MaterialTheme.typography.titleSmall)
            Text(
              text = if (volumeBoostPercent > 0) "+${volumeBoostPercent}% (+${String.format(java.util.Locale.US, "%.1f", volumeBoostPercent * 0.2f)} dB)" else "Off (100%)",
              style = MaterialTheme.typography.bodySmall,
              color = if (volumeBoostPercent > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
          }
          Slider(
            value = volumeBoostPercent.toFloat(),
            onValueChange = { value ->
              volumeBoostPercent = value.toInt()
              val gainMB = volumeBoostPercent * 20
              runCatching { loudnessEnhancer?.setTargetGain(gainMB) }
              runCatching {
                if (volumeBoostPercent > 0) {
                  PlayerBridge.setPropertyInt("volume-max", 200)
                  PlayerBridge.setPropertyInt("volume", 100 + volumeBoostPercent)
                } else {
                  PlayerBridge.setPropertyInt("volume-max", 100)
                }
              }
            },
            valueRange = 0f..100f,
          )
        }

        // Dialogue Enhancer & Night Mode
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = "Dialogue Enhancer (Night Mode)", style = MaterialTheme.typography.titleSmall)
            Text(
              text = "Tames loud explosions & clarifies quiet speech",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
          }
          Switch(
            checked = isDialogueEnhancerEnabled,
            onCheckedChange = { checked ->
              playerPreferences.dialogueEnhancer.set(checked)
              runCatching {
                if (checked) {
                  PlayerBridge.setPropertyString("af", "loudnorm=I=-16:TP=-1.5:LRA=11")
                } else {
                  PlayerBridge.setPropertyString("af", "")
                }
              }
            },
          )
        }

        // Graphic Equalizer Bands
        Text(
          text = "Equalizer Bands",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )

        bandLevels.indices.forEach { index ->
          val freq = if (index < bandFrequencies.size) "${bandFrequencies[index]} Hz" else "Band ${index + 1}"
          val level = bandLevels[index]
          val db = String.format("%.1f dB", level / 100f)

          Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(text = freq, style = MaterialTheme.typography.bodyMedium)
              Text(text = db, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
              value = level.toFloat(),
              onValueChange = { value ->
                bandLevels[index] = value.toInt()
                equalizer?.setBandLevel(index.toShort(), value.toInt().toShort())
                selectedPreset = 6 // Custom
              },
              valueRange = minLevel.toFloat()..maxLevel.toFloat(),
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onDismissRequest) {
          Text("Close")
        }
      }
    }
  }
}
