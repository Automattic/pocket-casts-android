package au.com.shiftyjelly.pocketcasts.discover

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.component.TvFeaturedTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvSponsoredTile
import au.com.shiftyjelly.pocketcasts.component.TvVideoTile

fun LazyListScope.tvDiscoverRow(
    row: TvDiscoverRow,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (TvDiscoverEpisode) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
) {
    when (row) {
        is TvDiscoverRow.FeaturedPodcasts -> item(key = row.id) {
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
                    onGoToPodcast = { onOpenPodcast(podcast.uuid) },
                    onPlayLastEpisode = {},
                )
            }
        }

        is TvDiscoverRow.SinglePodcast -> item(key = row.id) {
            TvRow(
                title = row.title,
                items = row.podcasts,
                itemSpacing = 32.dp,
                contentPadding = contentPadding,
                key = TvDiscoverPodcast::uuid,
                focusRequester = focusRequester,
                modifier = modifier,
            ) { podcast ->
                TvSponsoredTile(
                    artworkUrl = podcast.artworkUrl,
                    title = podcast.title,
                    author = podcast.author,
                    description = podcast.description,
                    isSponsored = podcast.isSponsored,
                    onClick = { onOpenPodcast(podcast.uuid) },
                )
            }
        }

        is TvDiscoverRow.Episodes -> item(key = row.id) {
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
                    onPlayEpisode = { onPlayEpisode(episode) },
                    onGoToPodcast = { onOpenPodcast(episode.podcastUuid) },
                )
            }
        }

        is TvDiscoverRow.Podcasts -> item(key = row.id) {
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
                    onClick = { onOpenPodcast(podcast.uuid) },
                    imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
                )
            }
        }
    }
}
