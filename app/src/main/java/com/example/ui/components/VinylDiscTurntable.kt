package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppTheme
import com.example.data.model.Song
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VinylDiscTurntable(
    song: Song?,
    isPlaying: Boolean,
    visualizerBand0: Float,
    playbackSpeed: Float,
    theme: AppTheme,
    onScratch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Smooth continuous rotation while playing
    LaunchedEffect(isPlaying, playbackSpeed) {
        var lastTime = System.nanoTime()
        while (isActive) {
            val now = System.nanoTime()
            val deltaSec = (now - lastTime) / 1_000_000_000f
            lastTime = now
            if (isPlaying) {
                // 33.3 RPM is approx 200 deg per second
                val speedFactor = 200f * playbackSpeed
                rotationAngle = (rotationAngle + speedFactor * deltaSec) % 360f
            }
            kotlinx.coroutines.delay(16L) // ~60fps
        }
    }

    // Tonearm smooth angle transition: resting ~0 deg, playing ~26 deg
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 26f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f),
        label = "tonearmAngle"
    )

    val primaryColor = Color(theme.primaryHex)
    val secondaryColor = Color(theme.secondaryHex)
    val labelColor = Color(theme.discLabelColorHex)
    val glowPulse = 0.85f + visualizerBand0 * 0.35f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient pulsing glow aura
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .testTag("vinyl_glow_aura")
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * glowPulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f * visualizerBand0.coerceIn(0.4f, 1f)),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }

        // 2. Turntable Platter Base with metallic edge
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1E28),
                            Color(0xFF12121A),
                            Color(0xFF09090E)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.8f),
                            secondaryColor.copy(alpha = 0.4f),
                            primaryColor.copy(alpha = 0.8f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 3. Spinning Vinyl Record
        Box(
            modifier = Modifier
                .fillMaxSize(0.88f)
                .rotate(rotationAngle)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF0D0D11))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        rotationAngle = (rotationAngle + 12f) % 360f
                        onScratch()
                    }
                }
                .testTag("spinning_vinyl_disc"),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Groove Rings and Specular Light Sheen Reflections
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f

                // Draw realistic vinyl groove rings
                val numGrooves = 28
                for (i in 1..numGrooves) {
                    val r = maxR * (0.36f + (i.toFloat() / numGrooves) * 0.60f)
                    val alpha = if (i % 3 == 0) 0.18f else 0.08f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        center = center,
                        radius = r,
                        style = Stroke(width = if (i % 5 == 0) 1.5f else 0.8f)
                    )
                }

                // Specular light reflection cones (vinyl shine)
                val sheenBrush1 = Brush.sweepGradient(
                    0.0f to Color.White.copy(alpha = 0.0f),
                    0.12f to Color.White.copy(alpha = 0.14f),
                    0.25f to Color.White.copy(alpha = 0.0f),
                    0.5f to Color.White.copy(alpha = 0.0f),
                    0.62f to Color.White.copy(alpha = 0.14f),
                    0.75f to Color.White.copy(alpha = 0.0f),
                    1.0f to Color.White.copy(alpha = 0.0f)
                )
                drawCircle(
                    brush = sheenBrush1,
                    center = center,
                    radius = maxR
                )

                // Neon edge accent ring
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    center = center,
                    radius = maxR * 0.98f,
                    style = Stroke(width = 2f)
                )
            }

            // Central Album Label (Cover Art + Center Spindle)
            Box(
                modifier = Modifier
                    .fillMaxSize(0.36f)
                    .clip(CircleShape)
                    .background(labelColor)
                    .border(3.dp, primaryColor.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Album cover art
                val coverDrawableId = when (song?.coverResName) {
                    "img_cover_cyber" -> R.drawable.img_cover_cyber
                    "img_cover_lofi" -> R.drawable.img_cover_lofi
                    "img_cover_cosmic" -> R.drawable.img_cover_cosmic
                    else -> R.drawable.img_cover_cyber
                }

                if (!song?.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = song?.albumArtUri,
                        contentDescription = "Album Cover",
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = coverDrawableId),
                        placeholder = painterResource(id = coverDrawableId),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = coverDrawableId),
                        contentDescription = "Album Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Center metallic spindle ring & hole
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                        .border(2.dp, Color(0xFF888888), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }

        // 4. Stylus Tonearm Overlay (Pivoting arm)
        ToneArm(
            angle = tonearmAngle,
            primaryColor = primaryColor,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
        )
    }
}

@Composable
fun ToneArm(
    angle: Float,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(140.dp)
            .testTag("turntable_tonearm"),
        contentAlignment = Alignment.TopEnd
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(angle)
        ) {
            val pivotX = size.width - 24.dp.toPx()
            val pivotY = 24.dp.toPx()
            val pivot = Offset(pivotX, pivotY)

            // 1. Pivot Base (Chunky metallic base)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF555566), Color(0xFF222230), Color(0xFF111118)),
                    center = pivot,
                    radius = 20.dp.toPx()
                ),
                center = pivot,
                radius = 20.dp.toPx()
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                center = pivot,
                radius = 12.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFCCCCCC),
                center = pivot,
                radius = 6.dp.toPx()
            )

            // 2. Tonearm Rod (Sleek curved chrome rod with stylus cartridge)
            val armPath = Path().apply {
                moveTo(pivotX, pivotY)
                lineTo(pivotX - 30.dp.toPx(), pivotY + 70.dp.toPx())
                lineTo(pivotX - 65.dp.toPx(), pivotY + 115.dp.toPx())
                lineTo(pivotX - 78.dp.toPx(), pivotY + 122.dp.toPx())
            }

            // Shadow
            drawPath(
                path = armPath,
                color = Color.Black.copy(alpha = 0.5f),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Silver chrome rod
            drawPath(
                path = armPath,
                color = Color(0xFFD5D5DF),
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Cartridge / Stylus Head
            val cartX = pivotX - 78.dp.toPx()
            val cartY = pivotY + 122.dp.toPx()

            rotate(degrees = -35f, pivot = Offset(cartX, cartY)) {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(cartX - 6.dp.toPx(), cartY - 4.dp.toPx()),
                    size = Size(16.dp.toPx(), 10.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Needle point
                drawCircle(
                    color = Color.White,
                    center = Offset(cartX - 4.dp.toPx(), cartY + 1.dp.toPx()),
                    radius = 1.5.dp.toPx()
                )
            }
        }
    }
}
