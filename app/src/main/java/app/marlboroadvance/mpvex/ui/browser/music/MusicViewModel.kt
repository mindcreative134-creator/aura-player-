package app.marlboroadvance.mpvex.ui.browser.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.domain.lyrics.LrcLibLyricsRepository
import app.marlboroadvance.mpvex.domain.lyrics.LyricsState
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager
import app.marlboroadvance.mpvex.domain.music.LocalMusicRepository
import app.marlboroadvance.mpvex.domain.music.PlayerState
import app.marlboroadvance.mpvex.domain.music.PlaylistStore
import app.marlboroadvance.mpvex.domain.music.Song
import app.marlboroadvance.mpvex.domain.radio.RadioBrowserRepository
import app.marlboroadvance.mpvex.domain.radio.RadioStation
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AlbumItem(val id: Long, val name: String, val artist: String, val songs: List<Song>)
data class ArtistItem(val name: String, val songs: List<Song>)

@UnstableApi
class MusicViewModel(
    private val library: LocalMusicRepository,
    private val playlistStore: PlaylistStore,
    val player: ApnaAudioPlayerManager,
    private val lyricsRepo: LrcLibLyricsRepository,
    private val radioRepo: RadioBrowserRepository
) : ViewModel() {

    // ---------------------------------------------------------------- library
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    val albums: StateFlow<List<AlbumItem>> = _songs.map { list ->
        list.groupBy { it.albumId }
            .map { (id, s) -> AlbumItem(id, s.first().album, s.first().artist, s) }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<ArtistItem>> = _songs.map { list ->
        list.groupBy { it.artist }
            .map { (name, s) -> ArtistItem(name, s) }
            .sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _playlists = MutableStateFlow(playlistStore.load())
    val playlists: StateFlow<Map<String, List<Long>>> = _playlists.asStateFlow()

    fun loadLibrary() {
        viewModelScope.launch { _songs.value = library.loadSongs() }
    }

    fun playSongs(queue: List<Song>, index: Int = 0, shuffle: Boolean = false) =
        player.playSongs(queue, index, shuffle)

    // ---------------------------------------------------------------- playlists
    fun createPlaylist(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || _playlists.value.containsKey(clean)) return
        updatePlaylists(_playlists.value + (clean to emptyList()))
    }

    fun addToPlaylist(name: String, song: Song) {
        val current = _playlists.value[name] ?: return
        if (song.id in current) return
        updatePlaylists(_playlists.value + (name to current + song.id))
    }

    fun deletePlaylist(name: String) = updatePlaylists(_playlists.value - name)

    fun playlistSongs(name: String): List<Song> {
        val byId = _songs.value.associateBy { it.id }
        return _playlists.value[name].orEmpty().mapNotNull { byId[it] }
    }

    private fun updatePlaylists(new: Map<String, List<Long>>) {
        _playlists.value = new
        playlistStore.save(new)
    }

    // ---------------------------------------------------------------- player state
    val playerState: StateFlow<PlayerState> = player.state
    val position: StateFlow<Long> = player.position
    val level: StateFlow<Float> = player.level

    // ---------------------------------------------------------------- lyrics
    @OptIn(ExperimentalCoroutinesApi::class)
    val lyrics: StateFlow<LyricsState> = player.state
        .distinctUntilChangedBy { it.mediaId }
        .flatMapLatest { s ->
            flow {
                if (!s.hasMedia || s.isLive || s.title.isBlank()) {
                    emit(LyricsState.Idle)
                } else {
                    emit(LyricsState.Loading)
                    emit(lyricsRepo.fetch(s.title, s.artist, s.album, s.durationMs))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsState.Idle)

    // ---------------------------------------------------------------- radio
    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()
    private val _radioLoading = MutableStateFlow(false)
    val radioLoading: StateFlow<Boolean> = _radioLoading.asStateFlow()
    private var radioJob: Job? = null

    fun searchRadio(query: String, tag: String?) {
        radioJob?.cancel()
        radioJob = viewModelScope.launch {
            _radioLoading.value = true
            _stations.value =
                if (query.isBlank() && tag == null) radioRepo.topStations() else radioRepo.search(query, tag)
            _radioLoading.value = false
        }
    }

    fun playStation(station: RadioStation) {
        viewModelScope.launch {
            val url = radioRepo.resolveStreamUrl(station.url)
            player.playStream(url, station.name, station.tags.ifBlank { "Live radio" }, live = true)
            radioRepo.registerClick(station.id)
        }
    }

    /** Custom M3U / stream URL. Audio file links are cached; live streams bypass cache. */
    fun playCustomUrl(input: String) {
        val url = input.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        viewModelScope.launch {
            val resolved = radioRepo.resolveStreamUrl(url)
            val path = resolved.substringBefore('?').lowercase()
            val isFile = FILE_EXTENSIONS.any { path.endsWith(".$it") }
            val host = runCatching { java.net.URI(resolved).host }.getOrNull() ?: "Custom stream"
            player.playStream(resolved, title = host, subtitle = "Custom stream", live = !isFile)
        }
    }

    companion object {
        val FILE_EXTENSIONS = listOf("mp3", "m4a", "flac", "wav", "ogg", "opus", "aac")

        fun create(context: Context): MusicViewModel {
            val appContext = context.applicationContext
            return MusicViewModel(
                library = LocalMusicRepository(appContext),
                playlistStore = PlaylistStore(appContext),
                player = ApnaAudioPlayerManager.getInstance(appContext),
                lyricsRepo = LrcLibLyricsRepository(),
                radioRepo = RadioBrowserRepository()
            )
        }
    }
}
