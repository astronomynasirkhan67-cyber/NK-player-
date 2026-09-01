package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AppTheme

@Composable
fun AudioSpectrumVisualizer(
    bands: FloatArray,
    isPlaying: Boolean,
    theme: AppTheme,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(theme.primaryHex)
    val secondaryColor = Color(theme.secondaryHex)
    val accentColor = Color(theme.accentHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("audio_spectrum_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val numBands = bands.size.coerceAtLeast(8)
            val barSpacing = 4.dp.toPx()
            val totalSpacing = barSpacing * (numBands - 1)
            val barWidth = ((size.width - totalSpacing) / numBands).coerceAtLeast(2.dp.toPx())
            val maxHeight = size.height

            for (i in 0 until numBands) {
                val rawVal = if (isPlaying) bands[i] else 0.06f
                val bandHeight = (rawVal.coerceIn(0.05f, 1.0f) * maxHeight).coerceAtLeast(4.dp.toPx())
                val x = i * (barWidth + barSpacing)
                val y = maxHeight - bandHeight

                // Draw Bar Gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            primaryColor,
                            secondaryColor.copy(alpha = 0.5f)
                        ),
                        startY = y,
                        endY = maxHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, bandHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                // Glowing peak dot
                if (isPlaying && rawVal > 0.3f) {
                    drawCircle(
                        color = Color.White,
                        center = Offset(x + barWidth / 2f, y),
                        radius = (barWidth * 0.45f).coerceIn(1.5.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }
    }
}
