package app.marlboroadvance.mpvex.ui.preferences

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.SettingsManager
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject
import java.io.File

@Serializable
object AdvancedPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val settingsManager = koinInject<SettingsManager>()
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var importStats by remember { mutableStateOf<SettingsManager.ImportStats?>(null) }
    var exportStats by remember { mutableStateOf<SettingsManager.ExportStats?>(null) }

    // Export settings launcher
    val exportLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/xml"),
      ) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.exportSettings(it).fold(
              onSuccess = { stats ->
                exportStats = stats
                showExportDialog = true
              },
              onFailure = { error ->
                Toast.makeText(
                  context,
                  "Export failed: ${error.message}",
                  Toast.LENGTH_LONG,
                ).show()
              },
            )
          }
        }
      }

    // Import settings launcher
    val importLauncher =
      rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
      ) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.importSettings(it).fold(
              onSuccess = { stats ->
                importStats = stats
                showImportDialog = true
              },
              onFailure = { error ->
                Toast.makeText(
                  context,
                  "Import failed: ${error.message}",
                  Toast.LENGTH_LONG,
                ).show()
              },
            )
          }
        }
      }

    // Export results dialog
    if (showExportDialog && exportStats != null) {
      AlertDialog(
        onDismissRequest = { showExportDialog = false },
        title = { Text("Export Complete") },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
          ) {
            Text("Successfully exported ${exportStats?.totalExported} items to XML backup.")
          }
        },
        confirmButton = {
          TextButton(onClick = { showExportDialog = false }) {
            Text("OK")
          }
        },
      )
    }

    // Import results dialog
    if (showImportDialog && importStats != null) {
      AlertDialog(
        onDismissRequest = { showImportDialog = false },
        title = { Text("Import Complete") },
        text = {
          Text(
            "Successfully imported: ${importStats?.imported}\n" +
              "Failed: ${importStats?.failed}\n" +
              "Version: ${importStats?.version}\n\n" +
              "Please restart the app for all changes to take effect.",
          )
        },
        confirmButton = {
          TextButton(onClick = { showImportDialog = false }) {
            Text("OK")
          }
        },
      )
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = "Advanced & Architecture",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
          },
          navigationIcon = {
            IconButton(onClick = backStack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        ) {
          // Engine Telemetry Card
          item {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Aura Player Core Architecture",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Native Android hardware-accelerated video pipeline with direct GPU rendering, low-latency display sync, and high-precision timeline caching.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  EngineBadge("Direct GPU Pipeline")
                  EngineBadge("Hardware Codec")
                  EngineBadge("High-Res Audio")
                  EngineBadge("Optimized Memory")
                }
              }
            }
          }

          // Backup & Restore Section
          item {
            PreferenceSectionHeader(title = "Backup & Restore")
          }

          item {
            PreferenceCard {
              Preference(
                title = { Text(text = "Export Settings") },
                summary = {
                  Text(
                    text = "Export player configuration and preferences to an XML backup",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(
                    Icons.Outlined.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  exportLauncher.launch(settingsManager.getDefaultExportFilename())
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text(text = "Import Settings") },
                summary = {
                  Text(
                    text = "Restore player configuration from a previously exported XML backup",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(
                    Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  importLauncher.launch(arrayOf("text/xml", "application/xml", "*/*"))
                },
              )
            }
          }

          // History Section
          item {
            PreferenceSectionHeader(title = "Playback History & States")
          }

          item {
            PreferenceCard {
              var isConfirmDialogShown by remember { mutableStateOf(false) }
              val mpvexDatabase = koinInject<MpvExDatabase>()
              val enableRecentlyPlayed by preferences.enableRecentlyPlayed.collectAsState()

              SwitchPreference(
                value = enableRecentlyPlayed,
                onValueChange = preferences.enableRecentlyPlayed::set,
                title = { Text("Track Recently Played Videos") },
                summary = {
                  Text(
                    "Maintain playback timestamps, track selection, and progress in history",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text("Clear Playback History") },
                summary = {
                  Text(
                    "Reset all saved timestamps and recently watched records",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onClick = { isConfirmDialogShown = true },
              )

              if (isConfirmDialogShown) {
                ConfirmDialog(
                  "Clear playback history?",
                  "This will clear all saved playback positions and media history.",
                  onConfirm = {
                    scope.launch(Dispatchers.IO) {
                      runCatching {
                        mpvexDatabase.videoDataDao().clearAllPlaybackStates()
                        RecentlyPlayedOps.clearAll()
                      }.onSuccess {
                        withContext(Dispatchers.Main) {
                          isConfirmDialogShown = false
                          Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
                        }
                      }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                          isConfirmDialogShown = false
                          Toast.makeText(context, "Failed to clear: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                      }
                    }
                  },
                  onCancel = { isConfirmDialogShown = false },
                )
              }
            }
          }

          // Cache Section
          item {
            PreferenceSectionHeader(title = "Cache & Storage Management")
          }

          item {
            PreferenceCard {
              var isClearThumbsConfirmShown by remember { mutableStateOf(false) }
              val thumbnailRepository = koinInject<ThumbnailRepository>()

              Preference(
                title = { Text(text = "Clear thumbnail cache") },
                summary = {
                  Text(
                    text = "Delete all cached video thumbnail frames from disk and memory",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                onClick = { isClearThumbsConfirmShown = true },
              )

              if (isClearThumbsConfirmShown) {
                ConfirmDialog(
                  title = "Clear thumbnail cache?",
                  subtitle = "This will delete cached video thumbnails. They will regenerate smoothly when browsing.",
                  onConfirm = {
                    scope.launch(Dispatchers.IO) {
                      runCatching {
                        thumbnailRepository.clearThumbnailCache()
                      }.onSuccess {
                        withContext(Dispatchers.Main) {
                          isClearThumbsConfirmShown = false
                          Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                        }
                      }.onFailure { error ->
                        withContext(Dispatchers.Main) {
                          isClearThumbsConfirmShown = false
                          Toast.makeText(context, "Failed to clear: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                      }
                    }
                  },
                  onCancel = { isClearThumbsConfirmShown = false },
                )
              }

              PreferenceDivider()

              Preference(
                title = { Text(text = "Clear cached fonts") },
                summary = {
                  Text(
                    text = "Remove any downloaded custom subtitle fonts",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    val fontsDir = File(context.filesDir.path + "/fonts")
                    if (fontsDir.exists()) {
                      fontsDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.lowercase().matches(".*\\.[ot]tf$".toRegex())) {
                          file.delete()
                        }
                      }
                    }
                    withContext(Dispatchers.Main) {
                      Toast.makeText(context, "Font cache cleared", Toast.LENGTH_SHORT).show()
                    }
                  }
                },
              )
            }
          }

          // Logging Section
          item {
            PreferenceSectionHeader(title = "Logging & Diagnostics")
          }

          item {
            PreferenceCard {
              val activity = LocalActivity.current!!
              @Suppress("DEPRECATION")
              val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
              val verboseLogging by preferences.verboseLogging.collectAsState()

              SwitchPreference(
                value = verboseLogging,
                onValueChange = preferences.verboseLogging::set,
                title = { Text("Verbose Engine Logging") },
                summary = {
                  Text(
                    "Include granular playback state, buffer, and decoder event traces in logcat",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                icon = {
                  Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
              )

              PreferenceDivider()

              Preference(
                title = { Text("Dump Diagnostic Logs") },
                summary = {
                  Text(
                    "Collect device hardware details and playback logs for troubleshooting",
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onClick = {
                  scope.launch(Dispatchers.IO) {
                    val deviceInfo = CrashActivity.collectDeviceInfo()
                    val logcat = CrashActivity.collectLogcat()

                    clipboard.setText(AnnotatedString(CrashActivity.concatLogs(deviceInfo, null, logcat)))
                    CrashActivity.shareLogs(deviceInfo, null, logcat, activity)
                  }
                },
              )
            }
          }

          item {
            Spacer(modifier = Modifier.height(32.dp))
          }
        }
      }
    }
  }
}

@Composable
private fun EngineBadge(text: String) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}
