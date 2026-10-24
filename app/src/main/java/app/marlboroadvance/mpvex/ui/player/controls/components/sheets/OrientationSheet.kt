package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.PlayerOrientation
import org.koin.compose.koinInject

private data class OrientationOption(
  val mode: PlayerOrientation,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val activityInfoCode: Int,
)

@Composable
fun OrientationSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val activity = context as? Activity
  val playerPreferences = koinInject<PlayerPreferences>()
  val currentOrientation by playerPreferences.orientation.collectAsState()

  val options = listOf(
    OrientationOption(
      mode = PlayerOrientation.Video,
      title = "Video Aspect (Smart Auto)",
      subtitle = "Auto-detects aspect: landscape for widescreen, portrait for reels/shorts",
      icon = Icons.Default.VideoSettings,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
    ),
    OrientationOption(
      mode = PlayerOrientation.SensorLandscape,
      title = "Sensor Landscape",
      subtitle = "Auto-rotates between left & right landscape using gyroscope",
      icon = Icons.Default.StayCurrentLandscape,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
    ),
    OrientationOption(
      mode = PlayerOrientation.Landscape,
      title = "Landscape (Fixed)",
      subtitle = "Standard horizontal landscape mode",
      icon = Icons.Default.CropLandscape,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
    ),
    OrientationOption(
      mode = PlayerOrientation.ReverseLandscape,
      title = "Reverse Landscape",
      subtitle = "Inverted landscape (convenient when charging cable is on the side)",
      icon = Icons.Default.ScreenRotation,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
    ),
    OrientationOption(
      mode = PlayerOrientation.SensorPortrait,
      title = "Sensor Portrait",
      subtitle = "Auto-rotates between normal & reverse portrait",
      icon = Icons.Default.StayCurrentPortrait,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT,
    ),
    OrientationOption(
      mode = PlayerOrientation.Portrait,
      title = "Portrait (Fixed)",
      subtitle = "Standard vertical portrait mode",
      icon = Icons.Default.CropPortrait,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
    ),
    OrientationOption(
      mode = PlayerOrientation.ReversePortrait,
      title = "Reverse Portrait",
      subtitle = "Inverted vertical portrait mode",
      icon = Icons.Default.ScreenRotation,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
    ),
    OrientationOption(
      mode = PlayerOrientation.Free,
      title = "Free (Full 360° Sensor)",
      subtitle = "Freely follows device orientation in all directions",
      icon = Icons.Default.ScreenRotation,
      activityInfoCode = ActivityInfo.SCREEN_ORIENTATION_SENSOR,
    ),
  )

  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = "Screen Orientation",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Choose how the player rotates and adapts to video dimensions",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
          val isSelected = currentOrientation == option.mode
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                playerPreferences.orientation.set(option.mode)
                activity?.requestedOrientation = option.activityInfoCode
                onDismissRequest()
              },
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = option.title,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = option.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline,
                )
              }
              if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
