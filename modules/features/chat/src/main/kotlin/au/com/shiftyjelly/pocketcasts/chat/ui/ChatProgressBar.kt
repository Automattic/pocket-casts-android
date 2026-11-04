package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.chat.ChatPlayback
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ChatProgressBar(
    playback: ChatPlayback,
    onSeek: (positionMs: Int) -> Unit,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var seekProgress by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(seekProgress) {
        if (seekProgress != null) {
            delay(SEEK_HOLD_MS)
            seekProgress = null
        }
    }
    val currentPlayback by rememberUpdatedState(playback)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isSeekable = playback.isSeekable && playback.durationMs > 0
    val progress = dragProgress ?: seekProgress ?: playback.progress

    fun seekTo(fraction: Float) {
        seekProgress = fraction
        currentOnSeek((fraction * currentPlayback.durationMs).roundToInt())
    }

    fun fractionAt(x: Float, width: Int): Float {
        val fraction = (x / width).coerceIn(0f, 1f)
        return if (isRtl) 1f - fraction else fraction
    }

    val description = stringResource(LR.string.chat_playback_position)
    val seekModifier = if (isSeekable) {
        Modifier
            .pointerInput(isRtl) {
                detectTapGestures { offset -> seekTo(fractionAt(offset.x, size.width)) }
            }
            .pointerInput(isRtl) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragProgress = fractionAt(offset.x, size.width) },
                    onDragEnd = {
                        dragProgress?.let(::seekTo)
                        dragProgress = null
                    },
                    onDragCancel = { dragProgress = null },
                    onHorizontalDrag = { change, _ -> dragProgress = fractionAt(change.position.x, size.width) },
                )
            }
    } else {
        Modifier
    }
    val isDragging = dragProgress != null
    val trackColor = theme.progressTrack
    val progressColor = theme.progress
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TouchHeight)
            .then(seekModifier)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                if (isSeekable) {
                    setProgress { target ->
                        seekTo(target.coerceIn(0f, 1f))
                        true
                    }
                }
            }
            .drawBehind {
                val barHeight = BarHeight.toPx()
                val top = size.height - barHeight
                val radius = CornerRadius(barHeight / 2)
                drawRoundRect(color = trackColor, topLeft = Offset(0f, top), size = Size(size.width, barHeight))
                val progressWidth = size.width * progress
                val progressLeft = if (isRtl) size.width - progressWidth else 0f
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(progressLeft, top),
                    size = Size(progressWidth, barHeight),
                    cornerRadius = radius,
                )
                if (isDragging) {
                    val thumbX = if (isRtl) progressLeft else progressWidth
                    drawCircle(color = progressColor, radius = ThumbRadius.toPx(), center = Offset(thumbX, top + barHeight / 2))
                }
            },
    )
}

private val BarHeight = 2.dp
private val TouchHeight = 14.dp
private val ThumbRadius = 6.dp
private const val SEEK_HOLD_MS = 1_500L

@Preview
@Composable
private fun ChatProgressBarPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        CompositionLocalProvider(LocalPodcastColors provides PodcastColors.ForUserEpisode) {
            val theme = rememberChatTheme()
            ChatProgressBar(
                playback = ChatPlayback(positionMs = 30, durationMs = 100, isSeekable = true),
                onSeek = {},
                theme = theme,
                modifier = Modifier
                    .background(theme.background)
                    .padding(vertical = 16.dp),
            )
        }
    }
}
