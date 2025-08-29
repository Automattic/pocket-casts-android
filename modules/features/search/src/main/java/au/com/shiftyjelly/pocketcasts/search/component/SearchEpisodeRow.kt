package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImageDeprecated
import au.com.shiftyjelly.pocketcasts.compose.components.TextC50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem

private val IconSize = 56.dp

@Composable
fun SearchEpisodeItem(
    episode: EpisodeItem,
    onClick: (EpisodeItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val durationMs = episode.duration * 1000
    val dateFormatter = RelativeDateFormatter(context)
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(episode) }
                .padding(16.dp),
        ) {
            @Suppress("DEPRECATION")
            PodcastImageDeprecated(
                uuid = episode.podcastUuid,
                modifier = Modifier.size(IconSize),
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp, end = 16.dp)
                    .weight(1f),
            ) {
                TextC50(
                    text = dateFormatter.format(episode.publishedAt),
                    maxLines = 1,
                )
                TextH40(
                    text = episode.title,
                    maxLines = 2,
                )
                TextH60(
                    text = TimeHelper.getTimeDurationMediumString(durationMs.toInt(), context),
                    maxLines = 1,
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
        }
        HorizontalDivider(startIndent = 16.dp)
    }
}
