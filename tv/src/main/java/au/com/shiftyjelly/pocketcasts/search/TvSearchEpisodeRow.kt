package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItemContainer
import au.com.shiftyjelly.pocketcasts.component.TvTile
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun TvSearchEpisodeRow(
    episode: ImprovedSearchResultItem.EpisodeItem,
    onClick: () -> Unit,
    onOpenActions: () -> Unit,
    modifier: Modifier = Modifier,
    episodeFocusRequester: FocusRequester? = null,
) {
    TvEpisodeListItemContainer(
        onOpenActions = onOpenActions,
        modifier = modifier,
    ) { rowModifier ->
        TvSearchEpisodeCard(
            episode = episode,
            onClick = onClick,
            modifier = rowModifier
                .then(if (episodeFocusRequester != null) Modifier.focusRequester(episodeFocusRequester) else Modifier),
        )
    }
}

@Composable
private fun TvSearchEpisodeCard(
    episode: ImprovedSearchResultItem.EpisodeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val textPrimary = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.textPrimary
    val textSecondary = if (isFocused) MaterialTheme.tvColors.textSecondaryActive else MaterialTheme.tvColors.textSecondary

    val context = LocalContext.current
    val duration = remember(episode.duration) {
        TimeHelper.getTimeDurationShortString(episode.duration.inWholeMilliseconds, context, emptyString = "")
    }

    TvTile(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundSunken,
            focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        ),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvArtworkImage(
                model = PodcastImage.getMediumArtworkUrl(episode.podcastUuid),
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
            ) {
                if (episode.podcastTitle.isNotBlank()) {
                    Text(
                        text = episode.podcastTitle,
                        style = MaterialTheme.tvTypography.caption1,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = episode.title,
                    style = MaterialTheme.tvTypography.body,
                    color = textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (duration.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = duration,
                        style = MaterialTheme.tvTypography.caption1,
                        color = textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchEpisodeRowPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken).padding(48.dp)) {
            TvSearchEpisodeRow(
                episode = ImprovedSearchResultItem.EpisodeItem(
                    uuid = "episode-1",
                    title = "The real cost of sugar and how it shapes the food we eat",
                    podcastUuid = "podcast-1",
                    podcastTitle = "Business Daily",
                    publishedDate = Date(0),
                    duration = 1440.seconds,
                ),
                onClick = {},
                onOpenActions = {},
            )
        }
    }
}
