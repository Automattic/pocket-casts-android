package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintConstants
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.DebugMatchQuality
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager

private val ColorGreen = Color(0xFF4CAF50)
private val ColorYellow = Color(0xFFFFC107)
private val ColorRed = Color(0xFFF44336)
private val ColorUnmapped = Color(0xFF9E9E9E).copy(alpha = 0.3f)
private val ColorPlayhead = Color.White

@Composable
internal fun SyncDebugTimeline(
    fingerprintTimingManager: FingerprintTimingManager,
    playbackManager: PlaybackManager,
    modifier: Modifier = Modifier,
) {
    val playbackState by remember {
        playbackManager.playbackStateFlow
    }.collectAsState(initial = null)

    val positionMs = playbackState?.positionMs ?: return
    val durationMs = playbackState?.durationMs ?: return
    if (durationMs <= 0) return

    val debugEntries = fingerprintTimingManager.debugSnapshot
    val totalDurationSec = durationMs / 1000.0

    val stats = remember(debugEntries) {
        DebugTimelineStats(
            total = debugEntries.size,
            strong = debugEntries.count { it.quality == DebugMatchQuality.STRONG_MATCH },
            weak = debugEntries.count { it.quality == DebugMatchQuality.WEAK_MATCH },
            noMatch = debugEntries.count { it.quality == DebugMatchQuality.NO_MATCH },
        )
    }

    val currentQuality = remember(positionMs, debugEntries) {
        findQualityAtPosition(debugEntries, positionMs / 1000.0)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        DebugTimelineBar(
            entries = debugEntries,
            totalDurationSec = totalDurationSec,
            playheadFraction = positionMs / durationMs.toFloat(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val qualityText = when (currentQuality) {
                DebugMatchQuality.STRONG_MATCH -> "Strong"
                DebugMatchQuality.WEAK_MATCH -> "Weak"
                DebugMatchQuality.NO_MATCH -> "No match"
                null -> "—"
            }
            Text(
                text = "Quality: $qualityText",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "${stats.strong}G ${stats.weak}Y ${stats.noMatch}R / ${stats.total}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private data class DebugTimelineStats(
    val total: Int,
    val strong: Int,
    val weak: Int,
    val noMatch: Int,
)

@Composable
private fun DebugTimelineBar(
    entries: List<FingerprintTimingManager.DebugWindowResult>,
    totalDurationSec: Double,
    playheadFraction: Float,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        drawRect(color = ColorUnmapped, size = size)

        if (entries.isNotEmpty() && totalDurationSec > 0) {
            val windowDurationSec = FingerprintConstants.WINDOW_INTERVAL_MS / 1000.0

            for (entry in entries) {
                val color = qualityToColor(entry.quality)
                val startX = (entry.playbackTime / totalDurationSec).toFloat() * size.width
                val endX = ((entry.playbackTime + windowDurationSec) / totalDurationSec).toFloat() * size.width
                val segmentWidth = (endX - startX).coerceAtLeast(1f)

                drawRect(
                    color = color,
                    topLeft = Offset(startX.coerceIn(0f, size.width), 0f),
                    size = Size(
                        segmentWidth.coerceAtMost(size.width - startX.coerceAtLeast(0f)),
                        size.height,
                    ),
                )
            }
        }

        // Playhead indicator
        val playheadX = playheadFraction.coerceIn(0f, 1f) * size.width
        drawRect(
            color = ColorPlayhead,
            topLeft = Offset(playheadX - 1.dp.toPx(), 0f),
            size = Size(2.dp.toPx(), size.height),
        )
    }
}

private fun qualityToColor(quality: DebugMatchQuality): Color = when (quality) {
    DebugMatchQuality.STRONG_MATCH -> ColorGreen
    DebugMatchQuality.WEAK_MATCH -> ColorYellow
    DebugMatchQuality.NO_MATCH -> ColorRed
}

private fun findQualityAtPosition(
    entries: List<FingerprintTimingManager.DebugWindowResult>,
    positionSec: Double,
): DebugMatchQuality? {
    if (entries.isEmpty()) return null
    val windowDuration = FingerprintConstants.WINDOW_INTERVAL_MS / 1000.0
    var lo = 0
    var hi = entries.size - 1
    while (lo <= hi) {
        val mid = (lo + hi) / 2
        val entry = entries[mid]
        when {
            positionSec < entry.playbackTime -> hi = mid - 1
            positionSec >= entry.playbackTime + windowDuration -> lo = mid + 1
            else -> return entry.quality
        }
    }
    return null
}
