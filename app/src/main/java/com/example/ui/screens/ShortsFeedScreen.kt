package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.MediaTarget
import com.example.data.model.VideoItem
import com.example.ui.components.AspectFitVideoContainer
import com.example.ui.components.AspectFitVideoView
import com.example.ui.components.VideoDimensionHelper
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pure Vertical Shorts / Reels Feed for NK Player.
 *
 * Requirements:
 * - Preserves scroll position & last-viewed short across tab navigation and switches
 * - Toggleable Auto-Scroll feature: "Auto Scroll: ON" / "Auto Scroll: OFF" with intelligent timing
 * - Immediate clean stop of playback and resources on exit / swipe to avoid duplicate audio
 * - Interactive Mute / Unmute toggle for audio control
 * - Right-side floating controls: ❤️ Favorite, 👁️ Views Count, and ⋮ More Options
 * - Strictly filters for videos with duration <= 90 seconds (1 minute 30 seconds)
 * - Original aspect ratio strictly preserved (fit/contain, never stretched or cropped)
 */
@Composable
fun ShortsFeedScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val rawShorts by viewModel.shorts.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Strictly enforce duration <= 90 seconds (1 minute 30 seconds)
    val validShorts = remember(rawShorts, uiState.shortsThresholdSeconds) {
        rawShorts.filter { it.durationSeconds in 1..uiState.shortsThresholdSeconds }
    }

    // Stop playback and cancel any pending auto-scroll when leaving the Shorts screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.pauseVideo()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_fullscreen_feed")
    ) {
        if (validShorts.isEmpty()) {
            // Clean empty state when no shorts exist on device
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Shorts found on your device.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Short videos (90 seconds or less) stored on your device will automatically appear here in vertical playback.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.scanLocalMedia(silent = false) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("shorts_scan_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Scan Device Storage",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // Restore scroll position: find last viewed index or default to saved index
            val initialPage = remember(validShorts) {
                val targetId = uiState.lastViewedShortId
                val indexById = if (targetId != null) {
                    validShorts.indexOfFirst { it.id == targetId }.takeIf { it >= 0 }
                } else null
                (indexById ?: uiState.shortsCurrentIndex).coerceIn(0, validShorts.size - 1)
            }

            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { validShorts.size }
            )

            val coroutineScope = rememberCoroutineScope()

            // Track user paused state per active short
            var isCurrentShortPaused by remember { mutableStateOf(false) }

            // Guard/Lock mechanism: ensures one completed video produces exactly one automatic transition
            var isAutoScrollLocked by remember { mutableStateOf(false) }
            var autoScrollJob by remember { mutableStateOf<Job?>(null) }

            // Sync active page with ViewModel state to preserve location across tabs
            LaunchedEffect(pagerState.currentPage) {
                // Reset transition lock for the new video
                isAutoScrollLocked = false
                autoScrollJob?.cancel()
                autoScrollJob = null
                isCurrentShortPaused = false

                val activeVideo = validShorts.getOrNull(pagerState.currentPage)
                if (activeVideo != null) {
                    viewModel.setShortsCurrentIndex(pagerState.currentPage, activeVideo.id)
                    viewModel.playVideo(activeVideo)
                }
            }

            // Immediately cancel any pending auto-scroll when user turns Auto Scroll OFF
            LaunchedEffect(uiState.isShortsAutoScrollEnabled) {
                if (!uiState.isShortsAutoScrollEnabled) {
                    autoScrollJob?.cancel()
                    autoScrollJob = null
                    isAutoScrollLocked = false
                }
            }

            // Strictly video-completion based auto-scroll handler.
            // NO repeating timers, NO duration estimations:
            // Only a genuine end-of-playback completion callback triggers moving to the next item.
            val handleVideoCompletion: (String) -> Unit = { completedVideoId ->
                val currentVideo = validShorts.getOrNull(pagerState.currentPage)
                if (uiState.isShortsAutoScrollEnabled &&
                    !isCurrentShortPaused &&
                    currentVideo != null &&
                    currentVideo.id == completedVideoId &&
                    validShorts.size > 1 &&
                    !isAutoScrollLocked &&
                    !pagerState.isScrollInProgress
                ) {
                    // Transition lock engaged immediately to prevent duplicate scroll calls
                    isAutoScrollLocked = true
                    autoScrollJob?.cancel()
                    autoScrollJob = coroutineScope.launch {
                        try {
                            // Brief 200ms natural breathing delay after complete end-of-media before moving
                            delay(200L)
                            if (isActive && uiState.isShortsAutoScrollEnabled && !isCurrentShortPaused) {
                                val nextPage = (pagerState.currentPage + 1) % validShorts.size
                                pagerState.animateScrollToPage(
                                    page = nextPage,
                                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                )
                            }
                        } finally {
                            if (!uiState.isShortsAutoScrollEnabled) {
                                isAutoScrollLocked = false
                            }
                        }
                    }
                }
            }

            // Clean up any pending transition if Shorts screen unmounts / user navigates away
            DisposableEffect(Unit) {
                onDispose {
                    autoScrollJob?.cancel()
                    autoScrollJob = null
                    isAutoScrollLocked = false
                }
            }

            // Vertical Pager
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("shorts_vertical_pager"),
                beyondViewportPageCount = 0,
                key = { page -> validShorts.getOrNull(page)?.id ?: page }
            ) { page ->
                val video = validShorts[page]
                val isActive = page == pagerState.currentPage

                ShortVideoPage(
                    video = video,
                    isActive = isActive,
                    isMuted = uiState.isShortsMuted,
                    canAutoAdvance = uiState.isShortsAutoScrollEnabled && validShorts.size > 1,
                    viewModel = viewModel,
                    isUserPaused = if (isActive) isCurrentShortPaused else false,
                    onToggleUserPause = {
                        if (isActive) {
                            isCurrentShortPaused = !isCurrentShortPaused
                        }
                    },
                    onVideoCompleted = { completedId ->
                        handleVideoCompletion(completedId)
                    }
                )
            }

            // Sleek, Non-Intrusive Top Overlay:
            // 1. Position badge: Shorts X/Y
            // 2. Mute / Unmute quick toggle
            // 3. Auto Scroll toggle: Arrow-style ICON ONLY (NO text displayed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Short index counter badge
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Black.copy(alpha = 0.60f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Shorts ${pagerState.currentPage + 1}/${validShorts.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute Button
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isShortsMuted) Color(0xFFFF5252).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.60f),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isShortsMuted) Color(0xFFFF5252) else Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .clickable { viewModel.toggleShortsMute() }
                            .testTag("shorts_mute_toggle")
                    ) {
                        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isShortsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (uiState.isShortsMuted) "Unmute Audio" else "Mute Audio",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Auto Scroll Toggle Button (ICON ONLY - Arrow style, NO text)
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.isShortsAutoScrollEnabled) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.60f),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isShortsAutoScrollEnabled) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .clickable { viewModel.toggleShortsAutoScroll() }
                            .testTag("shorts_auto_scroll_toggle")
                    ) {
                        Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (uiState.isShortsAutoScrollEnabled) {
                                    Icons.Default.KeyboardDoubleArrowDown
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (uiState.isShortsAutoScrollEnabled) "Turn Auto Scroll Off" else "Turn Auto Scroll On",
                                tint = if (uiState.isShortsAutoScrollEnabled) Color.Black else Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Short Video Item:
 * Fits the original video without stretching or cropping (fit/contain).
 * Preserves exact aspect ratio (portrait, landscape, square) with letterboxing/pillarboxing.
 * Clean audio management respecting mute preference and stopping cleanly on exit.
 * Features restored ❤️ Favorite, 👁️ Views count, and ⋮ More options.
 */
@Composable
private fun ShortVideoPage(
    video: VideoItem,
    isActive: Boolean,
    isMuted: Boolean,
    canAutoAdvance: Boolean,
    viewModel: MusicViewModel,
    isUserPaused: Boolean,
    onToggleUserPause: () -> Unit,
    onVideoCompleted: (String) -> Unit
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<AspectFitVideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var videoRatio by remember(video.id) {
        mutableStateOf<Float?>(VideoDimensionHelper.parseRatio(video.resolution))
    }

    // Inspect video file metadata to ensure rotation (90/270 degrees) and display dimensions are exact
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

    // Handle mute / unmute dynamics
    LaunchedEffect(isMuted) {
        try {
            val vol = if (isMuted) 0f else 1f
            mediaPlayerRef?.setVolume(vol, vol)
        } catch (_: Exception) {}
    }

    // When page is swiped away or unmounted, release underlying playback resources immediately
    DisposableEffect(video.id, isActive) {
        if (!isActive) {
            videoViewRef?.stopPlayback()
            mediaPlayerRef = null
        }
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
            mediaPlayerRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                onToggleUserPause()
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        vv.pause()
                        viewModel.pauseVideo()
                    } else {
                        vv.start()
                        viewModel.resumeVideo()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Aspect-fit Video Container: strict contain/fit behavior, never stretches, never crops
        AspectFitVideoContainer(
            videoRatio = videoRatio,
            modifier = Modifier.fillMaxSize()
        ) { fitWidth, fitHeight ->
            if (isActive && video.uri.isNotBlank()) {
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
                                val vol = if (isMuted) 0f else 1f
                                mp.setVolume(vol, vol)
                                mp.isLooping = false
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
                                seekTo(0)
                                if (!isUserPaused) {
                                    start()
                                }
                            }
                            setOnErrorListener { _, _, _ ->
                                // Handle media errors gracefully without crashing
                                true
                            }
                            setOnCompletionListener {
                                viewModel.onVideoCompleted(video.id)
                                if (isActive) {
                                    onVideoCompleted(video.id)
                                }
                                if (!canAutoAdvance || !isActive) {
                                    seekTo(0)
                                    if (!isUserPaused) {
                                        start()
                                    }
                                }
                            }
                            videoViewRef = this
                        }
                    },
                    update = { view ->
                        videoViewRef = view
                        if (!isActive && view.isPlaying) {
                            view.pause()
                        }
                    },
                    modifier = Modifier.size(fitWidth, fitHeight)
                )
            }
        }

        // Subtle play icon indicator only when user explicitly tapped to pause
        AnimatedVisibility(
            visible = isUserPaused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Right-Side Floating Controls: ❤️ Favorite, 👁️ Views Count, and ⋮ More Options
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ❤️ Favorite / Heart Button (functional toggle)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { viewModel.toggleVideoFavorite(video) }
                    .testTag("shorts_favorite_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (video.isFavorite) Color(0xFFFF2A6D) else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // 👁️ Views / View Count (functional view count tracking)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.testTag("shorts_views_indicator")
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Views",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.playCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ⋮ Three-Dot Options Menu (Rename, Share, Move, Delete)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { viewModel.openMediaMenu(MediaTarget.VideoMedia(video)) }
                    .testTag("shorts_more_menu_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "More",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
