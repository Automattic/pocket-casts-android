package au.com.shiftyjelly.pocketcasts.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.component.TvBannerRow
import au.com.shiftyjelly.pocketcasts.component.TvCategoryTile
import au.com.shiftyjelly.pocketcasts.component.TvFeaturedTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvSinglePodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvVideoTile
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory

fun LazyListScope.tvDiscoverRow(
    row: TvDiscoverRow,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (TvDiscoverEpisode) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
    onOpenCategory: (DiscoverCategory) -> Unit = {},
    onTapBanner: (TvDiscoverBanner) -> Unit = {},
    onPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit = { _, _ -> },
    onEpisodePlay: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onCategoryClick: (DiscoverCategory, Int) -> Unit = { _, _ -> },
    onListImpression: (TvDiscoverRow) -> Unit = {},
) {
    when (row) {
        is TvDiscoverRow.FeaturedPodcasts -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.podcasts,
                itemSpacing = 32.dp,
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
                    onGoToPodcast = {
                        onPodcastClick(row, podcast)
                        onOpenPodcast(podcast.uuid)
                    },
                    onPlayLastEpisode = {},
                )
            }
        }

        is TvDiscoverRow.SinglePodcast -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.podcasts,
                itemSpacing = 32.dp,
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
                    onClick = {
                        onPodcastClick(row, podcast)
                        onOpenPodcast(podcast.uuid)
                    },
                )
            }
        }

        is TvDiscoverRow.Episodes -> item(key = row.id) {
            LaunchedEffect(row.id) { onListImpression(row) }
            TvRow(
                title = row.title,
                items = row.episodes,
                itemSpacing = 32.dp,
                contentPadding = contentPadding,
                key = TvDiscoverEpisode::episodeUuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { episode ->
                TvVideoTile(
                    thumbnailUrl = episode.thumbnailUrl,
                    podcastArtworkUrl = episode.podcastArtworkUrl,
                    podcastTitle = episode.podcastTitle,
                    episodeTitle = episode.episodeTitle,
                    onPlayEpisode = {
                        onEpisodePlay(row, episode)
                        onPlayEpisode(episode)
                    },
                    onGoToPodcast = {
                        onEpisodePodcastClick(row, episode)
                        onOpenPodcast(episode.podcastUuid)
                    },
                )
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
                    onClick = {
                        onPodcastClick(row, podcast)
                        onOpenPodcast(podcast.uuid)
                    },
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
                TvCategoryTile(
                    category = category,
                    onClick = {
                        onCategoryClick(category, row.categories.indexOf(category))
                        onOpenCategory(category)
                    },
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
    }
}
