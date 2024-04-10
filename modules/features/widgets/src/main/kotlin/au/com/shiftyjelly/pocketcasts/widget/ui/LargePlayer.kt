package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import au.com.shiftyjelly.pocketcasts.widget.data.LargePlayerWidgetState

@Composable
internal fun LargePlayer(state: LargePlayerWidgetState) {
    val upNextEpisodes = state.upNextEpisodes

    WidgetTheme(state.useDynamicColors) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(350.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(16.dp),
        ) {
            LargePlayerHeader(state = state)
            if (upNextEpisodes.isNotEmpty()) {
                Spacer(
                    modifier = GlanceModifier.height(12.dp),
                )
                LargePlayerQueue(
                    queue = upNextEpisodes,
                    useEpisodeArtwork = state.useEpisodeArtwork,
                )
            }
        }
    }
}
