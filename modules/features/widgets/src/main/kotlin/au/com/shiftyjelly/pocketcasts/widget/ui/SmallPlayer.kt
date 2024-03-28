package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.action.controlPlaybackAction
import au.com.shiftyjelly.pocketcasts.widget.data.LocalSource
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SmallPlayer(state: PlayerWidgetState) {
    val controlPlayback = if (state.currentEpisode == null) {
        OpenPocketCastsAction.action()
    } else {
        controlPlaybackAction(state.isPlaying, LocalSource.current)
    }
    val contentDescription = when {
        state.currentEpisode == null -> LR.string.pocket_casts
        state.isPlaying -> LR.string.play_episode
        else -> LR.string.pause_episode
    }.let { LocalContext.current.getString(it) }

    WidgetTheme(state.useDynamicColors) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .clickable(controlPlayback)
                .semantics {
                    this.contentDescription = contentDescription
                },
        ) {
            EpisodeImage(
                episode = state.currentEpisode,
                useRssArtwork = state.useRssArtwork,
            )
            if (state.currentEpisode != null) {
                PlaybackButton(
                    state.isPlaying,
                    modifier = GlanceModifier.size(32.dp).padding(4.dp),
                )
            }
        }
    }
}
