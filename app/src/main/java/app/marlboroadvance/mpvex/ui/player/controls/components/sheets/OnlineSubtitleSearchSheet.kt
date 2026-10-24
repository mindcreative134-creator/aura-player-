package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.domain.subtitle.OnlineSubtitle
import app.marlboroadvance.mpvex.domain.subtitle.OnlineSubtitleSearchRepository
import app.marlboroadvance.mpvex.domain.subtitle.OnlineSubtitleSearchRequest
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.coroutines.launch

internal fun cleanMediaTitle(raw: String): String {
  var name = raw.substringBeforeLast(".")
  name = name.replace(Regex("\\[.*?\\]"), " ")
  name = name.replace(Regex("\\(.*?\\)"), " ")
  name = name.replace(Regex("[._-]"), " ")
  val patterns = listOf(
    "\\b(2160p|1080p|720p|480p|4k|uhd|fhd|hd)\\b",
    "\\b(web[- ]?dl|webrip|bluray|blu[- ]?ray|brrip|bdrip|hdtv|dvdrip|cam|telesync)\\b",
    "\\b(x264|x265|h264|h265|hevc|avc|10bit|8bit)\\b",
    "\\b(aac|dts|ac3|ddp5?|dd5?|mp3|eac3|truehd|atmos)\\b",
    "\\b(hindi|english|tamil|telugu|korean|dual[- ]?audio|esub|subs?|multi)\\b",
    "\\b(s\\d{1,2}e\\d{1,2}|s\\d{1,2}|ep?\\d{1,3})\\b",
    "\\b(repack|proper|unrated|extended|director'?s[- ]?cut|remux)\\b"
  )
  for (pat in patterns) {
    name = name.replace(Regex("(?i)$pat"), " ")
  }
  val cleaned = name.replace(Regex("\\s+"), " ").trim()
  return if (cleaned.isNotBlank()) cleaned else raw.substringBeforeLast(".").trim()
}

@Composable
fun OnlineSubtitleSearchSheet(
  mediaTitle: String,
  onDismissRequest: () -> Unit,
  onSubtitleSelected: (Uri) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val repository = remember { OnlineSubtitleSearchRepository(context) }
  val keyboardController = LocalSoftwareKeyboardController.current

  val initialQuery = remember(mediaTitle) {
    cleanMediaTitle(mediaTitle)
  }

  var searchQuery by remember { mutableStateOf(initialQuery) }
  var isSearching by remember { mutableStateOf(false) }
  var downloadingSubtitleId by remember { mutableStateOf<String?>(null) }
  var searchResults by remember { mutableStateOf<List<OnlineSubtitle>>(emptyList()) }
  var selectedLanguageFilter by remember { mutableStateOf("All") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val languages = listOf("All", "English", "Hindi", "Spanish", "French", "Arabic", "German", "Japanese")

  fun performSearch() {
    if (searchQuery.isBlank()) return
    keyboardController?.hide()
    scope.launch {
      isSearching = true
      errorMessage = null
      val result = repository.search(OnlineSubtitleSearchRequest(query = searchQuery))
      isSearching = false
      result.fold(
        onSuccess = { list ->
          searchResults = list
          if (list.isEmpty()) {
            errorMessage = "No subtitles found for '$searchQuery'"
          }
        },
        onFailure = { error ->
          errorMessage = error.localizedMessage ?: "Failed to search subtitles"
        }
      )
    }
  }

  // Auto trigger search on sheet open
  LaunchedEffect(initialQuery) {
    if (initialQuery.isNotBlank() && searchResults.isEmpty()) {
      performSearch()
    }
  }

  val filteredResults = remember(searchResults, selectedLanguageFilter) {
    if (selectedLanguageFilter == "All") {
      searchResults
    } else {
      searchResults.filter {
        it.displayLanguage.contains(selectedLanguageFilter, ignoreCase = true) ||
          it.language.equals(selectedLanguageFilter.take(2), ignoreCase = true)
      }
    }
  }

  PlayerSheet(onDismissRequest = onDismissRequest) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Online Subtitles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(onClick = onDismissRequest) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Search Box
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search movie / show name...") },
        singleLine = true,
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(end = 4.dp)
          ) {
            IconButton(
              onClick = {
                val cleaned = cleanMediaTitle(searchQuery.ifBlank { mediaTitle })
                searchQuery = cleaned
                performSearch()
              }
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Auto Clean Title",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
              }
            }
          }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Language Filter Chips
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(languages) { lang ->
          FilterChip(
            selected = selectedLanguageFilter == lang,
            onClick = { selectedLanguageFilter = lang },
            label = { Text(lang) }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Content area
      when {
        isSearching -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator()
              Text(
                "Searching online subtitles...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        errorMessage != null && searchResults.isEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
              )
              Text(
                errorMessage ?: "No subtitles found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Button(onClick = { performSearch() }) {
                Text("Retry")
              }
            }
          }
        }
        filteredResults.isEmpty() && searchResults.isNotEmpty() -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              "No subtitles matching filter '$selectedLanguageFilter'",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        else -> {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 380.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(filteredResults, key = { it.id.ifBlank { it.url } }) { sub ->
              val isDownloading = downloadingSubtitleId == (sub.id.ifBlank { sub.url })

              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable(enabled = !isDownloading) {
                    downloadingSubtitleId = sub.id.ifBlank { sub.url }
                    scope.launch {
                      val result = repository.download(sub, mediaTitle)
                      downloadingSubtitleId = null
                      result.fold(
                        onSuccess = { file ->
                          Toast.makeText(context, "Loaded: ${sub.displayName}", Toast.LENGTH_SHORT).show()
                          onSubtitleSelected(Uri.fromFile(file))
                          onDismissRequest()
                        },
                        onFailure = { error ->
                          Toast.makeText(
                            context,
                            "Download error: ${error.localizedMessage}",
                            Toast.LENGTH_SHORT
                          ).show()
                        }
                      )
                    }
                  },
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // Language Badge
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(MaterialTheme.colorScheme.primaryContainer)
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = sub.displayLanguage.take(3).uppercase(),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                  }

                  // Title and metadata
                  Column(
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(
                      text = sub.displayName,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = buildString {
                        append(sub.displayLanguage)
                        append(" • ")
                        append(sub.format.uppercase())
                        if (sub.isHashMatch) append(" • Exact Match")
                        if (sub.isHearingImpaired) append(" • CC")
                        if (sub.downloadCount > 0) append(" • ${sub.downloadCount} dl")
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  // Action
                  if (isDownloading) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(24.dp),
                      strokeWidth = 2.dp
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Download,
                      contentDescription = "Download & Load",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }
  }
}
