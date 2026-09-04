package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.data.local.MediaTarget
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.model.VideoItem
import com.example.ui.components.AspectFitVideoContainer
import com.example.ui.components.AspectFitVideoView
import com.example.ui.components.VideoDimensionHelper
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Information about audio or subtitle track detected in the video container.
 */
data class VideoTrackOption(
    val index: Int,
    val type: Int,
    val label: String,
    val language: String
)

/**
 * Long Videos Section:
 * Exclusively for full-length videos and productions (> 90 seconds).
 *
 * Supports:
 * - Fullscreen immersive player with aspect-ratio preservation (fit/contain)
 * - Portrait & Landscape rotation support (manual toggle & sensor-based)
 * - Complete playback controls (Seek, Play/Pause, -10s, +10s, Volume, Brightness)
 * - Real audio/language & subtitle track selection
 * - "Play Audio in Music Player" handoff for music videos
 * - Smooth BackHandler navigation
 */
@Composable
fun VideosScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val longVideos by viewModel.longVideos.collectAsState()
    val favoriteVideos by viewModel.favoriteVideos.collectAsState()

    // Filter long videos strictly for duration > 90 seconds (1 minute 30 seconds)
    val filteredLongVideos = remember(longVideos, uiState.shortsThresholdSeconds) {
        longVideos.filter { it.durationSeconds > uiState.shortsThresholdSeconds }
    }
    val filteredFavoriteLongVideos = remember(favoriteVideos, uiState.shortsThresholdSeconds) {
        favoriteVideos.filter { it.durationSeconds > uiState.shortsThresholdSeconds }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Long Videos, 1 = Favorites

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    // Intercept back button when in fullscreen mode
    BackHandler(enabled = uiState.isVideoFullscreen) {
        viewModel.setVideoFullscreen(false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // If fullscreen is active and a video is selected, show FullscreenVideoPlayer
    if (uiState.isVideoFullscreen && uiState.currentVideo != null) {
        FullscreenLongVideoPlayer(
            video = uiState.currentVideo!!,
            isPlaying = uiState.isVideoPlaying,
            currentPos = uiState.videoPositionSec,
            viewModel = viewModel,
            onExitFullscreen = {
                viewModel.setVideoFullscreen(false)
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        )
    } else {
        // Normal Catalog View
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0D0F14))
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Long Videos & Productions",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Full-length videos over 90s (${filteredLongVideos.size})",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.scanLocalMedia(silent = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2333)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("scan_long_videos_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Rescan",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search long videos, concerts, music videos...",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF141721),
                        unfocusedContainerColor = Color(0xFF141721),
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                val tabTitles = listOf("🎬 Long Videos", "❤️ Favorite Videos")
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF141721),
                    contentColor = Color(0xFF00E5FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00E5FF),
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Active filtered list
            val baseList = if (selectedTab == 0) filteredLongVideos else filteredFavoriteLongVideos
            val activeList = remember(baseList, searchQuery) {
                if (searchQuery.isBlank()) baseList
                else baseList.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true)
                }
            }

            LongVideosCatalogView(
                videos = activeList,
                viewModel = viewModel,
                currentVideo = uiState.currentVideo,
                isVideoPlaying = uiState.isVideoPlaying,
                videoPositionSec = uiState.videoPositionSec,
                isFavoriteTab = selectedTab == 1
            )
        }
    }
}

/**
 * Catalog list showing the active player card (if a video is selected) and the video cards.
 */
@Composable
fun LongVideosCatalogView(
    videos: List<VideoItem>,
    viewModel: MusicViewModel,
    currentVideo: VideoItem?,
    isVideoPlaying: Boolean,
    videoPositionSec: Float,
    isFavoriteTab: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Player Card if a long video is selected
        if (currentVideo != null && currentVideo.durationSeconds > uiState.shortsThresholdSeconds) {
            item(key = "active_player_card") {
                ActiveLongVideoPlayerCard(
                    video = currentVideo,
                    isPlaying = isVideoPlaying,
                    currentPos = videoPositionSec,
                    viewModel = viewModel
                )
            }
        }

        item {
            Text(
                text = if (isFavoriteTab) "Favorite Long Videos" else "Long Video Library Catalog",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (videos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavoriteTab) Icons.Default.FavoriteBorder else Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isFavoriteTab) "No favorite long videos yet." else "No long videos found on your device.",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFavoriteTab) {
                                "Tap the heart icon on any video to add it to your favorites."
                            } else {
                                "Videos over 90 seconds stored in your device storage will appear here."
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        if (!isFavoriteTab) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.scanLocalMedia() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Device Storage", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(videos, key = { it.id }) { video ->
                VideoListItemCard(
                    video = video,
                    isSelected = currentVideo?.id == video.id,
                    onClick = { viewModel.playVideo(video) },
                    onFavorite = { viewModel.toggleVideoFavorite(video) },
                    onPlayAsAudio = { viewModel.playVideoAsAudio(video) },
                    onMoreClick = { viewModel.openMediaMenu(MediaTarget.VideoMedia(video)) }
                )
            }
        }
    }
}

/**
 * Modern In-Card Long Video Player with aspect-ratio preservation, fullscreen button,
 * orientation toggle, volume/brightness/audio controls, and "Play Audio in Music Player".
 */
@Composable
fun ActiveLongVideoPlayerCard(
    video: VideoItem,
    isPlaying: Boolean,
    currentPos: Float,
    viewModel: MusicViewModel
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var showVolumeDialog by remember { mutableStateOf(false) }
    var showBrightnessDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    var detectedAudioTracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
    var detectedSubtitleTracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
    var selectedAudioTrackIndex by remember { mutableIntStateOf(-1) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<AspectFitVideoView?>(null) }
    var videoRatio by remember(video.id) {
        mutableStateOf<Float?>(VideoDimensionHelper.parseRatio(video.resolution))
    }

    LaunchedEffect(video.uri) {
        withContext(Dispatchers.IO) {
            val dims = VideoDimensionHelper.getDisplayDimensions(context, video.uri)
            if (dims != null && dims.first > 0 && dims.second > 0) {
                val ratio = dims.first.toFloat() / dims.second.toFloat()
                withContext(Dispatchers.Main) {
                    videoRatio = ratio
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Player View Container with aspect-fit contain
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .testTag("active_video_container"),
                contentAlignment = Alignment.Center
            ) {
                AspectFitVideoContainer(
                    videoRatio = videoRatio,
                    modifier = Modifier.fillMaxSize()
                ) { fitWidth, fitHeight ->
                    if (video.uri.isNotBlank()) {
                        AndroidView(
                            factory = { ctx ->
                                AspectFitVideoView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setVideoURI(Uri.parse(video.uri))
                                    setOnPreparedListener { mp ->
                                        mediaPlayerRef = mp
                                        mp.setOnVideoSizeChangedListener { _, w, h ->
                                            if (w > 0 && h > 0) {
                                                updateVideoSize(w, h)
                                                if (videoRatio == null) {
                                                    videoRatio = w.toFloat() / h.toFloat()
                                                }
                                            }
                                        }
                                        updateVideoSize(mp.videoWidth, mp.videoHeight)
                                        if (videoRatio == null && mp.videoWidth > 0 && mp.videoHeight > 0) {
                                            videoRatio = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                                        }
                                        // Detect audio and subtitle tracks
                                        val (aTracks, sTracks) = inspectMediaTracks(mp)
                                        detectedAudioTracks = aTracks
                                        detectedSubtitleTracks = sTracks

                                        if (isPlaying) start()
                                    }
                                    setOnErrorListener { _, _, _ -> true }
                                    setOnCompletionListener {
                                        viewModel.onVideoCompleted(video.id)
                                    }
                                    videoViewRef = this
                                }
                            },
                            update = { view ->
                                videoViewRef = view
                                if (isPlaying) {
                                    if (!view.isPlaying) view.start()
                                } else {
                                    if (view.isPlaying) view.pause()
                                }
                            },
                            modifier = Modifier.size(fitWidth, fitHeight)
                        )
                    }
                }

                // Top Overlay: Close, Rotation, and Fullscreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Resolution Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = video.resolution,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Rotation toggle
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    activity?.let { act ->
                                        act.requestedOrientation = if (isLandscape) {
                                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("card_rotation_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Rotate Screen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Fullscreen button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.setVideoFullscreen(true) },
                                modifier = Modifier.testTag("enter_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Enter Fullscreen",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Dismiss/Close player card
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.closeVideoPlayer() },
                                modifier = Modifier.testTag("close_player_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Controls & Information Area
            Column(modifier = Modifier.padding(14.dp)) {
                // Title and Favorite Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.artist,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleVideoFavorite(video) },
                        modifier = Modifier.testTag("active_video_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White
                        )
                    }
                }

                // Progress scrubber slider
                val duration = video.durationSeconds.toFloat().coerceAtLeast(1f)
                val safePos = currentPos.coerceIn(0f, duration)

                Slider(
                    value = safePos,
                    onValueChange = {
                        viewModel.setVideoPosition(it)
                        videoViewRef?.seekTo((it * 1000).toInt())
                    },
                    valueRange = 0f..duration,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatSeconds(safePos.toInt()),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Text(
                        formatSeconds(video.durationSeconds),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Control Buttons Row (-10s, Play/Pause, +10s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (currentPos - 10f).coerceAtLeast(0f)
                            viewModel.setVideoPosition(newPos)
                            videoViewRef?.seekTo((newPos * 1000).toInt())
                        }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = { viewModel.toggleVideoPlayPause() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    IconButton(
                        onClick = {
                            val newPos = (currentPos + 10f).coerceAtMost(duration)
                            viewModel.setVideoPosition(newPos)
                            videoViewRef?.seekTo((newPos * 1000).toInt())
                        }
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Modern Action Bar: Listen in Music Player, Volume, Brightness, Audio Tracks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Audio in Music Player Button
                    Button(
                        onClick = { viewModel.playVideoAsAudio(video) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A3D)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("card_play_audio_btn")
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Listen Audio",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Volume Dialog Trigger
                        IconButton(onClick = { showVolumeDialog = true }) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Brightness Dialog Trigger
                        IconButton(onClick = { showBrightnessDialog = true }) {
                            Icon(
                                Icons.Default.Brightness6,
                                contentDescription = "Brightness",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Audio Track Dialog Trigger
                        IconButton(onClick = { showAudioTrackDialog = true }) {
                            Icon(
                                Icons.Default.Audiotrack,
                                contentDescription = "Audio Tracks",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen trigger
                        IconButton(onClick = { viewModel.setVideoFullscreen(true) }) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 3-dot More Options Trigger
                        IconButton(onClick = { viewModel.openMediaMenu(MediaTarget.VideoMedia(video)) }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Volume Dialog
    if (showVolumeDialog) {
        PlayerVolumeDialog(
            onDismiss = { showVolumeDialog = false }
        )
    }

    // Brightness Dialog
    if (showBrightnessDialog) {
        PlayerBrightnessDialog(
            activity = activity,
            onDismiss = { showBrightnessDialog = false }
        )
    }

    // Audio & Subtitle Track Dialog
    if (showAudioTrackDialog) {
        AudioSubtitleTrackDialog(
            audioTracks = detectedAudioTracks,
            subtitleTracks = detectedSubtitleTracks,
            selectedAudioTrackIndex = selectedAudioTrackIndex,
            onSelectAudioTrack = { trackIndex ->
                try {
                    mediaPlayerRef?.selectTrack(trackIndex)
                    selectedAudioTrackIndex = trackIndex
                } catch (e: Exception) {
                    Log.w("VideoPlayer", "Could not select track $trackIndex", e)
                }
            },
            onDismiss = { showAudioTrackDialog = false }
        )
    }
}

/**
 * Complete Fullscreen Immersive Long Video Player:
 * - Edge-to-edge black background
 * - Aspect-fit preservation (fit/contain, uncropped, unstretched)
 * - Orientation toggle (Portrait / Landscape)
 * - Auto-hiding overlay controls with tap-to-toggle
 * - Volume, Brightness, Audio track selection, and Music Player handoff
 */
@Composable
fun FullscreenLongVideoPlayer(
    video: VideoItem,
    isPlaying: Boolean,
    currentPos: Float,
    viewModel: MusicViewModel,
    onExitFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var areControlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    var showVolumeDialog by remember { mutableStateOf(false) }
    var showBrightnessDialog by remember { mutableStateOf(false) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    var detectedAudioTracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
    var detectedSubtitleTracks by remember { mutableStateOf<List<VideoTrackOption>>(emptyList()) }
    var selectedAudioTrackIndex by remember { mutableIntStateOf(-1) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoViewRef by remember { mutableStateOf<AspectFitVideoView?>(null) }
    var videoRatio by remember(video.id) {
        mutableStateOf<Float?>(VideoDimensionHelper.parseRatio(video.resolution))
    }

    LaunchedEffect(video.uri) {
        withContext(Dispatchers.IO) {
            val dims = VideoDimensionHelper.getDisplayDimensions(context, video.uri)
            if (dims != null && dims.first > 0 && dims.second > 0) {
                val ratio = dims.first.toFloat() / dims.second.toFloat()
                withContext(Dispatchers.Main) {
                    videoRatio = ratio
                }
            }
        }
    }

    // Auto-hide controls after 3.5 seconds of playback
    LaunchedEffect(areControlsVisible, lastInteractionTime, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Enable transient immersive system bars
    LaunchedEffect(Unit) {
        activity?.window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Restore system bars and orientation on exit
    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
                lastInteractionTime = System.currentTimeMillis()
            }
            .testTag("fullscreen_video_player"),
        contentAlignment = Alignment.Center
    ) {
        // Video View with aspect-fit contain behavior (never stretched, never cropped)
        AspectFitVideoContainer(
            videoRatio = videoRatio,
            modifier = Modifier.fillMaxSize()
        ) { fitWidth, fitHeight ->
            if (video.uri.isNotBlank()) {
                AndroidView(
                    factory = { ctx ->
                        AspectFitVideoView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoURI(Uri.parse(video.uri))
                            setOnPreparedListener { mp ->
                                mediaPlayerRef = mp
                                mp.setOnVideoSizeChangedListener { _, w, h ->
                                    if (w > 0 && h > 0) {
                                        updateVideoSize(w, h)
                                        if (videoRatio == null) {
                                            videoRatio = w.toFloat() / h.toFloat()
                                        }
                                    }
                                }
                                updateVideoSize(mp.videoWidth, mp.videoHeight)
                                if (videoRatio == null && mp.videoWidth > 0 && mp.videoHeight > 0) {
                                    videoRatio = mp.videoWidth.toFloat() / mp.videoHeight.toFloat()
                                }
                                val (aTracks, sTracks) = inspectMediaTracks(mp)
                                detectedAudioTracks = aTracks
                                detectedSubtitleTracks = sTracks

                                seekTo((currentPos * 1000).toInt())
                                if (isPlaying) start()
                            }
                            setOnErrorListener { _, _, _ -> true }
                            setOnCompletionListener {
                                viewModel.onVideoCompleted(video.id)
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                        if (isPlaying) {
                            if (!view.isPlaying) view.start()
                        } else {
                            if (view.isPlaying) view.pause()
                        }
                    },
                    modifier = Modifier.size(fitWidth, fitHeight)
                )
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                onExitFullscreen()
                            },
                            modifier = Modifier.testTag("exit_fullscreen_button")
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = video.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${video.artist} • ${video.resolution}",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Play Audio in Music Player Handoff
                        Button(
                            onClick = {
                                onExitFullscreen()
                                viewModel.playVideoAsAudio(video)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("fullscreen_play_audio_btn")
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Listen in Music Player",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Orientation toggle button
                        IconButton(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                activity?.let { act ->
                                    act.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    }
                                }
                            },
                            modifier = Modifier.testTag("fullscreen_rotation_btn")
                        ) {
                            Icon(
                                Icons.Default.ScreenRotation,
                                contentDescription = "Toggle Orientation",
                                tint = Color.White
                            )
                        }

                        // 3-dot More Options Trigger
                        IconButton(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                viewModel.openMediaMenu(MediaTarget.VideoMedia(video))
                            },
                            modifier = Modifier.testTag("fullscreen_more_menu_btn")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Seek Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            val newPos = (currentPos - 10f).coerceAtLeast(0f)
                            viewModel.setVideoPosition(newPos)
                            videoViewRef?.seekTo((newPos * 1000).toInt())
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("fullscreen_rewind_btn")
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            viewModel.toggleVideoPlayPause()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                            .testTag("fullscreen_play_pause_btn")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            val duration = video.durationSeconds.toFloat().coerceAtLeast(1f)
                            val newPos = (currentPos + 10f).coerceAtMost(duration)
                            viewModel.setVideoPosition(newPos)
                            videoViewRef?.seekTo((newPos * 1000).toInt())
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("fullscreen_forward_btn")
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Bottom Control Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val duration = video.durationSeconds.toFloat().coerceAtLeast(1f)
                    val safePos = currentPos.coerceIn(0f, duration)

                    // Seek Slider
                    Slider(
                        value = safePos,
                        onValueChange = {
                            lastInteractionTime = System.currentTimeMillis()
                            viewModel.setVideoPosition(it)
                            videoViewRef?.seekTo((it * 1000).toInt())
                        },
                        valueRange = 0f..duration,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current time / Total time
                        Text(
                            text = "${formatSeconds(safePos.toInt())} / ${formatSeconds(video.durationSeconds)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Quick toolbar
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Volume
                            IconButton(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showVolumeDialog = true
                                },
                                modifier = Modifier.testTag("fullscreen_volume_btn")
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Brightness
                            IconButton(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showBrightnessDialog = true
                                },
                                modifier = Modifier.testTag("fullscreen_brightness_btn")
                            ) {
                                Icon(
                                    Icons.Default.Brightness6,
                                    contentDescription = "Brightness",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Audio tracks
                            IconButton(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    showAudioTrackDialog = true
                                },
                                modifier = Modifier.testTag("fullscreen_audio_track_btn")
                            ) {
                                Icon(
                                    Icons.Default.Audiotrack,
                                    contentDescription = "Audio Tracks",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Exit Fullscreen Button
                            IconButton(
                                onClick = { onExitFullscreen() },
                                modifier = Modifier.testTag("fullscreen_toggle_btn")
                            ) {
                                Icon(
                                    Icons.Default.FullscreenExit,
                                    contentDescription = "Exit Fullscreen",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Volume Dialog
    if (showVolumeDialog) {
        PlayerVolumeDialog(
            onDismiss = {
                lastInteractionTime = System.currentTimeMillis()
                showVolumeDialog = false
            }
        )
    }

    // Brightness Dialog
    if (showBrightnessDialog) {
        PlayerBrightnessDialog(
            activity = activity,
            onDismiss = {
                lastInteractionTime = System.currentTimeMillis()
                showBrightnessDialog = false
            }
        )
    }

    // Audio & Subtitle Track Dialog
    if (showAudioTrackDialog) {
        AudioSubtitleTrackDialog(
            audioTracks = detectedAudioTracks,
            subtitleTracks = detectedSubtitleTracks,
            selectedAudioTrackIndex = selectedAudioTrackIndex,
            onSelectAudioTrack = { trackIndex ->
                try {
                    mediaPlayerRef?.selectTrack(trackIndex)
                    selectedAudioTrackIndex = trackIndex
                } catch (e: Exception) {
                    Log.w("VideoPlayer", "Could not select track $trackIndex", e)
                }
            },
            onDismiss = {
                lastInteractionTime = System.currentTimeMillis()
                showAudioTrackDialog = false
            }
        )
    }
}

/**
 * Volume Dialog for adjusting Android AudioManager STREAM_MUSIC volume.
 */
@Composable
fun PlayerVolumeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    val maxVolume = remember {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 7)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2333),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (currentVolume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Volume Level", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (currentVolume == 0) "Muted" else "$currentVolume / $maxVolume",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = currentVolume.toFloat(),
                    onValueChange = {
                        val newVol = it.toInt().coerceIn(0, maxVolume)
                        currentVolume = newVol
                        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                    },
                    valueRange = 0f..maxVolume.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (currentVolume > 0) {
                                currentVolume = 0
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                            } else {
                                val restored = (maxVolume / 2).coerceAtLeast(1)
                                currentVolume = restored
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, restored, 0)
                            }
                        }
                    ) {
                        Text(if (currentVolume > 0) "Mute" else "Unmute", color = Color(0xFFFF5252))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Brightness Dialog for adjusting Window screenBrightness.
 */
@Composable
fun PlayerBrightnessDialog(
    activity: Activity?,
    onDismiss: () -> Unit
) {
    var brightness by remember {
        val current = activity?.window?.attributes?.screenBrightness ?: 0.75f
        mutableFloatStateOf(if (current < 0f) 0.75f else current)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2333),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Brightness6,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Screen Brightness", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${(brightness * 100).toInt()}%",
                    color = Color(0xFF00E5FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = brightness,
                    onValueChange = {
                        val clamped = it.coerceIn(0.05f, 1.0f)
                        brightness = clamped
                        activity?.window?.let { win ->
                            val lp = win.attributes
                            lp.screenBrightness = clamped
                            win.attributes = lp
                        }
                    },
                    valueRange = 0.05f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Real Audio Track and Subtitle Track Selection Dialog.
 * Only shows actual tracks that exist in the video container.
 */
@Composable
fun AudioSubtitleTrackDialog(
    audioTracks: List<VideoTrackOption>,
    subtitleTracks: List<VideoTrackOption>,
    selectedAudioTrackIndex: Int,
    onSelectAudioTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2333),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audio & Language Tracks", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Audio Tracks:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (audioTracks.isEmpty() || audioTracks.size == 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF222B3D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (audioTracks.size == 1) audioTracks.first().label else "Default Device Stereo Audio (Active)",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    audioTracks.forEach { track ->
                        val isSelected = selectedAudioTrackIndex == track.index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF222B3D),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAudioTrack(track.index) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = track.label,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Subtitles / Captions:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                if (subtitleTracks.isEmpty()) {
                    Text(
                        text = "No subtitle or closed-caption tracks found in this video file.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                } else {
                    subtitleTracks.forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF222B3D),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(sub.label, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Inspects MediaPlayer container for real Audio and Subtitle tracks.
 */
private fun inspectMediaTracks(mp: MediaPlayer): Pair<List<VideoTrackOption>, List<VideoTrackOption>> {
    val aTracks = mutableListOf<VideoTrackOption>()
    val sTracks = mutableListOf<VideoTrackOption>()
    try {
        val trackInfos = mp.trackInfo
        trackInfos.forEachIndexed { idx, info ->
            if (info.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                val lang = if (!info.language.isNullOrBlank() && info.language != "und") info.language else "Track ${aTracks.size + 1}"
                aTracks.add(
                    VideoTrackOption(
                        index = idx,
                        type = info.trackType,
                        label = "Audio: $lang",
                        language = lang
                    )
                )
            } else if (info.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE ||
                info.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT
            ) {
                val lang = if (!info.language.isNullOrBlank() && info.language != "und") info.language else "Track ${sTracks.size + 1}"
                sTracks.add(
                    VideoTrackOption(
                        index = idx,
                        type = info.trackType,
                        label = "Subtitle: $lang",
                        language = lang
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.w("VideoPlayer", "Error inspecting media tracks", e)
    }
    return Pair(aTracks, sTracks)
}

/**
 * Video list item card with "Listen Audio" button and favorite button.
 */
@Composable
fun VideoListItemCard(
    video: VideoItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onPlayAsAudio: () -> Unit,
    onMoreClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B2333) else Color(0xFF12141C)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("video_card_${video.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 55.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1D2230)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(24.dp)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        formatSeconds(video.durationSeconds),
                        color = Color.White,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${video.artist} • ${video.playCount} views",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            // Quick Play Audio in Music Player Button
            IconButton(
                onClick = onPlayAsAudio,
                modifier = Modifier.testTag("list_play_audio_${video.id}")
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = "Listen Audio in Music Player",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Favorite Button
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.testTag("list_fav_${video.id}")
            ) {
                Icon(
                    if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3-dot More menu
            IconButton(
                onClick = { onMoreClick?.invoke() },
                modifier = Modifier.testTag("list_more_${video.id}")
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
