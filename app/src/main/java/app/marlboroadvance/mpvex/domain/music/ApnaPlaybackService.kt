package app.marlboroadvance.mpvex.domain.music

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background playback + lockscreen/notification controls.
 * Binds to ApnaAudioPlayerManager's player and session.
 */
@UnstableApi
class ApnaPlaybackService : MediaSessionService() {

    private val playerManager: ApnaAudioPlayerManager by lazy {
        ApnaAudioPlayerManager.getInstance(applicationContext)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        playerManager.getMediaSession()

    /**
     * When swiped away from Recents:
     * If music is actively playing, keep foreground service running.
     * Otherwise stop the service.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = playerManager.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            pauseAllPlayersAndStopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
