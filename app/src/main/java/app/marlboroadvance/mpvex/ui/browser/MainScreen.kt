package app.marlboroadvance.mpvex.ui.browser

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.domain.music.ApnaAudioPlayerManager
import app.marlboroadvance.mpvex.ui.browser.folderlist.FolderListScreen
import app.marlboroadvance.mpvex.ui.browser.music.MiniPlayerPill
import app.marlboroadvance.mpvex.ui.browser.music.MusicLibraryScreen
import app.marlboroadvance.mpvex.ui.browser.music.NowPlayingSheet
import app.marlboroadvance.mpvex.ui.browser.networkstreaming.NetworkStreamingScreen
import app.marlboroadvance.mpvex.ui.browser.playlist.PlaylistScreen
import app.marlboroadvance.mpvex.ui.browser.recentlyplayed.RecentlyPlayedScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.LocalLiquidGlassEnabled
import app.marlboroadvance.mpvex.ui.theme.aura.AuraTokens
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.koin.compose.koinInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
object MainScreen : Screen {
  // Use a companion object to store state more persistently
  private var persistentSelectedTab: Int = 0

  // Reactive shared state that can be updated by FileSystemBrowserScreen / selection handlers
  private val _isInSelectionMode = MutableStateFlow(false)
  val isInSelectionMode: StateFlow<Boolean> = _isInSelectionMode.asStateFlow()

  private val _shouldHideNavigationBar = MutableStateFlow(false)
  val shouldHideNavigationBar: StateFlow<Boolean> = _shouldHideNavigationBar.asStateFlow()

  private val _sharedVideoSelectionManager = MutableStateFlow<Any?>(null)
  val sharedVideoSelectionManager: StateFlow<Any?> = _sharedVideoSelectionManager.asStateFlow()

  private val _isPermissionDenied = MutableStateFlow(false)

  /**
   * Update selection state and navigation bar visibility
   * This method should be called whenever selection changes
   */
  fun updateSelectionState(
    isInSelectionMode: Boolean,
    isOnlyVideosSelected: Boolean,
    selectionManager: Any?
  ) {
    _isInSelectionMode.value = isInSelectionMode
    _sharedVideoSelectionManager.value = selectionManager
    // Only hide navigation bar when videos are selected AND in selection mode
    _shouldHideNavigationBar.value = isInSelectionMode && isOnlyVideosSelected
  }

  /**
   * Update permission state to control FAB visibility
   */
  fun updatePermissionState(isDenied: Boolean) {
    _isPermissionDenied.value = isDenied
  }

  /**
   * Get current permission denied state
   */
  fun getPermissionDeniedState(): Boolean = _isPermissionDenied.value

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state
   */
  fun updateBottomBarVisibility(shouldShow: Boolean) {
    _shouldHideNavigationBar.value = !shouldShow
  }

  @Composable
  @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
  override fun Content() {
    val appearancePreferences = koinInject<AppearancePreferences>()
    val showHomeTab by appearancePreferences.showHomeTab.collectAsState()
    val showRecentsTab by appearancePreferences.showRecentsTab.collectAsState()
    val showPlaylistsTab by appearancePreferences.showPlaylistsTab.collectAsState()
    val showNetworkTab by appearancePreferences.showNetworkTab.collectAsState()
    val showMusicTab by appearancePreferences.showMusicTab.collectAsState()
    val isLiquidGlass = LocalLiquidGlassEnabled.current

    val availableTabs = remember(showHomeTab, showMusicTab, showRecentsTab, showPlaylistsTab, showNetworkTab) {
      buildList {
        if (showHomeTab) add(0)
        if (showMusicTab) add(1)
        if (showRecentsTab) add(2)
        if (showPlaylistsTab) add(3)
        if (showNetworkTab) add(4)
        if (isEmpty()) add(0) // Default fallback if all turned off
      }
    }

    var selectedTab by remember {
      mutableIntStateOf(if (persistentSelectedTab in availableTabs) persistentSelectedTab else availableTabs.first())
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val musicPlayerManager = remember { ApnaAudioPlayerManager.getInstance(context) }
    val isNowPlayingOpen by musicPlayerManager.isNowPlayingVisible.collectAsState()

    LaunchedEffect(availableTabs) {
      if (selectedTab !in availableTabs) {
        selectedTab = availableTabs.first()
      }
    }

    val hideNavigationBar by _shouldHideNavigationBar.collectAsState()

    val haptic = LocalHapticFeedback.current

    // Update persistent state whenever tab changes
    LaunchedEffect(selectedTab) {
      persistentSelectedTab = selectedTab
    }

    // Root container ensuring NowPlayingSheet is rendered at highest z-index across entire window
    Box(modifier = Modifier.fillMaxSize()) {
      // Scaffold with bottom navigation bar
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
          // Bottom bar completely hides when NowPlaying full-screen sheet is active
          AnimatedVisibility(
            visible = !isNowPlayingOpen,
            enter = fadeIn(tween(150)) + slideInVertically(
              animationSpec = tween(250),
              initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = fadeOut(tween(150)) + slideOutVertically(
              animationSpec = tween(200),
              targetOffsetY = { fullHeight -> fullHeight }
            )
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              // Floating Mini Player Pill docked above Navigation Bar
              MiniPlayerPill(
                playerManager = musicPlayerManager,
                onClick = { musicPlayerManager.openNowPlaying() },
                modifier = Modifier.fillMaxWidth(),
              )

              // Animated bottom navigation bar with slide animations
              AnimatedVisibility(
                visible = !hideNavigationBar && availableTabs.size > 1,
                enter = slideInVertically(
                  animationSpec = tween(durationMillis = 300),
                  initialOffsetY = { fullHeight -> fullHeight }
                ),
                exit = slideOutVertically(
                  animationSpec = tween(durationMillis = 300),
                  targetOffsetY = { fullHeight -> fullHeight }
                )
              ) {
                val navBarShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                val navBarModifier = if (isLiquidGlass) {
                  Modifier
                    .clip(navBarShape)
                    .border(
                      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                      navBarShape
                    )
                } else {
                  Modifier.clip(navBarShape)
                }

                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val navBarContainerColor = if (isLiquidGlass) {
                  if (isDarkTheme) AuraTokens.CarbonElevated.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.92f)
                } else {
                  MaterialTheme.colorScheme.surfaceContainer
                }

                val navItemColors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                NavigationBar(
                  containerColor = navBarContainerColor,
                  modifier = navBarModifier
                ) {
                  if (showHomeTab) {
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.Folder, contentDescription = "Folders") },
                      label = { Text("Folders") },
                      selected = selectedTab == 0,
                      colors = navItemColors,
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 0
                      }
                    )
                  }
                  if (showMusicTab) {
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.MusicNote, contentDescription = "Music") },
                      label = { Text("Music") },
                      selected = selectedTab == 1,
                      colors = navItemColors,
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 1
                      }
                    )
                  }
                  if (showRecentsTab) {
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.History, contentDescription = "Recents") },
                      label = { Text("Recents") },
                      selected = selectedTab == 2,
                      colors = navItemColors,
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 2
                      }
                    )
                  }
                  if (showPlaylistsTab) {
                    NavigationBarItem(
                      icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = "Playlists") },
                      label = { Text("Playlists") },
                      selected = selectedTab == 3,
                      colors = navItemColors,
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 3
                      }
                    )
                  }
                  if (showNetworkTab) {
                    NavigationBarItem(
                      icon = { Icon(Icons.Filled.Wifi, contentDescription = "Network") },
                      label = { Text("Network") },
                      selected = selectedTab == 4,
                      colors = navItemColors,
                      onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = 4
                      }
                    )
                  }
                }
              }
            }
          }
        }
      ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
          // Always use 80dp bottom padding regardless of navigation bar visibility
          val fabBottomPadding = 80.dp

          AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
              // Material 3 Expressive slide-in-fade animation (like Google Phone app)
              val slideDistance = with(density) { 48.dp.roundToPx() }
              val animationDuration = 250
              
              if (targetState > initialState) {
                // Moving forward: slide in from right with fade
                (slideInHorizontally(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  ),
                  initialOffsetX = { slideDistance }
                ) + fadeIn(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  )
                )) togetherWith (slideOutHorizontally(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  ),
                  targetOffsetX = { -slideDistance }
                ) + fadeOut(
                  animationSpec = tween(
                    durationMillis = animationDuration / 2,
                    easing = FastOutSlowInEasing
                  )
                ))
              } else {
                // Moving backward: slide in from left with fade
                (slideInHorizontally(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  ),
                  initialOffsetX = { -slideDistance }
                ) + fadeIn(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  )
                )) togetherWith (slideOutHorizontally(
                  animationSpec = tween(
                    durationMillis = animationDuration,
                    easing = FastOutSlowInEasing
                  ),
                  targetOffsetX = { slideDistance }
                ) + fadeOut(
                  animationSpec = tween(
                    durationMillis = animationDuration / 2,
                    easing = FastOutSlowInEasing
                  )
                ))
              }
            },
            label = "tab_animation"
          ) { targetTab ->
            CompositionLocalProvider(
              LocalNavigationBarHeight provides fabBottomPadding
            ) {
              when (targetTab) {
                0 -> FolderListScreen.Content()
                1 -> MusicLibraryScreen.Content()
                2 -> RecentlyPlayedScreen.Content()
                3 -> PlaylistScreen.Content()
                4 -> NetworkStreamingScreen.Content()
                else -> FolderListScreen.Content()
              }
            }
          }
        }
      }

      // Fullscreen Now Playing Sheet placed OUTSIDE the Scaffold - immune to bottom bar overlap
      NowPlayingSheet(
        isOpen = isNowPlayingOpen,
        onDismissRequest = { musicPlayerManager.closeNowPlaying() },
        playerManager = musicPlayerManager,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

// CompositionLocal for navigation bar height
val LocalNavigationBarHeight = compositionLocalOf { 0.dp }