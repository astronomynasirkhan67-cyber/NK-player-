package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.LibraryPlaylistsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.ThemesSoundLabScreen
import com.example.ui.theme.MusicNasirKhanTheme
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            MusicNasirKhanTheme(appTheme = uiState.currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        bottomBar = {
                            val primaryColor = Color(uiState.currentTheme.primaryHex)
                            val surfaceColor = MaterialTheme.colorScheme.surface

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(uiState.currentTheme.backgroundHex).copy(alpha = 0.95f),
                                                Color(uiState.currentTheme.backgroundHex)
                                            )
                                        )
                                    )
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                NavigationBar(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.horizontalGradient(
                                                listOf(
                                                    primaryColor.copy(alpha = 0.35f),
                                                    Color(uiState.currentTheme.secondaryHex).copy(alpha = 0.2f),
                                                    primaryColor.copy(alpha = 0.35f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .testTag("main_navigation_bar"),
                                    containerColor = surfaceColor.copy(alpha = 0.92f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.NOW_PLAYING,
                                        onClick = { viewModel.setTab(NavigationTab.NOW_PLAYING) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.NOW_PLAYING) Icons.Filled.Album else Icons.Outlined.Album,
                                                contentDescription = "Now Playing"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Player",
                                                fontWeight = if (uiState.currentTab == NavigationTab.NOW_PLAYING) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_player")
                                    )

                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.PLAYLISTS,
                                        onClick = { viewModel.setTab(NavigationTab.PLAYLISTS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.PLAYLISTS) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                                contentDescription = "Playlists"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Playlists",
                                                fontWeight = if (uiState.currentTab == NavigationTab.PLAYLISTS) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_playlists")
                                    )

                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.THEMES_LAB,
                                        onClick = { viewModel.setTab(NavigationTab.THEMES_LAB) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.THEMES_LAB) Icons.Filled.Palette else Icons.Outlined.Palette,
                                                contentDescription = "Themes & Lab"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Themes",
                                                fontWeight = if (uiState.currentTab == NavigationTab.THEMES_LAB) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_themes")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (uiState.currentTab) {
                                NavigationTab.NOW_PLAYING -> {
                                    NowPlayingScreen(
                                        uiState = uiState,
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onNext = { viewModel.skipNext() },
                                        onPrevious = { viewModel.skipPrevious() },
                                        onSeekTo = { viewModel.seekTo(it) },
                                        onToggleShuffle = { viewModel.toggleShuffle() },
                                        onToggleRepeat = { viewModel.toggleRepeat() },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onSetSpeed = { viewModel.setSpeed(it) },
                                        onSetSleepTimer = { viewModel.setSleepTimer(it) },
                                        onScratch = { viewModel.scratchTurntable() },
                                        onOpenEqualizer = { viewModel.toggleEqualizerDialog(true) },
                                        onToggleLyrics = { viewModel.toggleLyrics() },
                                        onAddToPlaylist = { viewModel.setAddToPlaylistSong(it) }
                                    )
                                }

                                NavigationTab.PLAYLISTS -> {
                                    LibraryPlaylistsScreen(
                                        viewModel = viewModel,
                                        onPlaySong = { song, queue ->
                                            viewModel.playSong(song, queue)
                                        }
                                    )
                                }

                                NavigationTab.THEMES_LAB -> {
                                    ThemesSoundLabScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Floating Mini Player when on Playlists or Themes tab
                            if (uiState.currentTab != NavigationTab.NOW_PLAYING && uiState.currentSong != null) {
                                val currentSong = uiState.currentSong
                                val duration = currentSong?.durationSeconds ?: 1
                                val progress = uiState.currentPositionSec / duration.toFloat()

                                MiniPlayerBar(
                                    song = currentSong,
                                    isPlaying = uiState.isPlaying,
                                    progress = progress,
                                    theme = uiState.currentTheme,
                                    onPlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.skipNext() },
                                    onToggleFavorite = {
                                        currentSong?.let { viewModel.toggleFavorite(it) }
                                    },
                                    onClick = { viewModel.setTab(NavigationTab.NOW_PLAYING) },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }

                        // Equalizer Dialog
                        if (uiState.showEqualizerDialog) {
                            EqualizerDialog(
                                settings = uiState.equalizerSettings,
                                theme = uiState.currentTheme,
                                onSave = { viewModel.updateEqualizer(it) },
                                onDismiss = { viewModel.toggleEqualizerDialog(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
