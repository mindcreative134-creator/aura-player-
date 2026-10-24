/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package app.marlboroadvance.mpvex.ui.browser.home

import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
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
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.FolderSortType
import app.marlboroadvance.mpvex.preferences.MediaLayoutMode
import app.marlboroadvance.mpvex.preferences.SortOrder
import app.marlboroadvance.mpvex.utils.media.MediaIdentifier
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

// NextPlayer Design System Palette Tokens
private val NextBlue = Color(0xFF0D59F2)
private val NextBlueLight = Color(0xFFEBF2FE)
private val NextBluePill = Color(0xFFE0EDFF)
private val NextPurpleBg = Color(0xFFF5F3FF)
private val NextPurpleIcon = Color(0xFF7C3AED)
private val NextGreenBg = Color(0xFFDCFCE7)
private val NextGreenIcon = Color(0xFF16A34A)

private data class NextHeaderTheme(
  val card: Color,
  val border: Color,
  val textPrimary: Color,
  val textMuted: Color,
  val chipBg: Color,
  val isDark: Boolean,
)

@Composable
private fun rememberNextHeaderTheme(): NextHeaderTheme {
  val cs = MaterialTheme.colorScheme
  val isDark = cs.background.luminance() < 0.5f
  return remember(isDark, cs) {
    if (isDark) {
      NextHeaderTheme(
        card = cs.surfaceContainer,
        border = cs.outlineVariant.copy(alpha = 0.35f),
        textPrimary = cs.onSurface,
        textMuted = cs.onSurfaceVariant,
        chipBg = cs.surfaceContainerHigh,
        isDark = true,
      )
    } else {
      NextHeaderTheme(
        card = cs.surfaceContainerLow,
        border = cs.outlineVariant.copy(alpha = 0.5f),
        textPrimary = cs.onSurface,
        textMuted = cs.onSurfaceVariant,
        chipBg = cs.surfaceContainerHighest,
        isDark = false,
      )
    }
  }
}

/**
 * Next Player style Home Dashboard Header.
 * Features:
 * 1. Hero Continue Watching Card (with real thumbnail, HW+ badge, duration, file size, progress bar, and Resume button).
 * 2. 2x2 Quick Access Grid (All Videos, Folders, Favorites, Playlists) + Online Stream full-width card.
 * 3. Horizontal Recently Played Carousel with real thumbnails and playback progress.
 * 4. Storage Directories Ribbon Header (folder count badge, sort chip, and layout toggle button).
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
  val thumbnailRepo = koinInject<ThumbnailRepository>()
  val colors = rememberNextHeaderTheme()

  val recentVideos by recentlyPlayedRepo
    .observeRecentlyPlayed(limit = 10)
    .collectAsState(initial = emptyList())

  val heroVideo = recentVideos.firstOrNull()

  // Calculate real aggregate metrics
  val totalVideosCount = remember(folders) {
    folders.sumOf { it.videoCount }
  }
  val totalFoldersCount = folders.size

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Continue Watching Hero Banner (Real video from recently played)
    if (heroVideo != null) {
      NextContinueWatchingCard(
        video = heroVideo,
        playbackStateRepo = playbackStateRepo,
        thumbnailRepo = thumbnailRepo,
        onPlay = { MediaUtils.playFile(heroVideo.filePath, context, "home_continue_watching") },
        colors = colors,
      )
    }

    // 2. Quick Access 2x2 Grid + Online Stream & Downloads
    NextQuickAccessGrid(
      totalVideosCount = totalVideosCount,
      totalFoldersCount = totalFoldersCount,
      pinnedCount = pinnedCount,
      onAllVideosClick = onAllVideosClick,
      onFoldersClick = {
        onScrollToFolders?.invoke()
      },
      onFavoritesClick = onNavigateToFavorites,
      onPlaylistsClick = onNavigateToPlaylists,
      onStreamClick = onStreamClick,
      onDownloadsClick = onDownloadsClick,
      colors = colors,
    )

    // 3. Recently Played Carousel (if more than 1 recent item)
    if (recentVideos.size > 1) {
      NextRecentlyPlayedCarousel(
        videos = recentVideos.drop(1),
        thumbnailRepo = thumbnailRepo,
        playbackStateRepo = playbackStateRepo,
        onVideoClick = { entity ->
          MediaUtils.playFile(entity.filePath, context, "home_recent_carousel")
        },
        colors = colors,
      )
    }

    // 4. Storage Section Ribbon Header
    NextStorageSectionRibbon(
      folderCount = totalFoldersCount,
      sortType = sortType,
      sortOrder = sortOrder,
      mediaLayoutMode = mediaLayoutMode,
      onSortClick = onSortClick,
      onToggleLayout = onToggleLayoutMode,
      colors = colors,
    )
  }
}

/**
 * Continue Watching Hero Card:
 * Real video from RecentlyPlayedRepository with real thumbnail, progress bar, duration, HW+ badge, and Resume button.
 */
@Composable
private fun NextContinueWatchingCard(
  video: RecentlyPlayedEntity,
  playbackStateRepo: PlaybackStateRepository,
  thumbnailRepo: ThumbnailRepository,
  onPlay: () -> Unit,
  colors: NextHeaderTheme,
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
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = colors.card),
    border = BorderStroke(1.dp, colors.border),
    modifier = Modifier
      .fillMaxWidth()
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
            .width(112.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.chipBg),
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
              tint = NextBlue.copy(alpha = 0.6f),
              modifier = Modifier.size(28.dp),
            )
          }
          if (video.duration > 0) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color.Black.copy(alpha = 0.75f),
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = if (colors.isDark) NextBlue.copy(alpha = 0.2f) else NextBlueLight,
            ) {
              Text(
                text = "HW+",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (colors.isDark) Color(0xFF60A5FA) else NextBlue,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
              )
            }
            val ext = video.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
            if (ext.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.chipBg,
              ) {
                Text(
                  text = ext,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = colors.textMuted,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = video.videoTitle ?: video.fileName,
            color = colors.textPrimary,
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
              color = colors.textMuted,
              style = MaterialTheme.typography.labelSmall,
            )

            // Resume Pill Button
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = NextBlue,
              modifier = Modifier.clickable(onClick = onPlay),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  Icons.Filled.PlayArrow,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "Resume »",
                  color = Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      }

      // Real Progress Bar
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(CircleShape)
          .background(colors.chipBg),
      ) {
        if (progressFraction > 0f) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progressFraction)
              .height(4.dp)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  listOf(NextBlue, Color(0xFF38BDF8)),
                ),
              ),
          )
        }
      }
    }
  }
}

/**
 * Quick Access 2x2 Feature Grid + Online Stream Card
 */
@Composable
private fun NextQuickAccessGrid(
  totalVideosCount: Int,
  totalFoldersCount: Int,
  pinnedCount: Int,
  onAllVideosClick: () -> Unit,
  onFoldersClick: () -> Unit,
  onFavoritesClick: () -> Unit,
  onPlaylistsClick: () -> Unit,
  onStreamClick: () -> Unit,
  onDownloadsClick: () -> Unit,
  colors: NextHeaderTheme,
) {
  val isDark = colors.isDark
  val blueContainer = if (isDark) NextBlue.copy(alpha = 0.2f) else NextBluePill
  val indigoContainer = if (isDark) Color(0xFF4F46E5).copy(alpha = 0.2f) else Color(0xFFEEF2FF)
  val purpleContainer = if (isDark) NextPurpleIcon.copy(alpha = 0.2f) else NextPurpleBg
  val skyContainer = if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE)
  val greenContainer = if (isDark) NextGreenIcon.copy(alpha = 0.2f) else NextGreenBg
  val amberContainer = if (isDark) Color(0xFFFBBF24).copy(alpha = 0.2f) else Color(0xFFFEF3C7)

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Row 1: All Videos | Folders
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      NextQuickPillCard(
        title = "All Videos",
        subtitle = "$totalVideosCount files",
        icon = Icons.Filled.VideoLibrary,
        iconTint = if (isDark) Color(0xFF60A5FA) else NextBlue,
        containerTint = blueContainer,
        onClick = onAllVideosClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
      NextQuickPillCard(
        title = "Folders",
        subtitle = "$totalFoldersCount dirs",
        icon = Icons.Filled.Folder,
        iconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
        containerTint = indigoContainer,
        onClick = onFoldersClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
    }

    // Row 2: Favorites | Playlists
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      NextQuickPillCard(
        title = "Favorites",
        subtitle = "$pinnedCount pinned",
        icon = Icons.Filled.Star,
        iconTint = if (isDark) Color(0xFFA78BFA) else NextPurpleIcon,
        containerTint = purpleContainer,
        onClick = onFavoritesClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
      NextQuickPillCard(
        title = "Playlists",
        subtitle = "Custom queues",
        icon = Icons.Filled.QueueMusic,
        iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
        containerTint = skyContainer,
        onClick = onPlaylistsClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
    }

    // Row 3: Online Stream | Downloads side-by-side
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      NextQuickPillCard(
        title = "Online Stream",
        subtitle = "IPTV & URLs",
        icon = Icons.Filled.PlayCircle,
        iconTint = if (isDark) Color(0xFF4ADE80) else NextGreenIcon,
        containerTint = greenContainer,
        onClick = onStreamClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
      NextQuickPillCard(
        title = "Downloads",
        subtitle = "Offline manager",
        icon = Icons.Outlined.FileDownload,
        iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
        containerTint = amberContainer,
        onClick = onDownloadsClick,
        colors = colors,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun NextQuickPillCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  containerTint: Color,
  onClick: () -> Unit,
  colors: NextHeaderTheme,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = colors.card),
    border = BorderStroke(1.dp, colors.border),
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = containerTint,
          modifier = Modifier.size(42.dp),
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
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = null,
        tint = colors.textMuted.copy(alpha = 0.6f),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

/**
 * Recently Played Carousel:
 * Real carousel showing subsequent recently played videos with real thumbnails, HW+ badge, duration, and progress bar.
 */
@Composable
private fun NextRecentlyPlayedCarousel(
  videos: List<RecentlyPlayedEntity>,
  thumbnailRepo: ThumbnailRepository,
  playbackStateRepo: PlaybackStateRepository,
  onVideoClick: (RecentlyPlayedEntity) -> Unit,
  colors: NextHeaderTheme,
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
          tint = NextBlue,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "RECENTLY PLAYED",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
          ),
          color = colors.textPrimary,
        )
      }
      Text(
        text = "${videos.size} ITEMS",
        style = MaterialTheme.typography.labelSmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold,
        ),
        color = colors.textMuted,
      )
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      contentPadding = PaddingValues(horizontal = 2.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(videos, key = { it.filePath }) { video ->
        NextRecentVideoCard(
          video = video,
          thumbnailRepo = thumbnailRepo,
          playbackStateRepo = playbackStateRepo,
          onClick = { onVideoClick(video) },
          colors = colors,
        )
      }
    }
  }
}

@Composable
private fun NextRecentVideoCard(
  video: RecentlyPlayedEntity,
  thumbnailRepo: ThumbnailRepository,
  playbackStateRepo: PlaybackStateRepository,
  onClick: () -> Unit,
  colors: NextHeaderTheme,
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
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = colors.card),
    border = BorderStroke(1.dp, colors.border),
    modifier = Modifier
      .width(170.dp)
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick),
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9.5f)
          .clip(RoundedCornerShape(10.dp))
          .background(colors.chipBg),
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
            tint = NextBlue.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp),
          )
        }

        // HW+ chip at top-left
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color.Black.copy(alpha = 0.75f),
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp),
        ) {
          Text(
            text = "HW+",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }

        // Timecode badge
        if (video.duration > 0) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.75f),
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
              .background(NextBlue),
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = video.videoTitle ?: video.fileName,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      val ext = video.fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())
      Text(
        text = ext.ifBlank { "VIDEO" },
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = colors.textMuted,
        maxLines = 1,
      )
    }
  }
}

/**
 * Storage Section Ribbon Header:
 * [Folder Icon] STORAGE DIRECTORIES [Count]       ^ TITLE   [::]
 */
@Composable
private fun NextStorageSectionRibbon(
  folderCount: Int,
  sortType: FolderSortType,
  sortOrder: SortOrder,
  mediaLayoutMode: MediaLayoutMode,
  onSortClick: () -> Unit,
  onToggleLayout: () -> Unit,
  colors: NextHeaderTheme,
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
        tint = colors.textPrimary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "STORAGE DIRECTORIES",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.6.sp,
        ),
        color = colors.textPrimary,
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (colors.isDark) NextBlue.copy(alpha = 0.2f) else NextBlueLight,
      ) {
        Text(
          text = "$folderCount",
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          color = if (colors.isDark) Color(0xFF60A5FA) else NextBlue,
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
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
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
            tint = NextBlue,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = sortType.displayName.uppercase(Locale.getDefault()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
          )
        }
      }

      // View Mode Toggle Button (List vs Grid)
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.clickable(onClick = onToggleLayout),
      ) {
        Box(
          modifier = Modifier.padding(6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            if (mediaLayoutMode == MediaLayoutMode.GRID) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
            contentDescription = "Toggle View",
            tint = colors.textPrimary,
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
