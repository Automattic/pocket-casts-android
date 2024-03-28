package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import au.com.shiftyjelly.pocketcasts.widget.action.OpenEpisodeDetailsAction
import au.com.shiftyjelly.pocketcasts.widget.action.OpenPocketCastsAction
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState

@Composable
internal fun MediumPlayer(state: PlayerWidgetState) {
    WidgetTheme(state.useDynamicColors) {
        val episode = state.currentEpisode
        val action = if (episode == null) {
            OpenPocketCastsAction.action()
        } else {
            OpenEpisodeDetailsAction.action(episode.uuid)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(96.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(12.dp)
                .clickable(action),
        ) {
            EpisodeImage(
                episode = state.currentEpisode,
                useRssArtwork = state.useRssArtwork,
                modifier = GlanceModifier.size(72.dp),
            )

            if (episode != null) {
                Spacer(modifier = GlanceModifier.width(12.dp))
                Column(
                    verticalAlignment = Alignment.Vertical.Top,
                    modifier = GlanceModifier.height(72.dp),
                ) {
                    Text(
                        text = episode.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.Vertical.Bottom,
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                    ) {
                        SkipBackButton(
                            modifier = GlanceModifier.size(42.dp).padding(8.dp),
                        )
                        Spacer(
                            modifier = GlanceModifier.width(8.dp),
                        )
                        PlaybackButton(
                            state.isPlaying,
                            modifier = GlanceModifier.size(42.dp).padding(6.dp),
                        )
                        Spacer(
                            modifier = GlanceModifier.width(8.dp),
                        )
                        SkipForwardButton(
                            modifier = GlanceModifier.size(42.dp).padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}
