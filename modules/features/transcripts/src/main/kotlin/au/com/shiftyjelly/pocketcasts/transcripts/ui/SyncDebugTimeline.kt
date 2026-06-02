package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintConstants
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager

// Color legend for the debug timeline bar:
// - Green: fingerprint window matched the reference — transcript timing is trustworthy here.
// - Red: fingerprint window passed the minimum score floor but was rejected by the drift filter
//   (score too low for anchoring, or not enough dominance over the runner-up).
// - Dark gray (backdrop): no fingerprint data yet, or the window scored below the floor
//   and was silently discarded (not enough signal to classify either way).
// - White cursor: current playback position.
private val ColorGreen = Color(0xFF4CAF50)
private val ColorRed = Color(0xFFF44336)
private val ColorBackdrop = Color.DarkGray.copy(alpha = 0.5f)
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

    val matcherState by fingerprintTimingManager.stateFlow.collectAsState()

    val positionMs = playbackState?.positionMs ?: return
    val durationMs = playbackState?.durationMs ?: return
    if (durationMs <= 0) return

    val acceptedEntries = fingerprintTimingManager.mappingSnapshot
    val rejectedEntries = fingerprintTimingManager.debugRejectionsSnapshot
    val totalDurationSec = durationMs / 1000.0

    val statusText = remember(matcherState) {
        when (val s = matcherState) {
            is FingerprintTimingManager.State.Idle -> "idle"
            is FingerprintTimingManager.State.Preparing -> "preparing"
            is FingerprintTimingManager.State.Active -> "active (${s.coverage})"
            is FingerprintTimingManager.State.Failed -> "failed"
            is FingerprintTimingManager.State.Unavailable -> "unavailable"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        DebugTimelineBar(
            acceptedEntries = acceptedEntries,
            rejectedEntries = rejectedEntries,
            totalDurationSec = totalDurationSec,
            playheadFraction = positionMs / durationMs.toFloat(),
            onTapFraction = { fraction ->
                val seekMs = (fraction * durationMs).toInt()
                playbackManager.seekToTimeMs(seekMs)
            },
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statusText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun DebugTimelineBar(
    acceptedEntries: List<FingerprintTimingManager.TimeMappingEntry>,
    rejectedEntries: List<FingerprintTimingManager.DebugRejection>,
    totalDurationSec: Double,
    playheadFraction: Float,
    onTapFraction: (Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onTapFraction(fraction)
                }
            },
    ) {
        // 1. Dark gray backdrop
        drawRect(color = ColorBackdrop, size = size)

        if (totalDurationSec > 0) {
            // 2. Red rejection ticks
            drawRejectionTicks(rejectedEntries, totalDurationSec)

            // 3. Green accepted segments (drawn on top of red)
            drawAcceptedSegments(acceptedEntries, totalDurationSec)
        }

        // 4. White playback cursor
        val cursorX = playheadFraction.coerceIn(0f, 1f) * size.width
        drawRect(
            color = ColorPlayhead,
            topLeft = Offset(cursorX - 1.dp.toPx(), 0f),
            size = Size(2.dp.toPx(), size.height),
        )
    }
}

private fun DrawScope.drawRejectionTicks(
    rejections: List<FingerprintTimingManager.DebugRejection>,
    totalDurationSec: Double,
) {
    val minTickWidth = 1.5.dp.toPx()
    for (entry in rejections) {
        val x = (entry.playbackTime / totalDurationSec).toFloat() * size.width
        val tickWidth = ((1.0 / totalDurationSec).toFloat() * size.width).coerceAtLeast(minTickWidth)
        drawRect(
            color = ColorRed,
            topLeft = Offset(x.coerceIn(0f, size.width - tickWidth), 0f),
            size = Size(tickWidth, size.height),
        )
    }
}

private fun DrawScope.drawAcceptedSegments(
    entries: List<FingerprintTimingManager.TimeMappingEntry>,
    totalDurationSec: Double,
) {
    val windowDurationSec = FingerprintConstants.WINDOW_INTERVAL_MS / 1000.0
    val minSegmentWidth = 2.dp.toPx()
    for (entry in entries) {
        val startX = (entry.playbackTime / totalDurationSec).toFloat() * size.width
        val endX = ((entry.playbackTime + windowDurationSec) / totalDurationSec).toFloat() * size.width
        val segmentWidth = (endX - startX).coerceAtLeast(minSegmentWidth)
        val clampedStartX = startX.coerceIn(0f, size.width)
        drawRect(
            color = ColorGreen,
            topLeft = Offset(clampedStartX, 0f),
            size = Size(segmentWidth.coerceAtMost(size.width - clampedStartX), size.height),
        )
    }
}
