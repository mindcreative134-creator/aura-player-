package app.marlboroadvance.mpvex.ui.downloads

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.download.ApnaDownloadManager
import app.marlboroadvance.mpvex.domain.download.DownloadStatus
import app.marlboroadvance.mpvex.domain.download.DownloadTask
import app.marlboroadvance.mpvex.domain.download.formatDownloadBytes
import app.marlboroadvance.mpvex.domain.download.formatDownloadSpeed
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
object DownloadsScreen : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val downloadManager = remember(context) { ApnaDownloadManager.getInstance(context) }
    val tasks by downloadManager.tasks.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredTasks = remember(tasks, selectedFilter) {
      when (selectedFilter) {
        "Active" -> tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
        "Completed" -> tasks.filter { it.status == DownloadStatus.COMPLETED }
        "Paused" -> tasks.filter { it.status == DownloadStatus.PAUSED }
        else -> tasks
      }
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = "Downloads",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
              )
              Text(
                text = "${tasks.count { it.status == DownloadStatus.DOWNLOADING }} downloading • ${tasks.size} total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            if (tasks.any { it.status == DownloadStatus.COMPLETED }) {
              IconButton(onClick = { downloadManager.clearCompleted() }) {
                Icon(Icons.Rounded.ClearAll, contentDescription = "Clear Completed")
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        )
      },
      floatingActionButton = {
        FloatingActionButton(
          onClick = { showAddDialog = true },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = RoundedCornerShape(16.dp)
        ) {
          Icon(Icons.Rounded.Add, contentDescription = "Add Download")
        }
      }
    ) { padding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
      ) {
        // Filter Chips
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          val filters = listOf("All", "Active", "Completed", "Paused")
          items(filters) { f ->
            FilterChip(
              selected = selectedFilter == f,
              onClick = { selectedFilter = f },
              label = { Text(f) }
            )
          }
        }

        if (filteredTasks.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Koi download nahi hai",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Download")
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredTasks, key = { it.id }) { task ->
              DownloadTaskCard(
                task = task,
                onPause = { downloadManager.pauseDownload(task.id) },
                onResume = { downloadManager.resumeDownload(task.id) },
                onRetry = { downloadManager.retryDownload(task.id) },
                onCancel = { downloadManager.cancelDownload(task.id, deleteFile = true) },
                onPlay = {
                  val file = File(task.filePath)
                  if (file.exists()) {
                    MediaUtils.playFile(file.absolutePath, context, "download_completed")
                  } else {
                    Toast.makeText(context, "File nahi mili!", Toast.LENGTH_SHORT).show()
                  }
                }
              )
            }
          }
        }
      }
    }

    if (showAddDialog) {
      AddDownloadDialog(
        onDismiss = { showAddDialog = false },
        onConfirm = { url, name ->
          downloadManager.enqueueDownload(url, name.takeIf { it.isNotBlank() })
          showAddDialog = false
          Toast.makeText(context, "Download shuru ho gaya!", Toast.LENGTH_SHORT).show()
        }
      )
    }
  }
}

@Composable
private fun DownloadTaskCard(
  task: DownloadTask,
  onPause: () -> Unit,
  onResume: () -> Unit,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
  onPlay: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Status Icon Badge
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              when (task.status) {
                DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.18f)
                DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when (task.status) {
              DownloadStatus.COMPLETED -> Icons.Rounded.CheckCircle
              DownloadStatus.DOWNLOADING -> Icons.Rounded.Download
              DownloadStatus.FAILED -> Icons.Rounded.Error
              else -> Icons.Rounded.Pause
            },
            contentDescription = null,
            tint = when (task.status) {
              DownloadStatus.COMPLETED -> Color(0xFF10B981)
              DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
              DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = task.fileName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = when (task.status) {
              DownloadStatus.COMPLETED -> "Completed • ${formatDownloadBytes(task.downloadedBytes)}"
              DownloadStatus.DOWNLOADING -> {
                val downloadedStr = formatDownloadBytes(task.downloadedBytes)
                val totalStr = if (task.totalBytes > 0) formatDownloadBytes(task.totalBytes) else "..."
                val speedStr = formatDownloadSpeed(task.speedBytesPerSec)
                "$downloadedStr / $totalStr • $speedStr"
              }
              DownloadStatus.PAUSED -> "Paused • ${formatDownloadBytes(task.downloadedBytes)}"
              DownloadStatus.FAILED -> task.errorMessage ?: "Download Error"
              DownloadStatus.PENDING -> "Waiting in queue..."
              DownloadStatus.CANCELLED -> "Cancelled"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (task.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
          when (task.status) {
            DownloadStatus.DOWNLOADING -> {
              IconButton(onClick = onPause) {
                Icon(Icons.Rounded.Pause, contentDescription = "Pause")
              }
            }
            DownloadStatus.PAUSED, DownloadStatus.PENDING -> {
              IconButton(onClick = onResume) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume")
              }
            }
            DownloadStatus.FAILED -> {
              IconButton(onClick = onRetry) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Retry")
              }
            }
            DownloadStatus.COMPLETED -> {
              Button(
                onClick = onPlay,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF10B981)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
            else -> {}
          }

          IconButton(onClick = onCancel) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
          }
        }
      }

      // Progress bar if downloading or paused
      if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED) {
        Spacer(modifier = Modifier.height(10.dp))
        if (task.totalBytes > 0L) {
          LinearProgressIndicator(
            progress = { task.progressFraction },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(CircleShape),
          )
        } else {
          LinearProgressIndicator(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(CircleShape)
          )
        }
      }
    }
  }
}

@Composable
private fun AddDownloadDialog(
  onDismiss: () -> Unit,
  onConfirm: (url: String, fileName: String) -> Unit
) {
  var url by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Download Media Link", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = url,
          onValueChange = { url = it },
          label = { Text("Direct Video / Audio URL") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("File Name (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(url, name) },
        enabled = url.isNotBlank(),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("Start Download")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
