package app.marlboroadvance.mpvex.ui.browser.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.marlboroadvance.mpvex.domain.download.ApnaDownloadManager
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayLinkSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onPlayLink: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val context = LocalContext.current
  val downloadManager = remember(context) { ApnaDownloadManager.getInstance(context) }
  var linkInputUrl by remember { mutableStateOf("") }
  var isLinkInputUrlValid by remember { mutableStateOf(true) }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(true) {
    if (isOpen) {
      linkInputUrl = ""
      isLinkInputUrlValid = true
    }
  }

  val handleDismiss = {
    onDismiss()
  }

  val handleConfirm = {
    val url = linkInputUrl.trim()
    if (url.isNotBlank() && MediaUtils.isURLValid(url)) {
      // Optimistically record in history so it shows up immediately
      coroutineScope.launch {
        val uri = url.toUri()
        val name = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { url } ?: url
        RecentlyPlayedOps.addRecentlyPlayed(
          filePath = url,
          fileName = name,
          launchSource = "play_link",
        )
      }
      onPlayLink(url)
      onDismiss()
    }
  }

  val handleDownload = {
    val cleanUrl = linkInputUrl.trim()
    if (cleanUrl.isNotBlank() && isLinkInputUrlValid) {
      val filename = cleanUrl.substringAfterLast('/').substringBefore('?').ifBlank { "stream_download.mp4" }
      downloadManager.enqueueDownload(url = cleanUrl, customName = filename)
      Toast.makeText(context, "Download started: $filename", Toast.LENGTH_SHORT).show()
      onDismiss()
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

  ModalBottomSheet(
    onDismissRequest = handleDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    modifier = modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp)
          .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Title
      Text(
        text = "Play Link",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // URL Input
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = linkInputUrl,
          onValueChange = { newValue ->
            linkInputUrl = newValue
            isLinkInputUrlValid = newValue.isBlank() || MediaUtils.isURLValid(newValue)
          },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Enter URL") },
          placeholder = { Text("https://example.com/video.mp4") },
          singleLine = true,
          isError = linkInputUrl.isNotBlank() && !isLinkInputUrlValid,
          trailingIcon = {
            if (linkInputUrl.isNotBlank()) {
              ValidationIcon(isValid = isLinkInputUrlValid)
            }
          },
        )

        if (linkInputUrl.isNotBlank() && !isLinkInputUrlValid) {
          Text(
            text = "Invalid URL protocol",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
        }
      }

      // Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = handleDismiss) {
          Text(
            text = "Cancel",
            fontWeight = FontWeight.Medium,
          )
        }

        OutlinedButton(
          onClick = handleDownload,
          enabled = linkInputUrl.isNotBlank() && isLinkInputUrlValid,
          shape = RoundedCornerShape(24.dp),
        ) {
          Text(
            text = "Download",
            fontWeight = FontWeight.Medium,
          )
        }

        Button(
          onClick = handleConfirm,
          enabled = linkInputUrl.isNotBlank() && isLinkInputUrlValid,
          shape = RoundedCornerShape(24.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
          Text(
            text = "Play",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

@Composable
private fun ValidationIcon(isValid: Boolean) {
  if (isValid) {
    Icon(
      Icons.Filled.Check,
      contentDescription = "Valid URL",
      tint = MaterialTheme.colorScheme.primary,
    )
  } else {
    Icon(
      Icons.Filled.Info,
      contentDescription = "Invalid URL",
      tint = MaterialTheme.colorScheme.error,
    )
  }
}
