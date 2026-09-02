package au.com.shiftyjelly.pocketcasts.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvBannerRow
import au.com.shiftyjelly.pocketcasts.component.TvCategoryTile
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeRow
import au.com.shiftyjelly.pocketcasts.component.TvFeaturedTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvResumeCard
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvSinglePodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvVideoTile
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

fun LazyListScope.tvDiscoverRow(
    row: TvDiscoverRow,
    onPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit,
    onEpisodePlay: (TvDiscoverRow, TvDiscoverEpisode) -> Unit,
    onEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit,
    onCategoryClick: (DiscoverCategory, Int) -> Unit,
    onPlayLatestEpisode: (TvDiscoverRow, TvDiscoverPodcast) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 42.dp),
    onTapBanner: (TvDiscoverBanner) -> Unit = {},
    onListImpression: (TvDiscoverRow) -> Unit = {},
    onRetryRow: (TvDiscoverRow) -> Unit = {},
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
    isPodcastPlaying: () -> Boolean = { false },
) {
    when (row) {
        is TvDiscoverRow.FeaturedPodcasts -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.podcasts,
                itemSpacing = 12.dp,
                contentPadding = contentPadding,
                key = TvDiscoverPodcast::uuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { podcast ->
                TvFeaturedTile(
                    artworkUrl = podcast.artworkUrl,
                    isSponsored = podcast.isSponsored,
                    title = podcast.title,
                    description = podcast.description,
                    onGoToPodcast = { onPodcastClick(row, podcast) },
                    onPlayLastEpisode = { onPlayLatestEpisode(row, podcast) },
                )
            }
        }

        is TvDiscoverRow.SinglePodcast -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.podcasts,
                itemSpacing = 12.dp,
                contentPadding = contentPadding,
                key = TvDiscoverPodcast::uuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { podcast ->
                TvSinglePodcastTile(
                    artworkUrl = podcast.artworkUrl,
                    title = podcast.title,
                    author = podcast.author,
                    description = podcast.description,
                    isSponsored = podcast.isSponsored,
                    onClick = { onPodcastClick(row, podcast) },
                )
            }
        }

        is TvDiscoverRow.Episodes -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.episodes,
                itemSpacing = 12.dp,
                contentPadding = contentPadding,
                key = TvDiscoverEpisode::episodeUuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { episode ->
                val podcastEpisode = episode.episode
                if (row.progressCardStyle != null && podcastEpisode != null) {
                    val context = LocalContext.current
                    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
                    when (row.progressCardStyle) {
                        TvProgressCardStyle.Resume -> TvResumeCard(
                            episode = podcastEpisode,
                            onClick = { onEpisodePlay(row, episode) },
                            onLongClick = { onEpisodePodcastClick(row, episode) },
                            dateFormatter = dateFormatter,
                        )

                        TvProgressCardStyle.Queue -> TvEpisodeRow(
                            episode = podcastEpisode,
                            onClick = { onEpisodePlay(row, episode) },
                            onLongClick = { onEpisodePodcastClick(row, episode) },
                            dateFormatter = dateFormatter,
                            modifier = Modifier.width(431.dp),
                        )
                    }
                } else {
                    TvVideoTile(
                        thumbnailUrl = episode.thumbnailUrl,
                        podcastArtworkUrl = episode.podcastArtworkUrl,
                        podcastTitle = episode.podcastTitle,
                        episodeTitle = episode.episodeTitle,
                        onPlayEpisode = { onEpisodePlay(row, episode) },
                        onGoToPodcast = { onEpisodePodcastClick(row, episode) },
                        videoPreviewUrl = episode.videoPreviewUrl,
                        isPodcastPlaying = isPodcastPlaying,
                    )
                }
            }
        }

        is TvDiscoverRow.Podcasts -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.podcasts,
                contentPadding = contentPadding,
                key = TvDiscoverPodcast::uuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { podcast ->
                TvPodcastTile(
                    artworkUrl = podcast.artworkUrl,
                    podcastTitle = podcast.title,
                    onClick = { onPodcastClick(row, podcast) },
                    imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
                    isSponsored = podcast.isSponsored,
                )
            }
        }

        is TvDiscoverRow.Categories -> item(key = row.id) {
            TvRow(
                title = row.title,
                items = row.categories,
                contentPadding = contentPadding,
                key = { it.id },
                focusRequester = focusRequester,
                modifier = modifier,
            ) { category ->
                val categoryIndex = row.categories.indexOfFirst { it.id == category.id }
                TvCategoryTile(
                    category = category,
                    onClick = { onCategoryClick(category, categoryIndex) },
                    colorIndex = categoryIndex,
                    loadCoverUrls = loadCategoryCovers?.let { load -> { load(category) } },
                )
            }
        }

        is TvDiscoverRow.Banner -> item(key = row.id) {
            val bannerModifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            TvBannerRow(
                banner = row.banner,
                onClick = { onTapBanner(row.banner) },
                modifier = bannerModifier.padding(contentPadding),
            )
        }

        is TvDiscoverRow.Failed -> item(key = row.id) {
            val failedModifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier
            TvDiscoverFailedRow(
                title = row.title,
                onRetry = { onRetryRow(row) },
                modifier = failedModifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun TvDiscoverFailedRow(
    title: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = modifier) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.tvTypography.title3,
                color = MaterialTheme.tvColors.textPrimary,
            )
        }
        Button(
            onClick = onRetry,
            colors = TvButtonDefaults.filledButtonColors(),
        ) {
            Text(text = stringResource(LR.string.try_again))
        }
    }
}
