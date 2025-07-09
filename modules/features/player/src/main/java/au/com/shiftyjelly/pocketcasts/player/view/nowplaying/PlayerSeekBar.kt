package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.player.view.PlayerSeekBar
import kotlin.time.Duration

@Composable
internal fun PlayerSeekBar(
    playbackPosition: Duration,
    playbackDuration: Duration,
    adjustPlaybackDuration: Boolean,
    playbackSpeed: Double,
    chapters: Chapters,
    isBuffering: Boolean,
    bufferedUpTo: Duration,
    playerColors: PlayerColors,
    onSeekToPosition: (Duration, onSeekComplete: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = MaterialTheme.theme.type

    AndroidView(
        factory = { context ->
            PlayerSeekBar(context).apply {
                changeListener = object : PlayerSeekBar.OnUserSeekListener {
                    override fun onSeekPositionChangeStop(progress: Duration, seekComplete: () -> Unit) {
                        onSeekToPosition(progress, seekComplete)
                    }

                    override fun onSeekPositionChanging(progress: Duration) = Unit

                    override fun onSeekPositionChangeStart() = Unit
                }
            }
        },
        update = { seekBar ->
            seekBar.apply {
                setTintColor(playerColors.highlight01.toArgb(), theme)
                setDuration(playbackDuration)
                setAdjustDuration(adjustPlaybackDuration)
                setPlaybackSpeed(playbackSpeed)
                setChapters(chapters)
                this.isBuffering = isBuffering
                bufferedUpToInSecs = bufferedUpTo.inWholeSeconds.toInt()
                setCurrentTime(playbackPosition)
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}
