package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.DeleteMediaConfirmDialog
import com.example.ui.components.MediaActionBottomSheet
import com.example.ui.components.MoveMediaDialog
import com.example.ui.components.RenameMediaDialog
import com.example.data.local.MediaTarget
import com.example.ui.screens.LibraryPlaylistsScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.ShortsFeedScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.ThemesSoundLabScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.MusicNasirKhanTheme
import com.example.ui.viewmodel.MediaActionType
import com.example.ui.viewmodel.MusicViewModel
import com.example.ui.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {

    private var currentViewModel: MusicViewModel? = null

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun checkHasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onResume() {
        super.onResume()
        currentViewModel?.onAppResume(checkHasPermissions())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            currentViewModel = viewModel
            val uiState by viewModel.uiState.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissionsMap ->
                val isGranted = permissionsMap.values.any { it }
                viewModel.updatePermissionStatus(isGranted)
            }

            val systemDeleteLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.onSystemDeleteConfirmed()
                } else {
                    viewModel.onSystemDeleteCancelled()
                }
            }

            LaunchedEffect(Unit) {
                val hasPermission = checkHasPermissions()
                viewModel.updatePermissionStatus(hasPermission)
                if (!hasPermission) {
                    permissionLauncher.launch(getRequiredPermissions())
                }
            }

            MusicNasirKhanTheme(appTheme = uiState.currentTheme) {
                val isImmersive = uiState.currentTab == NavigationTab.VIDEOS && uiState.isVideoFullscreen

                BackHandler(enabled = uiState.isVideoFullscreen) {
                    viewModel.setVideoFullscreen(false)
                }

                BackHandler(enabled = !uiState.isVideoFullscreen && uiState.currentTab != NavigationTab.NOW_PLAYING) {
                    viewModel.setTab(NavigationTab.NOW_PLAYING)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (!isImmersive) {
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
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    NavigationBar(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.horizontalGradient(
                                                    listOf(
                                                        primaryColor.copy(alpha = 0.35f),
                                                        Color(uiState.currentTheme.secondaryHex).copy(alpha = 0.2f),
                                                        primaryColor.copy(alpha = 0.35f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(22.dp)
                                            )
                                            .testTag("main_navigation_bar"),
                                        containerColor = surfaceColor.copy(alpha = 0.94f),
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        tonalElevation = 8.dp
                                    ) {
                                    // 1. Player
                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.NOW_PLAYING,
                                        onClick = { viewModel.setTab(NavigationTab.NOW_PLAYING) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.NOW_PLAYING) Icons.Filled.Album else Icons.Outlined.Album,
                                                contentDescription = "Player"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Player",
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.NOW_PLAYING) FontWeight.Bold else FontWeight.Normal
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

                                    // 2. Playlists / Library
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
                                                "Music",
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.PLAYLISTS) FontWeight.Bold else FontWeight.Normal
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

                                    // 3. Shorts (Pure Fullscreen Vertical Feed)
                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.SHORTS,
                                        onClick = { viewModel.setTab(NavigationTab.SHORTS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.SHORTS) Icons.Filled.Bolt else Icons.Outlined.Bolt,
                                                contentDescription = "Shorts"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Shorts",
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.SHORTS) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_shorts")
                                    )

                                    // 4. Long Videos
                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.VIDEOS,
                                        onClick = { viewModel.setTab(NavigationTab.VIDEOS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.VIDEOS) Icons.Filled.Movie else Icons.Outlined.Movie,
                                                contentDescription = "Videos"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Videos",
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.VIDEOS) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_videos")
                                    )

                                    // 5. Statistics
                                    NavigationBarItem(
                                        selected = uiState.currentTab == NavigationTab.STATS,
                                        onClick = { viewModel.setTab(NavigationTab.STATS) },
                                        icon = {
                                            Icon(
                                                imageVector = if (uiState.currentTab == NavigationTab.STATS) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                                contentDescription = "Stats"
                                            )
                                        },
                                        label = {
                                            Text(
                                                "Stats",
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.STATS) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.Black,
                                            selectedTextColor = primaryColor,
                                            indicatorColor = primaryColor,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_tab_stats")
                                    )

                                    // 6. Themes & Lab Settings
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
                                                fontSize = 10.sp,
                                                fontWeight = if (uiState.currentTab == NavigationTab.THEMES_LAB) FontWeight.Bold else FontWeight.Normal
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
                    }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = if (isImmersive) 0.dp else innerPadding.calculateTopPadding(),
                                    bottom = if (isImmersive) 0.dp else innerPadding.calculateBottomPadding()
                                )
                        ) {
                            if (!uiState.hasStoragePermission && !isImmersive) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Storage Access Required",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Media access is needed to play songs and videos from your device storage.",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                        Button(
                                            onClick = { permissionLauncher.launch(getRequiredPermissions()) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .then(if (isImmersive) Modifier else Modifier.statusBarsPadding())
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
                                        onAddToPlaylist = { viewModel.setAddToPlaylistSong(it) },
                                        onMoreClick = { viewModel.openMediaMenu(MediaTarget.SongMedia(it)) }
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

                                NavigationTab.SHORTS -> {
                                    ShortsFeedScreen(
                                        viewModel = viewModel
                                    )
                                }

                                NavigationTab.VIDEOS -> {
                                    VideosScreen(
                                        viewModel = viewModel
                                    )
                                }

                                NavigationTab.STATS -> {
                                    StatsScreen(
                                        viewModel = viewModel
                                    )
                                }

                                NavigationTab.THEMES_LAB -> {
                                    ThemesSoundLabScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }

                            // Floating Mini Player when not on NowPlaying, Videos, or Shorts screen
                            if (uiState.currentTab != NavigationTab.NOW_PLAYING &&
                                uiState.currentTab != NavigationTab.VIDEOS &&
                                uiState.currentTab != NavigationTab.SHORTS &&
                                uiState.currentSong != null &&
                                !uiState.isVideoPlaying
                            ) {
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

                        // 3-Dot Media Management Bottom Sheet & Dialogs (Rename, Share, Move, Delete)
                        val activeTarget = uiState.activeMediaTarget
                        val activeAction = uiState.activeMediaAction
                        if (activeTarget != null) {
                            when (activeAction) {
                                MediaActionType.MENU -> {
                                    MediaActionBottomSheet(
                                        target = activeTarget,
                                        theme = uiState.currentTheme,
                                        onDismiss = { viewModel.closeMediaAction() },
                                        onRenameClick = { viewModel.openRenameDialog(activeTarget) },
                                        onShareClick = { viewModel.performShare(this@MainActivity, activeTarget) },
                                        onMoveClick = { viewModel.openMoveDialog(activeTarget) },
                                        onDeleteClick = { viewModel.openDeleteDialog(activeTarget) }
                                    )
                                }
                                MediaActionType.RENAME -> {
                                    RenameMediaDialog(
                                        target = activeTarget,
                                        theme = uiState.currentTheme,
                                        onDismiss = { viewModel.closeMediaAction() },
                                        onConfirmRename = { newBaseName ->
                                            viewModel.performRename(activeTarget, newBaseName)
                                        }
                                    )
                                }
                                MediaActionType.MOVE -> {
                                    MoveMediaDialog(
                                        target = activeTarget,
                                        theme = uiState.currentTheme,
                                        onDismiss = { viewModel.closeMediaAction() },
                                        onConfirmMove = { destinationDir ->
                                            viewModel.performMove(activeTarget, destinationDir)
                                        }
                                    )
                                }
                                MediaActionType.DELETE -> {
                                    DeleteMediaConfirmDialog(
                                        target = activeTarget,
                                        theme = uiState.currentTheme,
                                        onDismiss = { viewModel.closeMediaAction() },
                                        onConfirmDelete = {
                                            viewModel.performDelete(activeTarget) { intentSender ->
                                                systemDeleteLauncher.launch(
                                                    IntentSenderRequest.Builder(intentSender).build()
                                                )
                                            }
                                        }
                                    )
                                }
                                null -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
