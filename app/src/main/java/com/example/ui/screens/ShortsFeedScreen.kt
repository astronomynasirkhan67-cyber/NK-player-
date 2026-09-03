package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.VideoItem
import com.example.ui.viewmodel.MusicViewModel

/**
 * Custom VideoView that scales edge-to-edge to occupy the full container,
 * providing the modern vertical fullscreen short-video presentation without black bars.
 */
class ScaledVideoView(context: Context) : VideoView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(0, widthMeasureSpec)
        val height = getDefaultSize(0, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

/**
 * Pure Fullscreen Vertical Shorts / Reels Feed.
 *
 * Strict Constraints:
 * - NO top header, search bar, or tabs
 * - NO like, views, comments, share, heart buttons or counts
 * - NO video title, filename, "Device Video", duration badge, or watch time
 * - ONLY the fullscreen short video itself
 * - Strictly filters for videos with duration <= 60 seconds
 * - Real device media only (no sample or demo videos)
 * - Automatic playback from beginning on swipe
 * - Immediate resource release on page change
 */
@Composable
fun ShortsFeedScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val rawShorts by viewModel.shorts.collectAsState()

    // Strictly enforce duration <= 60 seconds
    val validShorts = remember(rawShorts) {
        rawShorts.filter { it.durationSeconds in 1..60 }
    }

    // Stop playback when leaving the Shorts screen
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
            // Clean empty state when no shorts (<= 60s) exist on device
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
                        text = "Short videos (60 seconds or less) stored on your device will automatically appear here in vertical playback.",
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { validShorts.size })

            // When user swipes to a new Short, automatically play it from the beginning
            LaunchedEffect(pagerState.currentPage, validShorts) {
                if (pagerState.currentPage in validShorts.indices) {
                    val currentVideo = validShorts[pagerState.currentPage]
                    viewModel.playVideo(currentVideo)
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> validShorts.getOrNull(page)?.id ?: page }
            ) { page ->
                val video = validShorts[page]
                val isActive = pagerState.currentPage == page

                ShortVideoPage(
                    video = video,
                    isActive = isActive,
                    viewModel = viewModel
                )
            }
        }
    }
}

/**
 * Individual Short Video Item:
 * Strictly the video itself, full screen, edge to edge.
 * Tapping pauses/resumes playback.
 */
@Composable
private fun ShortVideoPage(
    video: VideoItem,
    isActive: Boolean,
    viewModel: MusicViewModel
) {
    var isUserPaused by remember(video.id) { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<ScaledVideoView?>(null) }

    // When page is swiped away, release underlying playback resources immediately
    DisposableEffect(video.id, isActive) {
        if (!isActive) {
            videoViewRef?.stopPlayback()
        }
        onDispose {
            videoViewRef?.stopPlayback()
            videoViewRef = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                videoViewRef?.let { vv ->
                    if (vv.isPlaying) {
                        vv.pause()
                        isUserPaused = true
                        viewModel.pauseVideo()
                    } else {
                        vv.start()
                        isUserPaused = false
                        viewModel.resumeVideo()
                    }
                }
            }
    ) {
        if (isActive && video.uri.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    ScaledVideoView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoURI(Uri.parse(video.uri))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            seekTo(0)
                            if (!isUserPaused) {
                                start()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            // Handle media errors gracefully without crashing or showing system dialogs
                            true
                        }
                        setOnCompletionListener {
                            viewModel.onVideoCompleted(video.id)
                            seekTo(0)
                            if (!isUserPaused) {
                                start()
                            }
                        }
                        videoViewRef = this
                    }
                },
                update = { view ->
                    videoViewRef = view
                    if (isActive && !isUserPaused) {
                        if (!view.isPlaying) {
                            view.start()
                        }
                    } else {
                        if (view.isPlaying) {
                            view.pause()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
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
    }
}
