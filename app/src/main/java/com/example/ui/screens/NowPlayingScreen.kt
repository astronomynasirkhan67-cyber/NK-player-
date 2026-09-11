package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import com.example.data.model.Song
import com.example.ui.components.AudioSpectrumVisualizer
import com.example.ui.components.LyricsView
import com.example.ui.components.VinylDiscTurntable
import com.example.ui.viewmodel.MusicUiState
import com.example.ui.viewmodel.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    uiState: MusicUiState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onScratch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onToggleLyrics: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onMoreClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val song = uiState.currentSong
    val primaryColor = Color(uiState.currentTheme.primaryHex)
    val secondaryColor = Color(uiState.currentTheme.secondaryHex)

    var showSpeedSheet by remember { mutableStateOf(false) }
    var showTimerSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(uiState.currentTheme.backgroundHex),
                        Color(uiState.currentTheme.surfaceHex).copy(alpha = 0.8f),
                        Color(uiState.currentTheme.backgroundHex)
                    )
                )
            )
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "NK PLAYER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = primaryColor
                )
                Text(
                    text = if (uiState.showLyrics) "Synchronized Lyrics" else "Vinyl Turntable Mode",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Lyrics Toggle
                IconButton(
                    onClick = onToggleLyrics,
                    modifier = Modifier.testTag("toggle_lyrics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (uiState.showLyrics) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Equalizer Button
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("open_equalizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sleep timer button
                IconButton(
                    onClick = { showTimerSheet = true },
                    modifier = Modifier.testTag("sleep_timer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AvTimer,
                        contentDescription = "Sleep Timer",
                        tint = if (uiState.sleepTimerMinutesLeft != null) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Sleep Timer badge if running
        if (uiState.sleepTimerMinutesLeft != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .clickable { onSetSleepTimer(null) }
            ) {
                Text(
                    text = "⏳ Sleep Timer: ${uiState.sleepTimerMinutesLeft}m remaining (tap to cancel)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Main Turntable or Lyrics View
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(340.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.showLyrics) {
                LyricsView(
                    song = song,
                    currentPositionSec = uiState.currentPositionSec,
                    theme = uiState.currentTheme,
                    onSeekTo = onSeekTo
                )
            } else {
                VinylDiscTurntable(
                    song = song,
                    isPlaying = uiState.isPlaying,
                    visualizerBand0 = uiState.visualizerBands.firstOrNull() ?: 0.2f,
                    playbackSpeed = uiState.playbackSpeed,
                    theme = uiState.currentTheme,
                    onScratch = onScratch
                )
            }
        }

        // Audio Spectrum Visualizer
        AudioSpectrumVisualizer(
            bands = uiState.visualizerBands,
            isPlaying = uiState.isPlaying,
            theme = uiState.currentTheme
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Song Title, Artist, Play Count Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song?.title ?: "Select a Track",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${song?.artist ?: "NK Player"} • ${song?.album ?: ""}",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Add to playlist
                if (song != null) {
                    IconButton(
                        onClick = { onAddToPlaylist(song) },
                        modifier = Modifier.testTag("add_to_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Favorite button
                if (song != null) {
                    IconButton(
                        onClick = { onToggleFavorite(song) },
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3-Dot Options menu (Rename, Share, Move, Delete)
                if (song != null && onMoreClick != null) {
                    IconButton(
                        onClick = { onMoreClick(song) },
                        modifier = Modifier.testTag("now_playing_more_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Song options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Play count & Genre Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Most played badge
            Card(
                colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Plays",
                        tint = primaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${song?.playCount ?: 0} plays",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            // Genre pill
            song?.genre?.let { genre ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = genre,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // RPM / Speed Chip
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { showSpeedSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = secondaryColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${uiState.playbackSpeed}x RPM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Slider
        val totalSec = (song?.durationSeconds ?: 1).toFloat()
        Slider(
            value = uiState.currentPositionSec.coerceIn(0f, totalSec),
            onValueChange = { onSeekTo(it) },
            valueRange = 0f..totalSec,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = primaryColor.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .testTag("playback_seek_slider")
        )

        // Time indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(uiState.currentPositionSec.toInt()),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(song?.durationSeconds ?: 0),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.testTag("shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (uiState.isShuffle) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Play / Pause glowing center button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = primaryColor)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(primaryColor, secondaryColor)
                        )
                    )
                    .clickable(onClick = onTogglePlayPause)
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.testTag("repeat_button")
            ) {
                val icon = when (uiState.repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                val tint = when (uiState.repeatMode) {
                    RepeatMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    else -> primaryColor
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint
                )
            }
        }
    }

    // Playback Speed Bottom Sheet
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Vinyl Turntable RPM Speed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryColor
                )
                Text(
                    text = "Adjust pitch and tempo just like an authentic vinyl deck:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    speeds.forEach { speed ->
                        val isSelected = uiState.playbackSpeed == speed
                        Card(
                            onClick = {
                                onSetSpeed(speed)
                                showSpeedSheet = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${speed}x",
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Sleep Timer Bottom Sheet
    if (showTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTimerSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Music Sleep Timer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryColor
                )
                Text(
                    text = "Music playback will automatically stop after chosen duration:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val timerOptions = listOf(15, 30, 45, 60, 90)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    timerOptions.forEach { mins ->
                        Card(
                            onClick = {
                                onSetSleepTimer(mins)
                                showTimerSheet = false
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${mins}m",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (uiState.sleepTimerMinutesLeft != null) {
                    TextButton(
                        onClick = {
                            onSetSleepTimer(null)
                            showTimerSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn Off Sleep Timer", color = Color(0xFFFF5252))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
