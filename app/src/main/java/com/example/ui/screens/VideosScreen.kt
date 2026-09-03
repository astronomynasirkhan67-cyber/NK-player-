package com.example.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.VideoItem
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun VideosScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val shorts by viewModel.shorts.collectAsState()
    val longVideos by viewModel.longVideos.collectAsState()
    val favoriteVideos by viewModel.favoriteVideos.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val tabTitles = listOf("⚡ Shorts & Reels", "🎬 Long Videos", "❤️ Favorites")

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
                        text = "Music for Nasir • Video Hub",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Shorts Reels (${shorts.size}) & Full Length Productions (${longVideos.size})",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { viewModel.scanLocalMedia(silent = false) },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .size(38.dp)
                        .testTag("rescan_videos_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan Videos",
                        tint = Color(0xFF00E5FF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_search_input"),
                placeholder = {
                    Text(
                        "Search videos, shorts or artists...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF00E5FF)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF00E5FF)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            TabRow(
                selectedTabIndex = uiState.selectedVideoTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00E5FF),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedVideoTab]),
                        color = Color(0xFF00E5FF)
                    )
                },
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedVideoTab == index,
                        onClick = { viewModel.setSelectedVideoTab(index) },
                        text = {
                            Text(
                                title,
                                color = if (uiState.selectedVideoTab == index) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = if (uiState.selectedVideoTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Filter list based on tab and query
        val activeList = when (uiState.selectedVideoTab) {
            0 -> shorts
            1 -> longVideos
            else -> favoriteVideos
        }.filter {
            if (searchQuery.isBlank()) true
            else it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
        }

        if (uiState.selectedVideoTab == 0) {
            if (activeList.isNotEmpty()) {
                // Shorts / Reels vertical swipe experience
                ShortsReelsPager(
                    shortsList = activeList,
                    viewModel = viewModel
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No Shorts found on your device.",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Short-form video clips under ${uiState.shortsThresholdSeconds} seconds will automatically appear here in vertical playback.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
        } else {
            // Long videos / Favorites catalog view
            LongVideosCatalogView(
                videos = activeList,
                viewModel = viewModel,
                currentVideo = uiState.currentVideo,
                isVideoPlaying = uiState.isVideoPlaying,
                videoPositionSec = uiState.videoPositionSec,
                isFavoriteTab = uiState.selectedVideoTab == 2
            )
        }
    }
}

@Composable
fun ShortsReelsPager(
    shortsList: List<VideoItem>,
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { shortsList.size })

    // When page changes, automatically trigger video playback
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in shortsList.indices) {
            val video = shortsList[pagerState.currentPage]
            viewModel.playVideo(video)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = shortsList[page]
            ShortVideoItemView(
                video = video,
                isActive = pagerState.currentPage == page,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ShortVideoItemView(
    video: VideoItem,
    isActive: Boolean,
    viewModel: MusicViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = isActive && uiState.isVideoPlaying
    var showControls by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                showControls = !showControls
                viewModel.toggleVideoPlayPause()
            }
    ) {
        // Video View
        if (isActive && video.uri.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(Uri.parse(video.uri))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            if (uiState.isVideoPlaying) {
                                start()
                            }
                        }
                        setOnCompletionListener {
                            viewModel.onVideoCompleted(video.id)
                        }
                    }
                },
                update = { videoView ->
                    if (isPlaying) {
                        if (!videoView.isPlaying) videoView.start()
                    } else {
                        if (videoView.isPlaying) videoView.pause()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback preview canvas when not streaming
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E1035), Color(0xFF090A10))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Short Video",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        video.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Center pause/play indicator overlay
        AnimatedVisibility(
            visible = !isPlaying || showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        // Right side action bar (Like, Watch count, Duration)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { viewModel.toggleVideoFavorite(video) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("video_like_button_${video.id}")
                ) {
                    Icon(
                        if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White
                    )
                }
                Text(
                    if (video.isFavorite) "Liked" else "Like",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            // Watch Count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Views",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    "${video.playCount} views",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }

            // Duration badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
            ) {
                Text(
                    "${video.durationSeconds}s",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom title & artist metadata
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFF2A6D),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    "⚡ REEL SHORT (≤60s)",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = video.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = video.artist,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Watch time: ${video.totalWatchTimeSeconds}s • Completed: ${video.completionCount} times",
                color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                fontSize = 11.sp
            )
        }
    }
}

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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Player Card if a long video is selected
        if (currentVideo != null) {
            item {
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
                text = if (isFavoriteTab) "Favorite Videos" else "Video Library Catalog",
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
                            text = if (isFavoriteTab) "No favorite videos yet." else "No videos found on your device.",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isFavoriteTab) {
                                "Tap the heart icon on any video to add it to your favorites."
                            } else {
                                "Supported formats: MP4, MKV, AVI, MOV, WEBM and more. Make sure video files are stored on your device."
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
            items(videos) { video ->
                VideoListItemCard(
                    video = video,
                    isSelected = currentVideo?.id == video.id,
                    onClick = { viewModel.playVideo(video) },
                    onFavorite = { viewModel.toggleVideoFavorite(video) }
                )
            }
        }
    }
}

@Composable
fun ActiveLongVideoPlayerCard(
    video: VideoItem,
    isPlaying: Boolean,
    currentPos: Float,
    viewModel: MusicViewModel
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141721)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Player Window
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (video.uri.isNotBlank()) {
                    AndroidView(
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoURI(Uri.parse(video.uri))
                                setOnPreparedListener { mp ->
                                    if (isPlaying) start()
                                }
                                setOnCompletionListener {
                                    viewModel.onVideoCompleted(video.id)
                                }
                            }
                        },
                        update = { videoView ->
                            if (isPlaying) {
                                if (!videoView.isPlaying) videoView.start()
                            } else {
                                if (videoView.isPlaying) videoView.pause()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Movie,
                            contentDescription = "Video",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Resolution Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        video.resolution,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Controls & Information
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.artist,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleVideoFavorite(video) },
                        modifier = Modifier.testTag("active_video_fav_btn")
                    ) {
                        Icon(
                            if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress scrubber
                val duration = video.durationSeconds.toFloat().coerceAtLeast(1f)
                val safePos = currentPos.coerceIn(0f, duration)

                Slider(
                    value = safePos,
                    onValueChange = { viewModel.setVideoPosition(it) },
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

                Spacer(modifier = Modifier.height(10.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (currentPos - 10f).coerceAtLeast(0f)
                            viewModel.setVideoPosition(newPos)
                        }
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { viewModel.toggleVideoPlayPause() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF00E5FF), CircleShape)
                            .testTag("video_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = {
                            val newPos = (currentPos + 10f).coerceAtMost(duration)
                            viewModel.setVideoPosition(newPos)
                        }
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatsPill(label = "Views", value = "${video.playCount}")
                    StatsPill(label = "Watch Time", value = formatSeconds(video.totalWatchTimeSeconds.toInt()))
                    StatsPill(label = "Completed", value = "${video.completionCount}x")
                }
            }
        }
    }
}

@Composable
fun VideoListItemCard(
    video: VideoItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
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
                    if (video.isShort) Icons.Default.Videocam else Icons.Default.Movie,
                    contentDescription = null,
                    tint = if (video.isShort) Color(0xFFFF2A6D) else Color(0xFF00E5FF),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (video.isShort) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF2A6D),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                "SHORT",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${video.artist} • ${video.playCount} views • ${formatSeconds(video.totalWatchTimeSeconds.toInt())} watched",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onFavorite) {
                Icon(
                    if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatsPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.06f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = Color(0xFF00E5FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
