package app.marlboroadvance.mpvex.ui.browser.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.repository.MediaFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTheme
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun FolderCard(
  folder: VideoFolder,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isRecentlyPlayed: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  isSelected: Boolean = false,
  onThumbClick: () -> Unit = {},
  showDateModified: Boolean = false,
  customIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  newVideoCount: Int = 0,
  customChipContent: @Composable (() -> Unit)? = null,
  isGridMode: Boolean = false,
  isPinned: Boolean = false,
  onPinClick: (() -> Unit)? = null,
  onQuickPlay: (() -> Unit)? = null,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val unlimitedNameLines by appearancePreferences.unlimitedNameLines.collectAsState()
  val showFolderThumbnails by browserPreferences.showFolderThumbnails.collectAsState()
  val showTotalVideosChip by browserPreferences.showTotalVideosChip.collectAsState()
  val showTotalDurationChip by browserPreferences.showTotalDurationChip.collectAsState()
  val showTotalSizeChip by browserPreferences.showTotalSizeChip.collectAsState()
  val showDateChip by browserPreferences.showDateChip.collectAsState()
  val showFolderPath by browserPreferences.showFolderPath.collectAsState()
  val maxLines = if (unlimitedNameLines) Int.MAX_VALUE else 2

  val context = LocalContext.current
  val density = LocalDensity.current
  // Folder cards exclusively show clean stacked folder icons with file badges, avoiding video thumbnail confusion

  // Remove the redundant folder name from the path
  val parentPath = folder.path.substringBeforeLast("/", folder.path)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = if (isGridMode) 4.dp else 8.dp, vertical = 4.dp)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        AuraTheme.selectedContainer
      } else {
        AuraTheme.cardBackground
      }
    ),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) {
        AuraTheme.selectedBorder
      } else {
        AuraTheme.cardBorder
      }
    ),
  ) {
    if (isGridMode) {
      // GRID LAYOUT - Vertical arrangement
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val folderGridColumnsPortrait by browserPreferences.folderGridColumnsPortrait.collectAsState()
        val folderGridColumnsLandscape by browserPreferences.folderGridColumnsLandscape.collectAsState()
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val folderGridColumns = if (isLandscape) folderGridColumnsLandscape else folderGridColumnsPortrait
        val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
        val horizontalPadding = 32.dp
        val spacing = 8.dp

        val thumbWidthDp = if (folderGridColumns > 1) {
          val totalSpacing = spacing * (folderGridColumns - 1)
          ((screenWidthDp - horizontalPadding - totalSpacing) / folderGridColumns).coerceAtLeast(120.dp)
        } else {
          160.dp
        }
        val aspect = 16f / 9f
        val thumbHeightDp = thumbWidthDp / aspect

        Box(
          modifier = Modifier.size(76.dp),
          contentAlignment = Alignment.Center,
        ) {
          // Subtle back layer to visually depict a folder full of files
          if (folder.videoCount > 1) {
            Surface(
              modifier = Modifier
                .size(62.dp)
                .offset(x = 4.dp, y = (-3).dp),
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
            ) {}
          }

          Box(
            modifier = Modifier
              .size(68.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHigh)
              .combinedClickable(
                onClick = onThumbClick,
                onLongClick = onLongClick,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              customIcon ?: Icons.Filled.Folder,
              contentDescription = "Folder",
              modifier = Modifier.size(52.dp),
              tint = MaterialTheme.colorScheme.secondary,
            )

          if (isPinned) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                modifier = Modifier
                  .padding(3.dp)
                  .size(12.dp),
              )
            }
          }

          if (newVideoCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFD32F2F))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
              Text(
                text = newVideoCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
              )
            }
          }

          if (showTotalDurationChip && folder.totalDuration > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
              Text(
                text = formatDuration(folder.totalDuration),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

        Text(
          folder.name,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
          color = if (isRecentlyPlayed) AuraTheme.accent else AuraTheme.textPrimary,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )

        if (showTotalVideosChip && folder.videoCount > 0) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AuraTheme.accent,
            modifier = Modifier
              .background(
                AuraTheme.accent.copy(alpha = 0.14f),
                RoundedCornerShape(50)
              )
              .padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(54.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(AuraTheme.accent.copy(alpha = if (AuraTheme.isDark) 0.16f else 0.10f))
              .combinedClickable(
                onClick = onThumbClick,
                onLongClick = onLongClick,
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              customIcon ?: Icons.Filled.Folder,
              contentDescription = "Folder",
              modifier = Modifier.size(28.dp),
              tint = AuraTheme.accent,
            )

          if (isPinned) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(3.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = "Pinned",
                modifier = Modifier
                  .padding(2.dp)
                  .size(10.dp),
              )
            }
          }

          // Show new video count badge if folder contains new videos
          if (newVideoCount > 0) {
            Box(
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFD32F2F))
                .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
              Text(
                text = newVideoCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                ),
                color = Color.White,
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            folder.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            color = if (isRecentlyPlayed) AuraTheme.accent else AuraTheme.textPrimary,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
          )
          if (showFolderPath && parentPath.isNotEmpty()) {
            Text(
              parentPath,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = AuraTheme.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
          } else {
            Spacer(modifier = Modifier.height(3.dp))
          }
          FlowRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
          ) {
            // Render custom chip content first if provided
            var hasChip = false
            if (customChipContent != null) {
              customChipContent()
              hasChip = true
            }

            // Hide chips at storage root level (when videoCount is 0)
            if (showTotalVideosChip && folder.videoCount > 0) {
              Text(
                if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                modifier = Modifier
                  .background(
                    AuraTheme.accent.copy(alpha = 0.14f),
                    RoundedCornerShape(50),
                  )
                  .padding(horizontal = 9.dp, vertical = 2.dp),
                color = AuraTheme.accent,
              )
              hasChip = true
            }

            if (showTotalSizeChip && folder.totalSize > 0) {
              Text(
                formatFileSize(folder.totalSize),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                modifier = Modifier
                  .background(
                    AuraTheme.chipBackground,
                    RoundedCornerShape(50),
                  )
                  .padding(horizontal = 9.dp, vertical = 2.dp),
                color = AuraTheme.chipContent,
              )
              hasChip = true
            }

            if (showTotalDurationChip && folder.totalDuration > 0) {
              Text(
                formatDuration(folder.totalDuration),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                modifier = Modifier
                  .background(
                    AuraTheme.chipBackground,
                    RoundedCornerShape(50),
                  )
                  .padding(horizontal = 9.dp, vertical = 2.dp),
                color = AuraTheme.chipContent,
              )
              hasChip = true
            }

            if (showDateChip && folder.lastModified > 0) {
              Text(
                formatDate(folder.lastModified),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
                modifier = Modifier
                  .background(
                    AuraTheme.chipBackground,
                    RoundedCornerShape(50),
                  )
                  .padding(horizontal = 9.dp, vertical = 2.dp),
                color = AuraTheme.chipContent,
              )
              hasChip = true
            }
          }
        }

        if (onQuickPlay != null && folder.videoCount > 0) {
          IconButton(
            onClick = onQuickPlay,
            modifier = Modifier
              .size(36.dp)
              .background(AuraTheme.accent.copy(alpha = 0.12f), CircleShape),
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = "Quick Play",
              tint = AuraTheme.accent,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
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

private fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = arrayOf("B", "KB", "MB", "GB", "TB")
  val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
  val value = bytes / 1024.0.pow(digitGroups.toDouble())
  return String.format(java.util.Locale.getDefault(), "%.1f %s", value, units[digitGroups])
}

private fun formatDate(timestampSeconds: Long): String {
  val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
  return sdf.format(java.util.Date(timestampSeconds * 1000))
}



