package app.marlboroadvance.mpvex.ui.preferences

import android.content.pm.PackageManager
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.update.UpdateViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable
object AboutScreen : Screen {
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val clipboardManager = LocalClipboardManager.current
    val packageManager: PackageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName = packageInfo.versionName?.substringBefore('-') ?: packageInfo.versionName ?: BuildConfig.VERSION_NAME
    val buildType = BuildConfig.BUILD_TYPE

    var showPrivacyDialog by remember { mutableStateOf(false) }

    Scaffold(
      topBar = {
        TopAppBar(
          title = { 
            Text(
              text = "About",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            ) 
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowBack, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { paddingValues ->
      val cs = MaterialTheme.colorScheme
      val colorPrimary = cs.primaryContainer
      val colorTertiary = cs.tertiaryContainer
      val transition = rememberInfiniteTransition()
      val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
          infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse,
          ),
      )
      val cornerRadius = 28.dp
      
      Column(
        modifier =
          Modifier
            .padding(paddingValues)
            .verticalScroll(rememberScrollState()),
      ) {
        // App Header Card
        PreferenceCard {
          Box(
            modifier =
              Modifier
                .drawWithCache {
                  val cx = size.width - size.width * fraction
                  val cy = size.height * fraction

                  val gradient =
                    Brush.radialGradient(
                      colors = listOf(colorPrimary, colorTertiary),
                      center = Offset(cx, cy),
                      radius = 800f,
                    )

                  onDrawBehind {
                    drawRoundRect(
                      brush = gradient,
                      cornerRadius =
                        CornerRadius(
                          cornerRadius.toPx(),
                          cornerRadius.toPx(),
                        ),
                    )
                  }
                }
                .padding(20.dp),
          ) {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(68.dp)) {
                  AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                      ImageView(ctx).apply {
                        setImageResource(R.mipmap.ic_launcher)
                      }
                    },
                  )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Aura Player",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onPrimaryContainer,
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = "v$versionName (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = cs.onPrimaryContainer.copy(alpha = 0.9f),
                  )
                  Text(
                    text = "Cinema & Sound Suite with Ambient Lighting",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onPrimaryContainer.copy(alpha = 0.75f),
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Device Info Quick Bar
              Column(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable {
                      clipboardManager.setText(AnnotatedString(collectDeviceInfo()))
                      Toast.makeText(context, "Device info copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(bottom = 6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Device Info",
                    modifier = Modifier.size(18.dp),
                    tint = cs.onPrimaryContainer,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Device & Hardware Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onPrimaryContainer,
                  )
                }
                Text(
                  text = collectDeviceInfo(),
                  style = MaterialTheme.typography.bodySmall,
                  color = cs.onPrimaryContainer.copy(alpha = 0.85f),
                )
              }
            }
          }
        }

        Spacer(Modifier.height(12.dp))

        // Features & Capabilities Section
        PreferenceSectionHeader(title = "Core Capabilities")
        PreferenceCard {
          FeatureRow(
            icon = Icons.Filled.Bolt,
            title = "Hardware Decoding",
            description = "Ultra HD, 4K & HDR hardware acceleration with zero stutter"
          )
          PreferenceDivider()
          FeatureRow(
            icon = Icons.Filled.VolumeUp,
            title = "MX-Style Gestures",
            description = "Volume boost up to 200%, Brightness HUD, and scrub seeking"
          )
          PreferenceDivider()
          FeatureRow(
            icon = Icons.Filled.Subtitles,
            title = "Advanced Subtitles",
            description = "Full ASS/SSA, SRT, VTT styling and multi-language track switching"
          )
          PreferenceDivider()
          FeatureRow(
            icon = Icons.Filled.Speed,
            title = "Precision Speed & Zoom",
            description = "Variable playback speeds (0.25x - 4x) and pinch-to-zoom"
          )
        }

        Spacer(Modifier.height(12.dp))

        // App Details Section
        PreferenceSectionHeader(title = "Application Details")
        PreferenceCard {
          DetailRow(title = "App Name", value = "Aura Player")
          PreferenceDivider()
          DetailRow(title = "Package", value = context.packageName)
          PreferenceDivider()
          DetailRow(title = "Engine", value = "Aura Ultra-HD Engine (Media3)")
          PreferenceDivider()
          DetailRow(title = "Release Channel", value = "Production / Standard")
        }

        Spacer(Modifier.height(12.dp))

        // Legal & Compliance Section
        PreferenceSectionHeader(title = "Privacy & Compliance")
        PreferenceCard {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showPrivacyDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Privacy Policy & Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "100% offline-first • Zero telemetry • Local playback only",
                style = MaterialTheme.typography.bodySmall,
                color = cs.outline,
              )
            }
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = cs.primary,
            )
          }
        }

        Spacer(Modifier.height(24.dp))
      }
    }

    if (showPrivacyDialog) {
      AlertDialog(
        onDismissRequest = { showPrivacyDialog = false },
        title = {
          Text(
            text = "Privacy & Offline Architecture",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Apna Player is designed with total user privacy as a core principle:",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "• Zero Tracking: No analytics, tracking SDKs, or background telemetry are collected.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "• Offline-First: All video indexing and thumbnail generation takes place strictly on-device using Android MediaStore.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "• Safe & Clean: Built using modern Jetpack Compose, Material 3, and Kotlin Coroutines in full compliance with Google Play Store policies.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showPrivacyDialog = false }) {
            Text("Got It", fontWeight = FontWeight.Bold)
          }
        },
      )
    }
  }
}

@Composable
private fun FeatureRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  description: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
      )
    }
  }
}

@Composable
private fun DetailRow(
  title: String,
  value: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun LicenseItem(name: String, license: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = name,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = license,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}
