package app.marlboroadvance.mpvex.ui.browser.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay

private val radioTags = listOf(
    "Top Hits" to null,
    "Bollywood" to "bollywood",
    "Retro" to "retro",
    "Lo-Fi" to "lofi"
)

@UnstableApi
@Composable
fun RadioTabContent(
    vm: MusicViewModel,
    modifier: Modifier = Modifier,
    externalQuery: String = ""
) {
    val stations by vm.stations.collectAsState()
    val loading by vm.radioLoading.collectAsState()
    val playerState by vm.playerState.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }
    var customUrl by rememberSaveable { mutableStateOf("") }

    val activeQuery = if (externalQuery.isNotBlank()) externalQuery else query

    LaunchedEffect(selectedTag, activeQuery) {
        if (activeQuery.isNotEmpty()) {
            delay(400)
            vm.searchRadio(activeQuery, selectedTag)
        } else {
            vm.searchRadio("", selectedTag)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Custom M3U / stream URL input
        OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("Stream / M3U URL paste karein") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.playCustomUrl(customUrl) }),
            trailingIcon = {
                IconButton(onClick = { vm.playCustomUrl(customUrl) }, enabled = customUrl.isNotBlank()) {
                    Icon(Icons.Rounded.PlayArrow, "Play URL")
                }
            }
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Radio station search") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(radioTags) { (label, tag) ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = tag },
                    label = { Text(label) }
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                stations.isEmpty() -> Text(
                    "Koi station nahi mila",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(stations, key = { it.id.ifBlank { it.url } }) { station ->
                        val isCurrent = playerState.mediaId?.let { station.url.endsWith(it) || it.endsWith(station.url) } == true
                        ListItem(
                            modifier = Modifier.clickable { vm.playStation(station) },
                            leadingContent = {
                                if (isCurrent && playerState.isPlaying) EqualizerBars(playing = true)
                                else Icon(Icons.Rounded.Radio, null)
                            },
                            headlineContent = { Text(station.name, maxLines = 1) },
                            supportingContent = {
                                Text(
                                    listOf(station.tags, station.country).filter { it.isNotBlank() }.joinToString(" • "),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
