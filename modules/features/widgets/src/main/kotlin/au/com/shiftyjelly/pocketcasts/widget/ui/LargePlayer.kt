package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState

@Composable
internal fun LargePlayer(state: PlayerWidgetState) {
    val upNextEpisodes = state.upNextEpisodes

    WidgetTheme(state.useDynamicColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.primaryContainer)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 0.dp),
        ) {
            LargePlayerHeader(state = state)
            if (upNextEpisodes.isNotEmpty()) {
                Spacer(
                    modifier = GlanceModifier.height(12.dp),
                )
                LargePlayerQueue(
                    queue = upNextEpisodes,
                    useRssArtwork = state.useRssArtwork,
                )
            }
        }
    }
}
