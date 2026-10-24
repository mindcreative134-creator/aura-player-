package app.marlboroadvance.mpvex.di

import app.marlboroadvance.mpvex.domain.lyrics.LrcLibLyricsRepository
import app.marlboroadvance.mpvex.domain.music.AlbumArtCache
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager
import app.marlboroadvance.mpvex.domain.music.AudioCacheManager
import app.marlboroadvance.mpvex.domain.music.LocalMusicRepository
import app.marlboroadvance.mpvex.domain.music.PlaylistStore
import app.marlboroadvance.mpvex.domain.radio.RadioBrowserRepository
import app.marlboroadvance.mpvex.ui.browser.music.MusicViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Koin module for Apna Player Music System */
val musicModule = module {
    single { AudioCacheManager.getInstance(androidContext()) }
    single { ApnaAudioPlayerManager.getInstance(androidContext()) }
    single { LocalMusicRepository(androidContext()) }
    single { AlbumArtCache(androidContext()) }
    single { PlaylistStore(androidContext()) }
    single { LrcLibLyricsRepository() }
    single { RadioBrowserRepository() }
    single { MusicViewModel.create(androidContext()) }
}
