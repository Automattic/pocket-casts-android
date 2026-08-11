package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography

@Composable
fun TvSeekBar(
    positionMs: Int,
    durationMs: Int,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasDuration = durationMs > 0
    val progress = if (hasDuration) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val trackHeight by animateDpAsState(if (isFocused) 8.dp else 4.dp, label = "TvSeekBarTrackHeight")

    Column(modifier = modifier) {
        BoxWithConstraints(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                }
                .onKeyEvent { event ->
                    when {
                        event.type != KeyEventType.KeyDown -> false

                        event.key == Key.DirectionCenter || event.key == Key.Enter -> {
                            if (event.nativeKeyEvent.repeatCount == 0) {
                                onPlayPause()
                            }
                            true
                        }

                        !hasDuration -> false

                        event.key == Key.DirectionLeft -> {
                            onSkipBack()
                            true
                        }

                        event.key == Key.DirectionRight -> {
                            onSkipForward()
                            true
                        }

                        else -> false
                    }
                }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .focusable(),
        ) {
            val thumbSize = 12.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.tvColors.backgroundActive20),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.tvColors.backgroundActive),
                )
            }
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .offset(x = (maxWidth - thumbSize) * progress)
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.tvColors.backgroundActive),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            SeekBarLabel(
                text = TimeHelper.formattedSeconds(positionMs / 1000.0),
                modifier = Modifier.align(Alignment.Center),
            )
            SeekBarLabel(
                text = if (hasDuration) TimeHelper.formattedSeconds(durationMs / 1000.0) else "-",
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun SeekBarLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.tvTypography.caption1,
        color = MaterialTheme.tvColors.textTertiary,
        modifier = modifier,
    )
}

@Preview(widthDp = 600)
@Composable
private fun TvSeekBarPreview() {
    TvTheme {
        TvSeekBar(
            positionMs = 600_000,
            durationMs = 3_600_000,
            onSkipBack = {},
            onSkipForward = {},
            onPlayPause = {},
        )
    }
}
