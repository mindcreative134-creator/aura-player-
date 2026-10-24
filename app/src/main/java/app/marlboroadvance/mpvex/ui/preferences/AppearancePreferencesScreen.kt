package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MultiChoiceSegmentedButton
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.ThemePicker
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Serializable
object AppearancePreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val preferences = koinInject<AppearancePreferences>()
        val browserPreferences = koinInject<BrowserPreferences>()
        val gesturePreferences = koinInject<GesturePreferences>()
        val backstack = LocalBackStack.current
        val systemDarkTheme = isSystemInDarkTheme()

        val darkMode by preferences.darkMode.collectAsState()
        val appTheme by preferences.appTheme.collectAsState()

        // Determine if we're in dark mode for theme preview
        val isDarkMode = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_appearance_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backstack::removeLastOrNull) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            ProvidePreferenceLocals {
                LazyColumn(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_theme))
                    }

                    item {
                        PreferenceCard {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // Dark mode selector
                                MultiChoiceSegmentedButton(
                                    choices = DarkMode.entries.map { stringResource(it.titleRes) }.toImmutableList(),
                                    selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
                                    onClick = { preferences.darkMode.set(DarkMode.entries[it]) },
                                )
                            }

                            PreferenceDivider()

                            // AMOLED mode state - need it before theme picker
                            val amoledMode by preferences.amoledMode.collectAsState()

                            // Theme picker - Aniyomi style
                            ThemePicker(
                                currentTheme = appTheme,
                                isDarkMode = isDarkMode,
                                onThemeSelected = { preferences.appTheme.set(it) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )

                            PreferenceDivider()

                            // AMOLED mode toggle
                            SwitchPreference(
                                value = amoledMode,
                                onValueChange = { newValue ->
                                    preferences.amoledMode.set(newValue)
                                },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_amoled_mode_title)) },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_amoled_mode_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                enabled = darkMode != DarkMode.Light
                            )
                            PreferenceDivider()

                            val liquidGlassSurfaces by preferences.liquidGlassSurfaces.collectAsState()
                            SwitchPreference(
                                value = liquidGlassSurfaces,
                                onValueChange = preferences.liquidGlassSurfaces::set,
                                title = { Text("Liquid glass surfaces") },
                                summary = {
                                    Text(
                                        text = "Use translucent, softly outlined surfaces across navigation bars and player controls",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            val useSystemFont by preferences.useSystemFont.collectAsState()
                            SwitchPreference(
                                value = useSystemFont,
                                onValueChange = preferences.useSystemFont::set,
                                title = { Text("Use system font") },
                                summary = {
                                    Text(
                                        text = "Display app interface using device's system default font",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = "Navigation Tabs")
                    }

                    item {
                        PreferenceCard {
                            val showHomeTab by preferences.showHomeTab.collectAsState()
                            val showRecentsTab by preferences.showRecentsTab.collectAsState()
                            val showPlaylistsTab by preferences.showPlaylistsTab.collectAsState()
                            val showNetworkTab by preferences.showNetworkTab.collectAsState()
                            val showMusicTab by preferences.showMusicTab.collectAsState()

                            SwitchPreference(
                                value = showHomeTab,
                                onValueChange = preferences.showHomeTab::set,
                                title = { Text("Folders & Videos") },
                                summary = {
                                    Text(
                                        text = "Show local storage folders and video browser tab",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = showRecentsTab,
                                onValueChange = preferences.showRecentsTab::set,
                                title = { Text("Recents") },
                                summary = {
                                    Text(
                                        text = "Show recently played videos tab",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = showPlaylistsTab,
                                onValueChange = preferences.showPlaylistsTab::set,
                                title = { Text("Playlists") },
                                summary = {
                                    Text(
                                        text = "Show custom user playlists tab",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = showNetworkTab,
                                onValueChange = preferences.showNetworkTab::set,
                                title = { Text("Network Streaming") },
                                summary = {
                                    Text(
                                        text = "Show online streams, HLS, and network servers tab",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            SwitchPreference(
                                value = showMusicTab,
                                onValueChange = preferences.showMusicTab::set,
                                title = { Text("Music Library") },
                                summary = {
                                    Text(
                                        text = "Show audio player and music library tab",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )
                        }
                    }

                    item {
                        PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_file_browser))
                    }

                    item {
                        PreferenceCard {
                            val unlimitedNameLines by preferences.unlimitedNameLines.collectAsState()
                            SwitchPreference(
                                value = unlimitedNameLines,
                                onValueChange = { preferences.unlimitedNameLines.set(it) },
                                title = {
                                    Text(
                                        text = "Show full video name",
                                    )
                                },
                                summary = {
                                    Text(
                                        text = "Display full video title across multiple lines without truncating",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val showVideoThumbnails by browserPreferences.showVideoThumbnails.collectAsState()
                            SwitchPreference(
                                value = showVideoThumbnails,
                                onValueChange = { browserPreferences.showVideoThumbnails.set(it) },
                                title = { Text("Show video thumbnails") },
                                summary = {
                                    Text(
                                        text = "Generate and display video frame thumbnails in lists",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val showNetworkThumbnails by browserPreferences.showNetworkThumbnails.collectAsState()
                            SwitchPreference(
                                value = showNetworkThumbnails,
                                onValueChange = { browserPreferences.showNetworkThumbnails.set(it) },
                                title = { Text("Network thumbnails") },
                                summary = {
                                    Text(
                                        text = "Fetch and show thumbnails for online and remote network media",
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                enabled = showVideoThumbnails,
                            )

                            PreferenceDivider()

                            val showUnplayedOldVideoLabel by preferences.showUnplayedOldVideoLabel.collectAsState()
                            SwitchPreference(
                                value = showUnplayedOldVideoLabel,
                                onValueChange = { preferences.showUnplayedOldVideoLabel.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val unplayedOldVideoDays by preferences.unplayedOldVideoDays.collectAsState()
                            SliderPreference(
                                value = unplayedOldVideoDays.toFloat(),
                                onValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_unplayed_old_video_days_title)) },
                                valueRange = 1f..30f,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_unplayed_old_video_days_summary,
                                            unplayedOldVideoDays,
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                                onSliderValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                sliderValue = unplayedOldVideoDays.toFloat(),
                                enabled = showUnplayedOldVideoLabel
                            )

                            PreferenceDivider()

                            val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
                            SwitchPreference(
                                value = autoScrollToLastPlayed,
                                onValueChange = { browserPreferences.autoScrollToLastPlayed.set(it) },
                                title = {
                                    Text(text = stringResource(R.string.pref_appearance_auto_scroll_title))
                                },
                                summary = {
                                    Text(
                                        text = stringResource(R.string.pref_appearance_auto_scroll_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )

                            PreferenceDivider()

                            val watchedThreshold by browserPreferences.watchedThreshold.collectAsState()
                            SliderPreference(
                                value = watchedThreshold.toFloat(),
                                onValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                sliderValue = watchedThreshold.toFloat(),
                                onSliderValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                title = { Text(text = stringResource(id = R.string.pref_appearance_watched_threshold_title)) },
                                valueRange = 50f..100f,
                                valueSteps = 9,
                                summary = {
                                    Text(
                                        text = stringResource(
                                            id = R.string.pref_appearance_watched_threshold_summary,
                                            watchedThreshold,
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                },
                            )

                            PreferenceDivider()

                            val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
                            SwitchPreference(
                                value = tapThumbnailToSelect,
                                onValueChange = { gesturePreferences.tapThumbnailToSelect.set(it) },
                                title = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_title),
                                    )
                                },
                                summary = {
                                    Text(
                                        text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_summary),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )
                        }
                    }

                }
            }
        }
    }
}
