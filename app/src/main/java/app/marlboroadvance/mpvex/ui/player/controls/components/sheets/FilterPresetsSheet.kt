package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.FilterPreset
import com.cmstudio.apnaplayer.engine.PlayerBridge
import org.koin.compose.koinInject

@Composable
fun FilterPresetsSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val decoderPreferences = koinInject<DecoderPreferences>()

  val brightness by decoderPreferences.brightnessFilter.collectAsState()
  val saturation by decoderPreferences.saturationFilter.collectAsState()
  val contrast by decoderPreferences.contrastFilter.collectAsState()
  val gamma by decoderPreferences.gammaFilter.collectAsState()
  val hue by decoderPreferences.hueFilter.collectAsState()
  val sharpness by decoderPreferences.sharpnessFilter.collectAsState()

  val currentPreset = FilterPreset.entries.find { preset ->
    preset.brightness == brightness &&
      preset.saturation == saturation &&
      preset.contrast == contrast &&
      preset.gamma == gamma &&
      preset.hue == hue &&
      preset.sharpness == sharpness
  } ?: FilterPreset.NONE.takeIf {
    brightness == 0 && saturation == 0 && contrast == 0 && gamma == 0 && hue == 0 && sharpness == 0
  }

  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "12 Video Color Looks",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = "Tap any cinema grading preset to instantly enhance video colors",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
      )

      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.height(340.dp),
      ) {
        items(FilterPreset.entries) { preset ->
          val isSelected = currentPreset == preset
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                decoderPreferences.brightnessFilter.set(preset.brightness)
                decoderPreferences.saturationFilter.set(preset.saturation)
                decoderPreferences.contrastFilter.set(preset.contrast)
                decoderPreferences.gammaFilter.set(preset.gamma)
                decoderPreferences.hueFilter.set(preset.hue)
                decoderPreferences.sharpnessFilter.set(preset.sharpness)

                PlayerBridge.setPropertyInt("brightness", preset.brightness)
                PlayerBridge.setPropertyInt("saturation", preset.saturation)
                PlayerBridge.setPropertyInt("contrast", preset.contrast)
                PlayerBridge.setPropertyInt("gamma", preset.gamma)
                PlayerBridge.setPropertyInt("hue", preset.hue)
                PlayerBridge.setPropertyInt("sharpen", preset.sharpness)
              },
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = preset.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
