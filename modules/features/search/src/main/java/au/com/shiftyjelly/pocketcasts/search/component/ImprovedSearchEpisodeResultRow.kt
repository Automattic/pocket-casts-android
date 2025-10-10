package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun ImprovedSearchEpisodeResultRow(
    item: SearchAutoCompleteItem.Episode,
    onClick: () -> Unit,
    playButtonListener: PlayButton.OnClickListener,
    modifier: Modifier = Modifier,
) {
    ImprovedSearchEpisodeResultRow(
        episodeUuid = item.uuid,
        podcastUuid = item.podcastUuid,
        title = item.title,
        duration = item.duration.seconds,
        publishedAt = item.publishedAt,
        onClick = onClick,
        playButtonListener = playButtonListener,
        modifier = modifier,
    )
}

@Composable
fun ImprovedSearchEpisodeResultRow(
    episode: ImprovedSearchResultItem.EpisodeItem,
    onClick: () -> Unit,
    playButtonListener: PlayButtonListener,
    modifier: Modifier = Modifier,
    fetchEpisode: (suspend (ImprovedSearchResultItem.EpisodeItem) -> BaseEpisode?)? = null,
) {
    val baseEpisode: BaseEpisode? by produceState(null) {
        value = fetchEpisode?.invoke(episode)
    }

    ImprovedSearchEpisodeResultRow(
        episodeUuid = episode.uuid,
        podcastUuid = episode.podcastUuid,
        title = episode.title,
        duration = episode.duration,
        publishedAt = episode.publishedDate,
        playButtonListener = playButtonListener,
        onClick = onClick,
        modifier = modifier,
        episode = baseEpisode,
    )
}

@Composable
private fun ImprovedSearchEpisodeResultRow(
    episodeUuid: String,
    podcastUuid: String,
    title: String,
    duration: Duration,
    publishedAt: Date,
    onClick: () -> Unit,
    playButtonListener: PlayButton.OnClickListener,
    modifier: Modifier = Modifier,
    episode: BaseEpisode? = null,
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
                duration = duration.inWholeSeconds.toDouble(),
                publishedDate = publishedAt,
                podcastUuid = podcastUuid,
            ),
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
            useEpisodeArtwork = false,
            corners = 4.dp,
            modifier = Modifier
                .size(56.dp)
                .shadow(1.dp, RoundedCornerShape(4.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val context = LocalContext.current
            val formattedDuration =
                remember(duration, context) { TimeHelper.getTimeDurationMediumString(duration.inWholeMilliseconds.toInt(), context) }
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
        episode?.let {
            val buttonColor = MaterialTheme.theme.colors.primaryInteractive01.toArgb()
            AndroidView(
                modifier = Modifier.size(48.dp),
                factory = {
                    PlayButton(it).apply {
                        listener = playButtonListener
                    }
                },
                update = { playButton ->
                    playButton.setButtonType(episode, buttonType = PlayButtonType.PLAY, color = buttonColor, null)
                },
            )
        }
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
            duration = 340.seconds,
            publishedAt = Date(),
            playButtonListener = object : PlayButton.OnClickListener {
                override var source: SourceView = SourceView.SEARCH_RESULTS

                override fun onPlayClicked(episodeUuid: String) = Unit

                override fun onPauseClicked() = Unit

                override fun onPlayNext(episodeUuid: String) = Unit

                override fun onPlayLast(episodeUuid: String) = Unit

                override fun onDownload(episodeUuid: String) = Unit

                override fun onStopDownloading(episodeUuid: String) = Unit

                override fun onPlayedClicked(episodeUuid: String) = Unit
            },
            onClick = {},
        )
    }
}
