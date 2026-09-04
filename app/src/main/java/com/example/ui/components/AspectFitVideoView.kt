package com.example.ui.components

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * AspectFitVideoView:
 * Custom VideoView that strictly enforces "fit/contain" behavior.
 * Preserves the original aspect ratio (9:16 portrait, 16:9 landscape, 4:3, 1:1 square, etc.)
 * Never stretches, crops, or distorts the video surface.
 */
class AspectFitVideoView(context: Context) : VideoView(context) {
    private var videoW = 0
    private var videoH = 0

    fun updateVideoSize(w: Int, h: Int) {
        if (w > 0 && h > 0 && (w != videoW || h != videoH)) {
            videoW = w
            videoH = h
            requestLayout()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (videoW > 0 && videoH > 0 && widthSize > 0 && heightSize > 0) {
            val videoRatio = videoW.toFloat() / videoH.toFloat()
            val parentRatio = widthSize.toFloat() / heightSize.toFloat()

            var measuredWidth = widthSize
            var measuredHeight = heightSize

            if (videoRatio > parentRatio) {
                // Video is wider than container: fit width, pillarbox/letterbox top & bottom
                measuredWidth = widthSize
                measuredHeight = (widthSize / videoRatio).toInt().coerceAtMost(heightSize)
            } else {
                // Video is taller than container: fit height, pillarbox/letterbox left & right
                measuredHeight = heightSize
                measuredWidth = (heightSize * videoRatio).toInt().coerceAtMost(widthSize)
            }
            setMeasuredDimension(measuredWidth, measuredHeight)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val parentWidth = r - l
        val parentHeight = b - t
        if (videoW > 0 && videoH > 0 && parentWidth > 0 && parentHeight > 0) {
            val videoRatio = videoW.toFloat() / videoH.toFloat()
            val parentRatio = parentWidth.toFloat() / parentHeight.toFloat()
            val actualW: Int
            val actualH: Int
            if (videoRatio > parentRatio) {
                actualW = parentWidth
                actualH = (parentWidth / videoRatio).toInt().coerceAtMost(parentHeight)
            } else {
                actualH = parentHeight
                actualW = (parentHeight * videoRatio).toInt().coerceAtMost(parentWidth)
            }
            val left = l + (parentWidth - actualW) / 2
            val top = t + (parentHeight - actualH) / 2
            super.layout(left, top, left + actualW, top + actualH)
        } else {
            super.layout(l, t, r, b)
        }
    }
}

/**
 * Helpers for extracting and calculating exact video dimensions and aspect ratios.
 */
object VideoDimensionHelper {
    /**
     * Parses width/height aspect ratio from formatted strings like "1080x1920", "1920x1080", "1280x720".
     */
    fun parseRatio(resolution: String?): Float? {
        if (resolution.isNullOrBlank() || !resolution.contains("x")) return null
        val parts = resolution.split("x")
        if (parts.size != 2) return null
        val w = parts[0].trim().toFloatOrNull() ?: return null
        val h = parts[1].trim().toFloatOrNull() ?: return null
        return if (w > 0f && h > 0f) w / h else null
    }

    /**
     * Inspects the video URI via MediaMetadataRetriever to extract true display dimensions,
     * taking 90°/270° orientation rotation metadata into account.
     */
    fun getDisplayDimensions(context: Context, uriString: String): Pair<Int, Int>? {
        if (uriString.isBlank()) return null
        var retriever: MediaMetadataRetriever? = null
        return try {
            retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(uriString))
            val wStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val hStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val w = wStr?.toIntOrNull() ?: 0
            val h = hStr?.toIntOrNull() ?: 0
            val rot = rotStr?.toIntOrNull() ?: 0
            if (w > 0 && h > 0) {
                // If rotated 90 or 270 degrees, display width is buffer height and display height is buffer width
                if (rot == 90 || rot == 270) {
                    Pair(h, w)
                } else {
                    Pair(w, h)
                }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Calculates the exact aspect-fit dimensions within available container bounds.
     */
    fun calculateAspectFitSize(videoRatio: Float?, containerW: Dp, containerH: Dp): Pair<Dp, Dp> {
        if (videoRatio == null || videoRatio <= 0f || containerW.value <= 0f || containerH.value <= 0f) {
            return Pair(containerW, containerH)
        }
        val containerRatio = containerW.value / containerH.value
        return if (videoRatio > containerRatio) {
            // Video is wider than container: fit container width, reduce height
            val fitH = (containerW.value / videoRatio).dp.coerceAtMost(containerH)
            Pair(containerW, fitH)
        } else {
            // Video is taller than container: fit container height, reduce width
            val fitW = (containerH.value * videoRatio).dp.coerceAtMost(containerW)
            Pair(fitW, containerH)
        }
    }
}

/**
 * AspectFitVideoContainer:
 * Responsive Jetpack Compose container that measures the available area and sizes
 * the child video view to maintain its original proportions without stretching or cropping.
 */
@Composable
fun AspectFitVideoContainer(
    videoRatio: Float?,
    modifier: Modifier = Modifier,
    content: @Composable (fitWidth: Dp, fitHeight: Dp) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val (fitWidth, fitHeight) = remember(videoRatio, maxWidth, maxHeight) {
            VideoDimensionHelper.calculateAspectFitSize(videoRatio, maxWidth, maxHeight)
        }
        content(fitWidth, fitHeight)
    }
}
