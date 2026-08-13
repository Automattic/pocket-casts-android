package au.com.shiftyjelly.pocketcasts.nowplaying

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Locale
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * A side panel with a compact player, reachable from ANY screen via the
 * remote's Options / MENU key. Lets playback be controlled without navigating
 * away from whatever is being browsed.
 */
@Composable
fun TvMiniPlayerDrawer(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvNowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.playbackState.collectAsState()
    val playback = state
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = true) { onDismissRequest() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(modifier = modifier.fillMaxSize()) {
        // Scrim over the underlying screen.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xB3000000)),
        )
        Column(
            modifier = Modifier
                .width(520.dp)
                .fillMaxHeight()
                .background(Color(0xFF161616))
                .padding(horizontal = 36.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            if (playback == null || playback.title.isBlank()) {
                Text(
                    text = "Nothing is playing",
                    style = MaterialTheme.tvTypography.caption1.copy(
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                return@Column
            }

            playback.podcast?.uuid?.let { uuid ->
                TvArtworkImage(
                    model = PodcastImage.getMediumArtworkUrl(uuid),
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Spacer(Modifier.height(24.dp))
            }

            playback.podcast?.title?.let { podcastTitle ->
                Text(
                    text = podcastTitle,
                    style = MaterialTheme.tvTypography.caption1.copy(
                        color = Color(0xFFB6B6B6),
                        fontSize = 20.sp,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
            }

            Text(
                text = playback.title,
                style = MaterialTheme.tvTypography.caption1.copy(
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(22.dp))

            val fraction = if (playback.durationMs > 0) {
                (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x33FFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = drawerTime(playback.positionMs) + "  /  " + drawerTime(playback.durationMs),
                style = MaterialTheme.tvTypography.caption1.copy(
                    color = Color.White,
                    fontSize = 20.sp,
                ),
            )

            Spacer(Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawerButton(
                    text = "\u2212 " + viewModel.skipBackSeconds + "s",
                    onClick = { viewModel.skipBackward() },
                    modifier = Modifier,
                )
                DrawerButton(
                    text = androidx.compose.ui.res.stringResource(
                        if (playback.isPlaying) LR.string.pause else LR.string.play,
                    ),
                    onClick = { viewModel.playPause() },
                    modifier = Modifier.focusRequester(focusRequester),
                )
                DrawerButton(
                    text = "+ " + viewModel.skipForwardSeconds + "s",
                    onClick = { viewModel.skipForward() },
                    modifier = Modifier,
                )
            }
        }
    }
}

@Composable
private fun DrawerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = TvButtonDefaults.filledButtonColors(),
        modifier = modifier,
    ) {
        Text(text = text, style = MaterialTheme.tvTypography.caption1.copy(fontSize = 20.sp))
    }
}

private fun drawerTime(ms: Int): String {
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
