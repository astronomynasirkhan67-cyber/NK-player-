package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppTheme
import com.example.data.model.Song

data class LyricLine(
    val timestampSeconds: Int,
    val text: String
)

@Composable
fun LyricsView(
    song: Song?,
    currentPositionSec: Float,
    theme: AppTheme,
    onSeekTo: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = remember(song?.lyrics) {
        parseLyrics(song?.lyrics ?: "")
    }

    val activeIndex = remember(lines, currentPositionSec) {
        var idx = 0
        for (i in lines.indices) {
            if (currentPositionSec >= lines[i].timestampSeconds) {
                idx = i
            } else {
                break
            }
        }
        idx
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    val primaryColor = Color(theme.primaryHex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("lyrics_view_container")
    ) {
        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No lyrics available for this track",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lines) { index, item ->
                    val isActive = index == activeIndex
                    Card(
                        onClick = { onSeekTo(item.timestampSeconds.toFloat()) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) primaryColor.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .testTag("lyric_line_$index")
                    ) {
                        Text(
                            text = item.text,
                            textAlign = TextAlign.Center,
                            fontSize = if (isActive) 19.sp else 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun parseLyrics(raw: String): List<LyricLine> {
    if (raw.isBlank()) return emptyList()
    val list = mutableListOf<LyricLine>()
    val lines = raw.lines()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            val closeIdx = trimmed.indexOf("]")
            val timePart = trimmed.substring(1, closeIdx)
            val textPart = trimmed.substring(closeIdx + 1).trim()
            val timeParts = timePart.split(":")
            if (timeParts.size == 2) {
                val mins = timeParts[0].toIntOrNull() ?: 0
                val secs = timeParts[1].toIntOrNull() ?: 0
                list.add(LyricLine(mins * 60 + secs, textPart))
            }
        } else if (trimmed.isNotBlank()) {
            list.add(LyricLine(list.size * 15, trimmed))
        }
    }
    return list
}
