package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode

@Composable
internal fun LargePlayerQueue(
    queue: List<PlayerWidgetEpisode>,
    useEpisodeArtwork: Boolean,
    useDynamicColors: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val lastIndex = queue.lastIndex
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp),
    ) {
        itemsIndexed(queue, { _, episode -> episode.longId }) { index, episode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(if (index == lastIndex) 58.dp else 66.dp)
                    .padding(bottom = if (index == lastIndex) 0.dp else 8.dp),
            ) {
                EpisodeImage(
                    episode = episode,
                    useEpisodeArtwork = useEpisodeArtwork,
                    size = 58.dp,
                )
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.defaultWeight().padding(start = 12.dp),
                ) {
                    NonScalingText(
                        text = episode.title,
                        textSize = 13.dp,
                        useDynamicColors = useDynamicColors,
                        maxLines = 2,
                        isBold = true,
                        modifier = GlanceModifier.padding(end = 16.dp),
                    )
                    Spacer(
                        modifier = GlanceModifier.height(2.dp),
                    )
                    NonScalingText(
                        text = episode.getTimeLeft(LocalContext.current),
                        textSize = 13.dp,
                        useDynamicColors = useDynamicColors,
                        alpha = 0.8,
                        modifier = GlanceModifier.padding(end = 16.dp),
                    )
                }
                PlayButton(episode = episode)
            }
        }
    }
}
