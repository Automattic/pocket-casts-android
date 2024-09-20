package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.action.controlPlaybackAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.data.SmallPlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SmallPlayer(state: SmallPlayerWidgetState) {
    val controlPlayback = if (state.episode == null) {
        OpenPocketCastsAction.action()
    } else {
        controlPlaybackAction(state.isPlaying, LocalSource.current)
    }
    val contentDescription = when {
        state.episode == null -> LR.string.pocket_casts
        state.isPlaying -> LR.string.play_episode
        else -> LR.string.pause_episode
    }.let { LocalContext.current.getString(it) }

    val size = min(LocalSize.current.width, LocalSize.current.height)

    WidgetTheme(state.useDynamicColors) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(controlPlayback)
                .semantics {
                    this.contentDescription = contentDescription
                },
        ) {
            EpisodeImage(
                episode = state.episode,
                useEpisodeArtwork = state.useEpisodeArtwork,
                size = size,
                backgroundColor = { it.background },
                onClick = controlPlayback,
            )
            if (state.episode != null) {
                val buttonSize = (size / 2.3f).coerceAtMost(48.dp)
                PlaybackButton(
                    isPlaying = state.isPlaying,
                    iconPadding = buttonSize / 4.75f,
                    modifier = GlanceModifier.size(buttonSize),
                )
            }
        }
    }
}
