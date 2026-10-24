package app.marlboroadvance.mpvex.domain.music

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import app.marlboroadvance.mpvex.MainActivity

data class PlayerState(
    val hasMedia: Boolean = false,
    val mediaId: String? = null,
    val songUri: Uri? = null,
    val albumId: Long = 0L,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isLive: Boolean = false,
    val error: String? = null
)

/**
 * App-wide single ExoPlayer manager.
 * Owns the Media3 player, LevelAudioProcessor for real-time Waveform HUD,
 * 200MB disk stream caching, and dual-mode pause-for-video integration.
 */
@UnstableApi
class ApnaAudioPlayerManager(
    private val context: Context,
    cacheManager: AudioCacheManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val levelProcessor = LevelAudioProcessor()

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink? = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false) // 16-bit PCM for visualizer audio processor
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(levelProcessor))
            .build()
    }

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheManager.dataSourceFactory()))
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true) // auto-pause on headphone unplug
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    /** 0..1 real-time amplitude flow for Waveform HUD. */
    val level: StateFlow<Float> = levelProcessor.level

    // Centralized NowPlaying visibility state for UI
    private val _isNowPlayingVisible = MutableStateFlow(false)
    val isNowPlayingVisible: StateFlow<Boolean> = _isNowPlayingVisible.asStateFlow()

    fun openNowPlaying() {
        _isNowPlayingVisible.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingVisible.value = false
    }

    private var pausedForVideo = false
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaSession: MediaSession? = null
    private var lastError: String? = null

    init {
        try {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            mediaSession = MediaSession.Builder(context, player)
                .setSessionActivity(pendingIntent)
                .build()
        } catch (e: Throwable) {
            android.util.Log.e("ApnaAudioPlayer", "Error creating MediaSession", e)
        }

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = publish()
            override fun onPlayerError(error: PlaybackException) {
                lastError = "Playback error: ${error.errorCodeName}"
                publish()
            }
        })

        scope.launch {
            while (true) {
                _position.value = player.currentPosition.coerceAtLeast(0L)
                delay(250)
            }
        }
    }

    fun getMediaSession(): MediaSession? = mediaSession

    // ---------------------------------------------------------------- playback API

    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        ensureService()
        lastError = null
        pausedForVideo = false
        player.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(songs.indices), C.TIME_UNSET)
        player.shuffleModeEnabled = shuffle
        player.prepare()
        player.play()
    }

    fun playStream(url: String, title: String, subtitle: String = "", live: Boolean) {
        ensureService()
        lastError = null
        pausedForVideo = false
        val extras = Bundle().apply { putBoolean(EXTRA_LIVE, live) }
        val item = MediaItem.Builder()
            .setMediaId(url)
            .setUri(url)
            .setCustomCacheKey(if (live) AudioCacheManager.LIVE_KEY_PREFIX + url else url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setExtras(extras)
                    .build()
            )
            .build()
        player.setMediaItem(item)
        player.shuffleModeEnabled = false
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
            player.play()
        }
    }

    fun pause() {
        player.pause()
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // ---------------------------------------------------------------- Dual mode (video <-> music)

    fun pauseForVideo() {
        if (player.isPlaying) {
            pausedForVideo = true
            player.pause()
        }
    }

    fun resumeAfterVideo() {
        if (pausedForVideo) {
            pausedForVideo = false
            player.play()
        }
    }

    // ---------------------------------------------------------------- internals

    private fun ensureService() {
        val existing = controllerFuture
        if (existing != null && existing.isDone) {
            val connected = runCatching { existing.get().isConnected }.getOrDefault(false)
            if (connected) return
        } else if (existing != null) {
            return
        }
        existing?.let { MediaController.releaseFuture(it) }
        val token = SessionToken(context, ComponentName(context, ApnaPlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync()
    }

    private fun publish() {
        val item = player.currentMediaItem
        val md = item?.mediaMetadata
        val extras = md?.extras
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: extras?.getLong(EXTRA_DURATION, 0L)
            ?: 0L
        if (!player.isPlaying) levelProcessor.level.value = 0f

        _state.value = PlayerState(
            hasMedia = item != null,
            mediaId = item?.mediaId,
            songUri = item?.localConfiguration?.uri,
            albumId = extras?.getLong(EXTRA_ALBUM_ID, 0L) ?: 0L,
            title = md?.title?.toString().orEmpty(),
            artist = md?.artist?.toString().orEmpty(),
            album = md?.albumTitle?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = duration,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            isLive = extras?.getBoolean(EXTRA_LIVE, false) == true,
            error = lastError
        )
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong(EXTRA_DURATION, durationMs)
            putLong(EXTRA_ALBUM_ID, albumId)
        }
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    companion object {
        const val EXTRA_LIVE = "apna_live"
        const val EXTRA_DURATION = "apna_duration"
        const val EXTRA_ALBUM_ID = "apna_album_id"

        @Volatile
        private var INSTANCE: ApnaAudioPlayerManager? = null

        fun getInstance(context: Context): ApnaAudioPlayerManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApnaAudioPlayerManager(
                    context.applicationContext,
                    AudioCacheManager.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
    }
}
