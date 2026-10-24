package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
) {
  val coercedValue = value.coerceIn(range)
  Box(
    modifier =
      modifier
        .height(120.dp)
        .aspectRatio(0.22f)
        .clip(RoundedCornerShape(16.dp))
        .background(AuraTokens.ObsidianSurface)
        .border(
          width = 1.dp,
          color = AuraTokens.CarbonBorder,
          shape = RoundedCornerShape(16.dp),
        ),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(coercedValue, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .clip(RoundedCornerShape(14.dp))
        .background(AuraTokens.AuraCyan),
    )
    if (overflowRange != null && overflowValue != null) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue, overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .clip(RoundedCornerShape(14.dp))
          .background(AuraTokens.AuraSunset),
      )
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
) {
  val coercedValue = value.coerceIn(range)
  Box(
    modifier =
      modifier
        .height(120.dp)
        .aspectRatio(0.22f)
        .clip(RoundedCornerShape(16.dp))
        .background(AuraTokens.ObsidianSurface)
        .border(
          width = 1.dp,
          color = AuraTokens.CarbonBorder,
          shape = RoundedCornerShape(16.dp),
        ),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(percentage(coercedValue, range), label = "vsliderheight")
    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .clip(RoundedCornerShape(14.dp))
        .background(AuraTokens.AuraCyan),
    )
    if (overflowRange != null && overflowValue != null) {
      val overflowHeight by animateFloatAsState(
        percentage(overflowValue, overflowRange),
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .clip(RoundedCornerShape(14.dp))
          .background(AuraTokens.AuraSunset),
      )
    }
  }
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
) {
  val coercedBrightness = brightness.coerceIn(range)
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    color = AuraTokens.CarbonElevated.copy(alpha = 0.88f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, AuraTokens.CarbonBorder),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Text(
        (coercedBrightness * 100).toInt().toString(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = AuraTokens.AuraCyan,
      )
      VerticalSlider(
        coercedBrightness,
        range,
      )
      Icon(
        when (percentage(coercedBrightness, range)) {
          in 0f..0.3f -> Icons.Default.BrightnessLow
          in 0.3f..0.6f -> Icons.Default.BrightnessMedium
          in 0.6f..1f -> Icons.Default.BrightnessHigh
          else -> Icons.Default.BrightnessMedium
        },
        contentDescription = null,
        tint = AuraTokens.AuraCyan,
      )
    }
  }
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
) {
  val percentage = (percentage(volume, range) * 100).roundToInt()
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(22.dp),
    color = AuraTokens.CarbonElevated.copy(alpha = 0.88f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = BorderStroke(1.dp, AuraTokens.CarbonBorder),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      val boostVolume = mpvVolume - 100
      Text(
        getVolumeSliderText(volume, mpvVolume, boostVolume, percentage, displayAsPercentage),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
        color = if (boostVolume > 0) AuraTokens.AuraSunset else AuraTokens.AuraCyan,
      )
      VerticalSlider(
        if (displayAsPercentage) percentage else volume,
        if (displayAsPercentage) 0..100 else range,
        overflowValue = boostVolume,
        overflowRange = boostRange,
      )
      Icon(
        when (percentage) {
          0 -> Icons.AutoMirrored.Default.VolumeOff
          in 0..30 -> Icons.AutoMirrored.Default.VolumeMute
          in 30..60 -> Icons.AutoMirrored.Default.VolumeDown
          in 60..100 -> Icons.AutoMirrored.Default.VolumeUp
          else -> Icons.AutoMirrored.Default.VolumeOff
        },
        contentDescription = null,
        tint = if (boostVolume > 0) AuraTokens.AuraSunset else AuraTokens.AuraCyan,
      )
    }
  }
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume == 100 ->
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }

      mpvVolume > 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      mpvVolume < 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      else -> {
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }
      }
    }
  }
