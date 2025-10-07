package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun ImprovedSearchEpisodeResultRow(
    item: SearchAutoCompleteItem.Episode,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchEpisodeResultRow(
        episodeUuid = item.uuid,
        podcastUuid = item.podcastUuid,
        title = item.title,
        duration = item.duration,
        publishedAt = item.publishedAt,
        onClick = onClick,
        onPlay = onPlay,
        modifier = modifier
    )
}

@Composable
fun ImprovedSearchEpisodeResultRow(
    episode: EpisodeItem,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchEpisodeResultRow(
        episodeUuid = episode.uuid,
        podcastUuid = episode.podcastUuid,
        title = episode.title,
        duration = episode.duration,
        publishedAt = episode.publishedAt,
        onClick = onClick,
        onPlay = onPlay,
        modifier = modifier
    )
}

@Composable
private fun ImprovedSearchEpisodeResultRow(
    episodeUuid: String,
    podcastUuid: String,
    title: String,
    duration: Double,
    publishedAt: Date,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EpisodeImage(
            episode = PodcastEpisode(
                uuid = episodeUuid,
                title = title,
                duration = duration,
                publishedDate = publishedAt,
                podcastUuid = podcastUuid,
            ),
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
            useEpisodeArtwork = false,
            corners = 4.dp,
            modifier = modifier
                .size(56.dp)
                .shadow(1.dp, RoundedCornerShape(4.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {

            val context = LocalContext.current
            val formattedDuration =
                remember(duration, context) { TimeHelper.getTimeDurationMediumString((duration * 1000).toInt(), context) }
            val dateFormatter = RelativeDateFormatter(context)
            val formattedPublishDate = remember(publishedAt, dateFormatter) { dateFormatter.format(publishedAt) }

            TextC70(
                fontSize = 11.sp,
                text = formattedPublishDate,
                maxLines = 1,
            )
            TextH40(
                text = title,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 1,
            )
            TextH60(
                fontSize = 12.sp,
                text = formattedDuration,
                color = MaterialTheme.theme.colors.secondaryText02,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(IR.drawable.filter_play),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(1.dp, color = MaterialTheme.theme.colors.primaryInteractive01, shape = CircleShape)
                .clickable(onClick = onPlay),
            tint = MaterialTheme.theme.colors.primaryInteractive01,
        )
    }
}

@Preview
@Composable
private fun PreviewEpisodeResultRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchEpisodeResultRow(
            episodeUuid = "",
            podcastUuid = "",
            title = "Episode title",
            duration = 320.0,
            publishedAt = Date(),
            onClick = {},
            onPlay = {}
        )
    }
}