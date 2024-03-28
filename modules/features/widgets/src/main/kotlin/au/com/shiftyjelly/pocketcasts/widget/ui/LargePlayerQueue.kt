package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import au.com.shiftyjelly.pocketcasts.widget.action.OpenEpisodeDetailsAction
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode

@Composable
internal fun LargePlayerQueue(
    queue: List<PlayerWidgetEpisode>,
    useRssArtwork: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
    ) {
        items(queue, PlayerWidgetEpisode::longId) { episode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .padding(bottom = 12.dp),
            ) {
                EpisodeImage(
                    episode = episode,
                    useRssArtwork = useRssArtwork,
                    modifier = GlanceModifier
                        .size(66.dp)
                        .clickable(OpenEpisodeDetailsAction.action(episode.uuid)),
                )
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
                ) {
                    Text(
                        text = episode.title,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(
                        modifier = GlanceModifier.height(4.dp),
                    )
                    Text(
                        text = episode.getTimeLeft(LocalContext.current),
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 16.sp,
                        ),
                    )
                }
                PlayButton(
                    episode = episode,
                )
            }
        }
    }
}
