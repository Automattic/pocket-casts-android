package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
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

    val width = LocalSize.current.width

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
                size = width,
                backgroundColor = { it.background },
                onClick = { controlPlayback },
            )
            if (state.episode != null) {
                PlaybackButton(
                    isPlaying = state.isPlaying,
                    height = width / 2,
                    iconPadding = width / 16,
                    modifier = GlanceModifier.width(width / 2),
                )
            }
        }
    }
}
