package app.marlboroadvance.mpvex.ui.browser.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.PreferencesScreen
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.LocalThemeTransitionState
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import androidx.compose.foundation.BorderStroke
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens

@Serializable
object MusicLibraryScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @UnstableApi
    @Composable
    override fun Content() {
        MusicLibraryScreenContent(modifier = Modifier.fillMaxSize())
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreenContent(
    modifier: Modifier = Modifier,
    vm: MusicViewModel? = null
) {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val actualVm = vm ?: remember(context) { MusicViewModel.create(context) }
    val appearancePreferences = koinInject<AppearancePreferences>()
    val darkMode by appearancePreferences.darkMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val themeTransition = LocalThemeTransitionState.current
    val coroutineScope = rememberCoroutineScope()
    val titleBounds = remember { mutableStateOf(Rect.Zero) }

    fun toggleDarkMode() {
        when (darkMode) {
            DarkMode.System -> if (isSystemDark) {
                appearancePreferences.darkMode.set(DarkMode.Light)
            } else {
                appearancePreferences.darkMode.set(DarkMode.Dark)
            }
            DarkMode.Light -> if (isSystemDark) {
                appearancePreferences.darkMode.set(DarkMode.System)
            } else {
                appearancePreferences.darkMode.set(DarkMode.Dark)
            }
            DarkMode.Dark -> if (isSystemDark) {
                appearancePreferences.darkMode.set(DarkMode.Light)
            } else {
                appearancePreferences.darkMode.set(DarkMode.System)
            }
        }
    }

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(granted) { if (granted) actualVm.loadLibrary() }

    val songs by actualVm.songs.collectAsState()
    val albums by actualVm.albums.collectAsState()
    val artists by actualVm.artists.collectAsState()
    val playlists by actualVm.playlists.collectAsState()
    val playerState by actualVm.playerState.collectAsState()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }

    // Search state
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredAlbums = remember(albums, searchQuery) {
        if (searchQuery.isBlank()) albums
        else albums.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredArtists = remember(artists, searchQuery) {
        if (searchQuery.isBlank()) artists
        else artists.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists
        else playlists.filter {
            it.key.contains(searchQuery, ignoreCase = true)
        }
    }

    val tabs = listOf(
        "Songs (${filteredSongs.size})",
        "Albums (${filteredAlbums.size})",
        "Artists (${filteredArtists.size})",
        "Playlists (${filteredPlaylists.size})",
        "Radio"
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isSearchActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search songs, albums, artists...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                isSearchActive = false
                            }
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Search")
                        }
                    }
                }
            } else {
                val titleModifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        titleBounds.value = coordinates.boundsInWindow()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { localOffset ->
                                if (themeTransition?.isAnimating == true) return@detectTapGestures
                                val windowOffset = Offset(
                                    titleBounds.value.left + localOffset.x,
                                    titleBounds.value.top + localOffset.y
                                )
                                themeTransition?.startTransition(windowOffset)
                                coroutineScope.launch {
                                    toggleDarkMode()
                                }
                            }
                        )
                    }

                TopAppBar(
                    title = {
                        Column(modifier = titleModifier) {
                            Text(
                                text = "Music Library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${songs.size} tracks • ${albums.size} albums",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "Search Music",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            backstack.add(PreferencesScreen)
                        }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (MaterialTheme.colorScheme.background == Color.Black) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    )
                )
            }
        },
        bottomBar = {
            if (playerState.hasMedia) {
                MiniPlayerPill(
                    playerManager = actualVm.player,
                    onClick = { showNowPlaying = true },
                    modifier = Modifier.padding(bottom = 68.dp)
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        text = {
                            Text(
                                text = title,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (!granted && tab != 4) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Storage or audio permission is required to display your music tracks.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { audioLauncher.launch(audioPermission) },
                        modifier = Modifier.padding(top = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission")
                    }
                }
            } else when (tab) {
                0 -> SongsTab(
                    songs = filteredSongs,
                    allSongs = songs,
                    vm = actualVm,
                    playlistNames = playlists.keys.toList()
                )
                1 -> AlbumsTab(albums = filteredAlbums, vm = actualVm)
                2 -> ArtistsTab(artists = filteredArtists, vm = actualVm)
                3 -> PlaylistsTab(vm = actualVm, playlists = filteredPlaylists)
                4 -> RadioTabContent(vm = actualVm, externalQuery = searchQuery)
            }
        }
    }

    if (showNowPlaying) NowPlayingSheet(vm = actualVm, onDismiss = { showNowPlaying = false })
}

@UnstableApi
@Composable
private fun SongsTab(
    songs: List<Song>,
    allSongs: List<Song>,
    vm: MusicViewModel,
    playlistNames: List<String>
) {
    val playerState by vm.playerState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        // Quick Action Bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { vm.playSongs(songs.ifEmpty { allSongs }) },
                enabled = songs.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All")
            }

            OutlinedButton(
                onClick = { vm.playSongs(songs.ifEmpty { allSongs }, shuffle = true) },
                enabled = songs.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle")
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${songs.size} tracks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No songs found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                    var menu by remember { mutableStateOf(false) }
                    val isCurrent = playerState.mediaId == song.id.toString()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { vm.playSongs(songs, index) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                AuraTheme.selectedContainer
                            } else {
                                AuraTheme.cardBackground
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) AuraTheme.accent.copy(alpha = 0.5f) else AuraTheme.cardBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArt(
                                songUri = song.uri,
                                albumId = song.albumId,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) AuraTheme.accent else AuraTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.artist} • ${formatTime(song.durationMs)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTheme.textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    EqualizerBars(
                                        playing = playerState.isPlaying,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { menu = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.MoreVert,
                                            contentDescription = "Options",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menu,
                                        onDismissRequest = { menu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Play Now") },
                                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                                            onClick = {
                                                vm.playSongs(listOf(song))
                                                menu = false
                                            }
                                        )
                                        if (playlistNames.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Create a playlist first") },
                                                onClick = { menu = false }
                                            )
                                        }
                                        playlistNames.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text("Add to $name") },
                                                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                                                onClick = {
                                                    vm.addToPlaylist(name, song)
                                                    menu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun AlbumsTab(albums: List<AlbumItem>, vm: MusicViewModel) {
    if (albums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(albums, key = { it.id }) { album ->
                val first = album.songs.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { vm.playSongs(album.songs) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AuraTheme.cardBackground
                    ),
                    border = BorderStroke(1.dp, AuraTheme.cardBorder)
                ) {
                    Column {
                        AlbumArt(
                            songUri = first.uri,
                            albumId = first.albumId,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        )
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = album.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AuraTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = album.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${album.songs.size} tracks",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun ArtistsTab(artists: List<ArtistItem>, vm: MusicViewModel) {
    if (artists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No artists found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(artists, key = { it.name }) { artist ->
                val first = artist.songs.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { vm.playSongs(artist.songs) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AuraTheme.cardBackground
                    ),
                    border = BorderStroke(1.dp, AuraTheme.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArt(
                            songUri = first.uri,
                            albumId = first.albumId,
                            modifier = Modifier.size(54.dp),
                            shape = CircleShape
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = AuraTheme.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${artist.songs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { vm.playSongs(artist.songs) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play Artist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun PlaylistsTab(vm: MusicViewModel, playlists: Map<String, List<Long>>) {
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showCreate = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Playlist")
            }

            Text(
                text = "${playlists.size} playlists",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No playlists yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists.keys.toList(), key = { it }) { name ->
                    val count = playlists[name]?.size ?: 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { vm.playSongs(vm.playlistSongs(name)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AuraTheme.cardBackground
                        ),
                        border = BorderStroke(1.dp, AuraTheme.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = AuraTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$count songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AuraTheme.textSecondary
                                )
                            }

                            IconButton(
                                onClick = { vm.deletePlaylist(name) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete Playlist",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Playlist name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.createPlaylist(newName)
                        newName = ""
                        showCreate = false
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

