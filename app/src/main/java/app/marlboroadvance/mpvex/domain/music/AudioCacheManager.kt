package app.marlboroadvance.mpvex.domain.music

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * 200MB LRU disk cache (Media3 SimpleCache).
 * - Online file streams (mp3 URL etc.) are cached.
 * - Live radio / HLS / local files bypass cache.
 */
@UnstableApi
class AudioCacheManager(private val context: Context) {

    val cache: SimpleCache by lazy {
        SimpleCache(
            File(context.cacheDir, "audio_stream_cache"),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
            StandaloneDatabaseProvider(context)
        )
    }

    fun dataSourceFactory(): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("ApnaPlayer/1.3")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        val upstream = DefaultDataSource.Factory(context, http)

        val cachedFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return DataSource.Factory {
            RoutingDataSource(cachedFactory.createDataSource(), upstream.createDataSource())
        }
    }

    fun release() {
        try {
            cache.release()
        } catch (_: Exception) {}
    }

    private class RoutingDataSource(
        private val cached: DataSource,
        private val plain: DataSource
    ) : DataSource {
        private var active: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            cached.addTransferListener(transferListener)
            plain.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val target = if (shouldCache(dataSpec)) cached else plain
            active = target
            return target.open(dataSpec)
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
            active?.read(buffer, offset, length) ?: -1

        override fun getUri(): Uri? = active?.uri

        override fun getResponseHeaders(): Map<String, List<String>> =
            active?.responseHeaders ?: emptyMap()

        override fun close() {
            try {
                active?.close()
            } finally {
                active = null
            }
        }

        private fun shouldCache(spec: DataSpec): Boolean {
            val scheme = spec.uri.scheme
            if (scheme != "http" && scheme != "https") return false
            if (spec.key?.startsWith(LIVE_KEY_PREFIX) == true) return false
            val path = spec.uri.path.orEmpty().lowercase()
            return !(path.endsWith(".m3u8") || path.endsWith(".mpd"))
        }
    }

    companion object {
        const val MAX_CACHE_BYTES = 200L * 1024 * 1024
        const val LIVE_KEY_PREFIX = "live:"

        @Volatile
        private var INSTANCE: AudioCacheManager? = null

        fun getInstance(context: Context): AudioCacheManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioCacheManager(context.applicationContext).also { INSTANCE = it }
            }

        fun getCacheDataSourceFactory(context: Context): DataSource.Factory =
            getInstance(context).dataSourceFactory()
    }
}
