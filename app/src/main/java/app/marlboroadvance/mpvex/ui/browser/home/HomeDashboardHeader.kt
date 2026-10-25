/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package app.marlboroadvance.mpvex.ui.browser.home

import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import app.marlboroadvance.mpvex.utils.media.MediaIdentifier
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Aura Home Theme Palette Tokens
 */
private data class AuraHomePalette(
  val cardBg: Color,
  val cardBorder: Color,
  val textPrimary: Color,
  val textMuted: Color,
  val chipBg: Color,
  val accentColor: Color,
  val accentGlow: Color,
  val isDark: Boolean,
)

@Composable
private fun rememberAuraHomePalette(): AuraHomePalette {
  val cs = MaterialTheme.colorScheme
  val isDark = cs.background.luminance() < 0.5f
  return remember(isDark, cs) {
    if (isDark) {
      AuraHomePalette(
        cardBg = AuraTokens.CarbonElevated,
        cardBorder = AuraTokens.BorderSubtle,
        textPrimary = AuraTokens.TextPrimary,
        textMuted = AuraTokens.TextSecondary,
        chipBg = AuraTokens.SlateSurface,
        accentColor = AuraTokens.AuraCyan,
        accentGlow = AuraTokens.AuraCyanGlow,
        isDark = true,
      )
    } else {
      AuraHomePalette(
        cardBg = AuraTokens.PureWhite,
        cardBorder = cs.outlineVariant.copy(alpha = 0.45f),
        textPrimary = cs.onSurface,
        textMuted = cs.onSurfaceVariant,
        chipBg = cs.surfaceContainerHighest.copy(alpha = 0.7f),
        accentColor = cs.primary,
        accentGlow = cs.primary.copy(alpha = 0.2f),
        isDark = false,
      )
    }
  }
}

/**
 * Aura Player Home Dashboard Header
 * Completely redesigned original experience featuring:
 * 1. Hero Continue Watching Card with ambient glowing accents, video progress, metadata, and quick resume.
 * 2. Aura 2x3 Quick Action Grid (All Media, Directories, Favorites, Playlists, Stream URL, Downloads).
 * 3. Aura Recently Played Carousel with high-fidelity video cards.
 * 4. Storage Directories Ribbon Header with count badges, sort chips, and responsive layout toggle.
 */
@Composable
fun HomeDashboardHeader(
  folders: List<VideoFolder>,
  pinnedCount: Int = 0,
  mediaLayoutMode: MediaLayoutMode = MediaLayoutMode.GRID,
  sortType: FolderSortType = FolderSortType.Title,
  sortOrder: SortOrder = SortOrder.Ascending,
  onSortClick: () -> Unit = {},
  onToggleLayoutMode: () -> Unit = {},
  onNavigateToFavorites: () -> Unit = {},
  onNavigateToPlaylists: () -> Unit = {},
  onStreamClick: () -> Unit = {},
  onDownloadsClick: () -> Unit = {},
  onAllVideosClick: () -> Unit = {},
  onScrollToFolders: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val recentlyPlayedRepo = koinInject<RecentlyPlayedRepository>()
  val playbackStateRepo = koinInject<PlaybackStateRepository>()
  val playlistRepo = koinInject<PlaylistRepository>()
  val thumbnailRepo = koinInject<ThumbnailRepository>()
  val palette = rememberAuraHomePalette()

  val recentVideos by recentlyPlayedRepo
    .observeRecentlyPlayed(limit = 10)
    .collectAsState(initial = emptyList())

  val playlists by playlistRepo
    .observeAllPlaylists()
    .collectAsState(initial = emptyList())

  val heroVideo = recentVideos.firstOrNull()

  // Real aggregate statistics
  val totalVideosCount = remember(folders) {
    folders.sumOf { it.videoCount }
  }
  val totalVideosSizeBytes = remember(folders) {
    folders.sumOf { it.totalSize }
  }
  val totalFoldersCount = folders.size

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Scoped Storage Telemetry Bar (From Stitch Design)
    AuraStorageTelemetryCard(
      totalVideosCount = totalVideosCount,
      totalVideosSizeBytes = totalVideosSizeBytes,
      onRescan = onDownloadsClick,
      palette = palette,
    )

    // 2. Hero Continue Watching Card
    if (heroVideo != null) {
      AuraContinueWatchingHero(
        video = heroVideo,
        playbackStateRepo = playbackStateRepo,
        thumbnailRepo = thumbnailRepo,
        onPlay = { MediaUtils.playFile(heroVideo.filePath, context, "home_continue_watching") },
        palette = palette,
      )
    }

    // 3. Aura Quick Action Grid
    AuraQuickAccessGrid(
      totalVideosCount = totalVideosCount,
      totalFoldersCount = totalFoldersCount,
      recentCount = recentVideos.size,
      pinnedCount = pinnedCount,
      onAllVideosClick = onAllVideosClick,
      onFoldersClick = { onScrollToFolders?.invoke() },
      onFavoritesClick = onNavigateToFavorites,
      onPlaylistsClick = onNavigateToPlaylists,
      palette = palette,
    )

    // 4. Fast Ingestion & Network Strip (Connected to Room DB Playlists)
    AuraFastIngestionStrip(
      playlistCount = playlists.size,
      onStreamClick = onStreamClick,
      onPlaylistsClick = onNavigateToPlaylists,
      palette = palette,
    )

    // 5. Recently Played Carousel
    if (recentVideos.size > 1) {
      AuraRecentlyPlayedCarousel(
        videos = recentVideos.drop(1),
        thumbnailRepo = thumbnailRepo,
        playbackStateRepo = playbackStateRepo,
        onVideoClick = { entity ->
          MediaUtils.playFile(entity.filePath, context, "home_recent_carousel")
        },
        palette = palette,
      )
    }

    // 6. Storage Directories Ribbon Header
    AuraStorageSectionRibbon(
      folderCount = totalFoldersCount,
      sortType = sortType,
      sortOrder = sortOrder,
      mediaLayoutMode = mediaLayoutMode,
      onSortClick = onSortClick,
      onToggleLayout = onToggleLayoutMode,
      palette = palette,
    )
  }
}

/**
 * Aura Hero Continue Watching Card
 */
@Composable
private fun AuraContinueWatchingHero(
  video: RecentlyPlayedEntity,
  playbackStateRepo: PlaybackStateRepository,
  thumbnailRepo: ThumbnailRepository,
  onPlay: () -> Unit,
  palette: AuraHomePalette,
) {
  val resolvedVideo = remember(video.filePath, video.timestamp) { video.toVideo() }
  var thumbnailBitmap by remember(video.filePath) {
    mutableStateOf(
      runCatching {
        thumbnailRepo.getThumbnailFromMemory(resolvedVideo, widthPx = 360, heightPx = 200)
      }.getOrNull(),
    )
  }
  var progressFraction by remember(video.filePath) { mutableStateOf(0f) }

  LaunchedEffect(video.filePath, video.duration) {
    withContext(Dispatchers.IO) {
      runCatching {
        val mediaId = MediaIdentifier.forLocalPath(video.filePath)
        val state = playbackStateRepo.getVideoDataByTitle(mediaId)
        val durSec = video.duration / 1000L
        if (state != null && durSec > 0L) {
          val remaining = state.timeRemaining.toLong()
          val watched = durSec - remaining
          progressFraction = (watched.toFloat() / durSec.toFloat()).coerceIn(0.01f, 0.99f)
        }
      }
      if (thumbnailBitmap == null) {
        val bmp = runCatching {
          thumbnailRepo.getCachedThumbnail(resolvedVideo, widthPx = 360, heightPx = 200)
            ?: thumbnailRepo.getThumbnail(resolvedVideo, widthPx = 360, heightPx = 200)
        }.getOrNull()
        if (bmp != null) {
          withContext(Dispatchers.Main) {
            thumbnailBitmap = bmp
          }
        }
      }
    }
  }

  Card(
    shape = AuraTokens.ShapeCardLarge,
    colors = CardDefaults.cardColors(containerColor = palette.cardBg),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(AuraTokens.ShapeCardLarge)
      .clickable(onClick = onPlay),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Thumbnail Box
        Box(
          modifier = Modifier
            .width(116.dp)
            .aspectRatio(16f / 10f)
            .clip(AuraTokens.ShapeThumbnail)
            .background(palette.chipBg),
          contentAlignment = Alignment.Center,
        ) {
          val bmp = thumbnailBitmap
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(
              Icons.Filled.Movie,
              contentDescription = null,
              tint = palette.accentColor.copy(alpha = 0.6f),
              modifier = Modifier.size(28.dp),
            )
          }
          if (video.duration > 0) {
            Surface(
              shape = RoundedCornerShape(5.dp),
              color = Color.Black.copy(alpha = 0.8f),
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
            ) {
              Text(
                text = formatDuration(video.duration),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
              )
            }
          }
        }

        // Information Column
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = palette.accentColor.copy(alpha = 0.18f),
            ) {
              Text(
                text = "CONTINUE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentColor,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
            val ext = video.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
            if (ext.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = palette.chipBg,
              ) {
                Text(
                  text = ext,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = palette.textMuted,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = video.videoTitle ?: video.fileName,
            color = palette.textPrimary,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val sizeStr = if (video.fileSize > 0) formatFileSize(video.fileSize) else ""
            Text(
              text = sizeStr,
              color = palette.textMuted,
              style = MaterialTheme.typography.labelSmall,
            )

            // Tactile Resume Button with Aura accent
            Surface(
              shape = AuraTokens.ShapePill,
              color = palette.accentColor,
              modifier = Modifier.clickable(onClick = onPlay),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Filled.PlayArrow,
                  contentDescription = null,
                  tint = if (palette.isDark) Color.Black else Color.White,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Resume",
                  color = if (palette.isDark) Color.Black else Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      // Sleek Progress Bar with subtle glowing gradient
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(CircleShape)
          .background(palette.chipBg),
      ) {
        if (progressFraction > 0f) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progressFraction)
              .height(4.dp)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  listOf(palette.accentColor, Color(0xFF38BDF8)),
                ),
              ),
          )
        }
      }
    }
  }
}

/**
 * Aura Quick Access Grid (2 columns x 2 rows + quick utility pills)
 * Matches Reference Image 1 "nPlayer Pro" aesthetic
 */
@Composable
private fun AuraQuickAccessGrid(
  totalVideosCount: Int,
  totalFoldersCount: Int,
  recentCount: Int,
  pinnedCount: Int,
  onAllVideosClick: () -> Unit,
  onFoldersClick: () -> Unit,
  onFavoritesClick: () -> Unit,
  onPlaylistsClick: () -> Unit,
  palette: AuraHomePalette,
) {
  val pinkAccent = Color(0xFFFF3B7A)
  val goldAccent = Color(0xFFFFB300)
  val blueAccent = Color(0xFF2979FF)
  val orangeAccent = Color(0xFFFF6D00)

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Row 1: Recent | Favorite
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AuraQuickActionCard(
        title = "Recent",
        subtitle = if (recentCount > 0) "$recentCount Files" else "History",
        icon = Icons.Filled.History,
        iconTint = pinkAccent,
        onClick = onAllVideosClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )
      AuraQuickActionCard(
        title = "Favorite",
        subtitle = "$pinnedCount Files",
        icon = Icons.Filled.Star,
        iconTint = goldAccent,
        onClick = onFavoritesClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )
    }

    // Row 2: Audio | Video
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      AuraQuickActionCard(
        title = "Audio",
        subtitle = "Music Library",
        icon = Icons.Filled.Audiotrack,
        iconTint = blueAccent,
        onClick = onPlaylistsClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )
      AuraQuickActionCard(
        title = "Video",
        subtitle = "$totalVideosCount Files",
        icon = Icons.Filled.Movie,
        iconTint = orangeAccent,
        onClick = onFoldersClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/**
 * Aura Scoped Storage & Indexing Telemetry Card (From Stitch Design)
 */
@Composable
private fun AuraStorageTelemetryCard(
  totalVideosCount: Int,
  totalVideosSizeBytes: Long,
  onRescan: () -> Unit,
  palette: AuraHomePalette,
) {
  val statFs = remember {
    runCatching {
      StatFs(Environment.getExternalStorageDirectory().path)
    }.getOrNull()
  }
  val totalStorageBytes = statFs?.totalBytes ?: (256L * 1024L * 1024L * 1024L)
  val availableStorageBytes = statFs?.availableBytes ?: (72L * 1024L * 1024L * 1024L)
  val usedStorageBytes = (totalStorageBytes - availableStorageBytes).coerceAtLeast(0L)
  val usedGb = "%.1f".format(Locale.US, usedStorageBytes / (1024.0 * 1024.0 * 1024.0))
  val totalGb = "%.0f".format(Locale.US, totalStorageBytes / (1024.0 * 1024.0 * 1024.0))

  val videoFraction = if (totalStorageBytes > 0L) {
    (totalVideosSizeBytes.toFloat() / totalStorageBytes.toFloat()).coerceIn(0.05f, 0.65f)
  } else 0.40f

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = palette.cardBg),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(palette.accentColor)
          )
          Text(
            text = "Internal Scoped Storage",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = palette.textPrimary,
          )
          Surface(
            shape = RoundedCornerShape(50),
            color = palette.chipBg,
          ) {
            Text(
              text = "UFS 3.1",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = palette.accentColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = "$usedGb GB",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = palette.textPrimary,
          )
          Text(
            text = "/ $totalGb GB",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
          )
        }
      }

      // Multi-segment progress visualizer
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(CircleShape)
          .background(palette.chipBg),
      ) {
        Row(modifier = Modifier.fillMaxSize()) {
          Box(
            modifier = Modifier
              .fillMaxWidth(videoFraction)
              .fillMaxHeight()
              .background(palette.accentColor)
          )
          Box(
            modifier = Modifier
              .fillMaxWidth(0.18f)
              .fillMaxHeight()
              .background(Color(0xFFFFB300))
          )
          Box(
            modifier = Modifier
              .fillMaxWidth(0.10f)
              .fillMaxHeight()
              .background(Color(0xFF7C4DFF))
          )
        }
      }

      // Footer stats and rescan
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(palette.accentColor)
          )
          Text(
            text = "$totalVideosCount Videos (${formatFileSize(totalVideosSizeBytes)})",
            fontSize = 11.sp,
            color = palette.textMuted,
          )
        }

        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onRescan)
            .padding(horizontal = 6.dp, vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Text(
            text = "Rescan",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentColor,
          )
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Rescan",
            tint = palette.accentColor,
            modifier = Modifier.size(13.dp),
          )
        }
      }
    }
  }
}

/**
 * Fast Ingestion & Network Strip (From Stitch Design)
 */
@Composable
private fun AuraFastIngestionStrip(
  playlistCount: Int,
  onStreamClick: () -> Unit,
  onPlaylistsClick: () -> Unit,
  palette: AuraHomePalette,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "FAST INGESTION & NETWORK",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.6.sp,
        ),
        color = palette.textPrimary,
      )
      Text(
        text = "AURA MEDIA ENGINE",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = palette.textMuted,
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // 1. Stream Link
      AuraIngestionTile(
        title = "Stream Link",
        subtitle = "M3U8 / Direct URL",
        icon = Icons.Filled.PlayCircle,
        iconTint = Color(0xFF00E676),
        onClick = onStreamClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )

      // 2. Network NAS
      AuraIngestionTile(
        title = "Network NAS",
        subtitle = "SMB / FTP / WebDAV",
        icon = Icons.Filled.Storage,
        iconTint = Color(0xFFFFB300),
        onClick = onStreamClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )

      // 3. Playlists (Room DB Connected)
      AuraIngestionTile(
        title = "Playlists",
        subtitle = if (playlistCount > 0) "$playlistCount Playlists" else "Create Mix",
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        iconTint = Color(0xFF38BDF8),
        onClick = onPlaylistsClick,
        palette = palette,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun AuraIngestionTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  palette: AuraHomePalette,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = palette.cardBg,
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = iconTint.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
        color = palette.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AuraQuickActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  palette: AuraHomePalette,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = palette.cardBg),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = iconTint.copy(alpha = if (palette.isDark) 0.18f else 0.12f),
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = palette.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/**
 * Aura Recently Played Carousel
 */
@Composable
private fun AuraRecentlyPlayedCarousel(
  videos: List<RecentlyPlayedEntity>,
  thumbnailRepo: ThumbnailRepository,
  playbackStateRepo: PlaybackStateRepository,
  onVideoClick: (RecentlyPlayedEntity) -> Unit,
  palette: AuraHomePalette,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          Icons.Filled.History,
          contentDescription = null,
          tint = palette.accentColor,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "RECENTLY PLAYED",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
          ),
          color = palette.textPrimary,
        )
      }
      Text(
        text = "${videos.size} ITEMS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
        ),
        color = palette.textMuted,
      )
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(videos, key = { it.filePath }) { video ->
        AuraRecentVideoCard(
          video = video,
          thumbnailRepo = thumbnailRepo,
          playbackStateRepo = playbackStateRepo,
          onClick = { onVideoClick(video) },
          palette = palette,
        )
      }
    }
  }
}

@Composable
private fun AuraRecentVideoCard(
  video: RecentlyPlayedEntity,
  thumbnailRepo: ThumbnailRepository,
  playbackStateRepo: PlaybackStateRepository,
  onClick: () -> Unit,
  palette: AuraHomePalette,
) {
  val resolvedVideo = remember(video.filePath, video.timestamp) { video.toVideo() }
  var thumbnailBitmap by remember(video.filePath) {
    mutableStateOf(
      runCatching {
        thumbnailRepo.getThumbnailFromMemory(resolvedVideo, widthPx = 300, heightPx = 170)
      }.getOrNull(),
    )
  }
  var progressFraction by remember(video.filePath) { mutableStateOf(0f) }

  LaunchedEffect(video.filePath, video.duration) {
    withContext(Dispatchers.IO) {
      runCatching {
        val mediaId = MediaIdentifier.forLocalPath(video.filePath)
        val state = playbackStateRepo.getVideoDataByTitle(mediaId)
        val durSec = video.duration / 1000L
        if (state != null && durSec > 0L) {
          val remaining = state.timeRemaining.toLong()
          val watched = durSec - remaining
          progressFraction = (watched.toFloat() / durSec.toFloat()).coerceIn(0.01f, 0.99f)
        }
      }
      if (thumbnailBitmap == null) {
        val bmp = runCatching {
          thumbnailRepo.getCachedThumbnail(resolvedVideo, widthPx = 300, heightPx = 170)
            ?: thumbnailRepo.getThumbnail(resolvedVideo, widthPx = 300, heightPx = 170)
        }.getOrNull()
        if (bmp != null) {
          withContext(Dispatchers.Main) {
            thumbnailBitmap = bmp
          }
        }
      }
    }
  }

  Card(
    shape = AuraTokens.ShapeThumbnail,
    colors = CardDefaults.cardColors(containerColor = palette.cardBg),
    border = BorderStroke(1.dp, palette.cardBorder),
    modifier = Modifier
      .width(170.dp)
      .clip(AuraTokens.ShapeThumbnail)
      .clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9.5f)
          .clip(RoundedCornerShape(9.dp))
          .background(palette.chipBg),
        contentAlignment = Alignment.Center,
      ) {
        val bmp = thumbnailBitmap
        if (bmp != null) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Icon(
            Icons.Filled.Movie,
            contentDescription = null,
            tint = palette.accentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp),
          )
        }

        // Timecode badge
        if (video.duration > 0) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(4.dp),
          ) {
            Text(
              text = formatDuration(video.duration),
              fontSize = 8.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }

        // Bottom progress bar
        if (progressFraction > 0f) {
          Box(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .fillMaxWidth(progressFraction)
              .height(3.dp)
              .background(palette.accentColor),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = video.videoTitle ?: video.fileName,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = palette.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      val ext = video.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
      Text(
        text = ext.ifBlank { "MEDIA" },
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = palette.textMuted,
        maxLines = 1,
      )
    }
  }
}

/**
 * Storage Section Ribbon Header
 */
@Composable
private fun AuraStorageSectionRibbon(
  folderCount: Int,
  sortType: FolderSortType,
  sortOrder: SortOrder,
  mediaLayoutMode: MediaLayoutMode,
  onSortClick: () -> Unit,
  onToggleLayout: () -> Unit,
  palette: AuraHomePalette,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 6.dp, start = 2.dp, end = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        Icons.Filled.Folder,
        contentDescription = null,
        tint = palette.accentColor,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "FREQUENTLY USED",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.6.sp,
        ),
        color = palette.textPrimary,
      )
      Surface(
        shape = AuraTokens.ShapeChip,
        color = palette.accentColor.copy(alpha = 0.18f),
      ) {
        Text(
          text = "$folderCount",
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          color = palette.accentColor,
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
        )
      }
    }

    // Active Sort & View Mode Quick Toggle
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      // Sort Chip
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.clickable(onClick = onSortClick),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            if (sortOrder.isAscending) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = palette.accentColor,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = sortType.displayName.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
          )
        }
      }

      // View Mode Toggle Button (List vs Grid)
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.cardBg,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = Modifier.clickable(onClick = onToggleLayout),
      ) {
        Box(
          modifier = Modifier.padding(6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            if (mediaLayoutMode == MediaLayoutMode.GRID) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
            contentDescription = "Toggle View",
            tint = palette.textPrimary,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }
  }
}

private fun RecentlyPlayedEntity.toVideo(): Video {
  val file = File(filePath)
  val isLocal = filePath.startsWith("/") || filePath.startsWith("file://")
  val uri = if (isLocal) {
    Uri.fromFile(file)
  } else {
    runCatching { Uri.parse(filePath) }.getOrDefault(Uri.EMPTY)
  }
  val name = file.name.ifBlank { fileName.ifBlank { "Video" } }
  val title = videoTitle?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension.ifBlank { name }
  val size = if (fileSize > 0) fileSize else if (isLocal && file.exists()) file.length() else 0L

  return Video(
    id = file.absolutePath.hashCode().toLong(),
    title = title,
    displayName = name,
    path = filePath,
    uri = uri,
    duration = duration,
    durationFormatted = formatDuration(duration),
    size = size,
    sizeFormatted = formatFileSize(size),
    dateModified = timestamp,
    dateAdded = timestamp,
    mimeType = "video/*",
    bucketId = (file.parent?.hashCode() ?: (uri.host ?: "local").hashCode()).toString(),
    bucketDisplayName = file.parentFile?.name ?: "",
    width = width,
    height = height,
    fps = 0f,
    resolution = if (width > 0 && height > 0) "${width}x${height}" else "",
  )
}

private fun formatDuration(durationMs: Long): String {
  val seconds = durationMs / 1000
  val hours = seconds / 3600
  val minutes = (seconds % 3600) / 60
  val secs = seconds % 60

  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m"
    else -> "${secs}s"
  }
}

private fun formatFileSize(bytes: Long): String =
  when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
  }
