package au.com.shiftyjelly.pocketcasts.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetEpisode

@Composable
internal fun LargePlayerQueue(
    queue: List<PlayerWidgetEpisode>,
    useEpisodeArtwork: Boolean,
    useDynamicColors: Boolean,
    modifier: GlanceModifier = GlanceModifier,
) {
    val lastIndex = queue.lastIndex
    val secondaryTextColor = if (useDynamicColors) {
        GlanceTheme.colors.onPrimaryContainer
    } else {
        GlanceTheme.colors.onSecondaryContainer
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
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
                    Text(
                        text = episode.title,
                        maxLines = 2,
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = GlanceModifier.padding(end = 16.dp),
                    )
                    Spacer(
                        modifier = GlanceModifier.height(2.dp),
                    )
                    Text(
                        text = episode.getTimeLeft(LocalContext.current),
                        maxLines = 1,
                        style = TextStyle(
                            color = secondaryTextColor,
                            fontSize = 13.sp,
                        ),
                        modifier = GlanceModifier.padding(end = 16.dp),
                    )
                }
                PlayButton(episode = episode)
            }
        }
    }
}
