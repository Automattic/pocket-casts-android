package au.com.shiftyjelly.pocketcasts.nowplaying

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.repositories.playback.SimplePlayer
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB6B6B6)
private val TrackBackground = Color(0x33FFFFFF)
private val TrackBuffered = Color(0x55FFFFFF)

@Composable
fun TvNowPlayingScreen(
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsState()
    val player by viewModel.player.collectAsState()
    val playback = state
    var isFullscreen by remember { mutableStateOf(false) }
    val isVideo = viewModel.isVideoEpisode

    // A single top-level `when` so this composable has exactly one emission
    // point; the three states below are mutually exclusive, not stacked.
    when {
        playback == null || playback.title.isBlank() -> EmptyPlayer(modifier = modifier)

        // Fullscreen video: fills the whole content area, Back returns to the
        // normal layout. Controls are hidden so nothing covers the picture.
        isFullscreen && isVideo -> {
            BackHandler(enabled = true) { isFullscreen = false }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                VideoSurface(
                    player = player,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                )
            }
        }

        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 72.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(40.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isVideo) {
                    // Video episodes get a 16:9 surface instead of square artwork.
                    VideoSurface(
                        player = player,
                        modifier = Modifier
                            .width(392.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                    )
                } else {
                    playback.podcast?.uuid?.let { uuid ->
                        TvArtworkImage(
                            model = PodcastImage.getMediumArtworkUrl(uuid),
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    playback.podcast?.title?.let { podcastTitle ->
                        Text(
                            text = podcastTitle,
                            style = MaterialTheme.tvTypography.caption1.copy(
                                color = TextSecondary,
                                fontSize = 24.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Text(
                        text = playback.title,
                        // lineHeight matters: without it the two lines overlap.
                        style = MaterialTheme.tvTypography.caption1.copy(
                            color = TextPrimary,
                            fontSize = 32.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(24.dp))

                    ProgressTrack(
                        positionMs = playback.positionMs,
                        bufferedMs = playback.bufferedMs,
                        durationMs = playback.durationMs,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (playback.isBuffering) {
                                "Buffering..."
                            } else {
                                formatMs(playback.positionMs) + "  /  " + formatMs(playback.durationMs)
                            },
                            style = MaterialTheme.tvTypography.caption1.copy(
                                color = TextPrimary,
                                fontSize = 24.sp,
                            ),
                        )
                        Spacer(Modifier.weight(1f))
                        val remaining = (playback.durationMs - playback.positionMs).coerceAtLeast(0)
                        Text(
                            text = "-" + formatMs(remaining) + " left",
                            style = MaterialTheme.tvTypography.caption1.copy(
                                color = TextSecondary,
                                fontSize = 24.sp,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            // wrapContentHeight stops the row being squeezed to nothing when the
            // column is tight - that clipped the button labels entirely.
            // start padding = artwork width (220) + row gap (40) so the controls
            // line up under the episode text instead of sitting off to the left.
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 260.dp),
            ) {
                // "- 10s" / "+ 20s" reads unambiguously across a room; the small
                // circular-arrow glyphs were easy to miss. Numbers come from the
                // user's own skip settings, not hardcoded.
                ControlButton(text = "\u2212 " + viewModel.skipBackSeconds + "s") { viewModel.skipBackward() }
                ControlButton(
                    text = stringResourceFor(playback.isPlaying),
                ) { viewModel.playPause() }
                ControlButton(text = "+ " + viewModel.skipForwardSeconds + "s") { viewModel.skipForward() }
                if (isVideo) {
                    ControlButton(text = "Fullscreen") { isFullscreen = true }
                }
                Spacer(Modifier.weight(1f))
                ControlButton(text = formatSpeed(playback.playbackSpeed)) { viewModel.cycleSpeed() }
            }
        }
    }
}

/**
 * Hosts a SurfaceView and hands it to the running player, which is exactly what
 * the phone app's VideoView does:
 *     (player as? SimplePlayer)?.setDisplay(surfaceView)
 * The tv module previously had no surface at all, so video episodes played
 * audio-only with no picture.
 */
@Composable
private fun VideoSurface(player: Player?, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) = Unit
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        (player as? SimplePlayer)?.setDisplay(null)
                    }
                })
            }
        },
        update = { surfaceView ->
            // Re-attach whenever the player instance changes (new episode,
            // player recreated after a stop, etc).
            (player as? SimplePlayer)?.setDisplay(surfaceView)
        },
        onRelease = { _ ->
            (player as? SimplePlayer)?.setDisplay(null)
        },
    )
}

@Composable
private fun stringResourceFor(isPlaying: Boolean): String = androidx.compose.ui.res.stringResource(if (isPlaying) LR.string.pause else LR.string.play)

@Composable
private fun EmptyPlayer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nothing is playing",
            style = MaterialTheme.tvTypography.caption1.copy(
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Choose an episode and select Play",
            style = MaterialTheme.tvTypography.caption1.copy(
                color = TextSecondary,
                fontSize = 22.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProgressTrack(positionMs: Int, bufferedMs: Int, durationMs: Int) {
    val playedFraction = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bufferedFraction = if (durationMs > 0) {
        (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(TrackBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction)
                .height(8.dp)
                .background(TrackBuffered),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(playedFraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TextPrimary),
        )
    }
}

@Composable
private fun ControlButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = TvButtonDefaults.filledButtonColors(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.tvTypography.caption1.copy(fontSize = 22.sp),
        )
    }
}

private fun formatMs(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds)
    }
}

private fun formatSpeed(speed: Double): String {
    val rounded = Math.round(speed * 10.0) / 10.0
    return if (rounded == Math.floor(rounded)) {
        String.format(Locale.ENGLISH, "%.0fx", rounded)
    } else {
        String.format(Locale.ENGLISH, "%.1fx", rounded)
    }
}
