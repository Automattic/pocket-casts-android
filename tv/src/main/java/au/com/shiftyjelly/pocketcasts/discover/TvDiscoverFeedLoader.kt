package au.com.shiftyjelly.pocketcasts.discover

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TvDiscoverFeedLoader @Inject constructor(
    private val listRepository: ListRepository,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
) {
    suspend fun load(isLoggedIn: Boolean): List<TvDiscoverRow> = coroutineScope {
        val discover = listRepository.getDiscoverFeed()
        val region = discover.regions[settings.discoverCountryCode.value]
            ?: discover.regions[discover.defaultRegionCode]
            ?: error("Could not resolve discover region")
        val replacements = mapOf(
            discover.regionCodeToken to region.code,
            discover.regionNameToken to region.name,
        )

        discover.layout
            .transformWithRegion(region, replacements, context.resources)
            .filter { it.categoryId == null } // Rows with a category ID are sponsored ads for the category pages.
            .filter { isLoggedIn || it.authenticated != true }
            .map { row -> async { loadRow(row) } }
            .awaitAll()
            .filterNotNull()
            .distinctBy(TvDiscoverRow::id)
    }

    private suspend fun loadRow(row: DiscoverRow): TvDiscoverRow? {
        return when (row.type) {
            is ListType.PodcastList -> loadPodcastsRow(row)
            is ListType.EpisodeList -> loadEpisodesRow(row)
            is ListType.Categories, is ListType.Unknown -> null
        }
    }

    private suspend fun loadPodcastsRow(row: DiscoverRow): TvDiscoverRow? {
        val feed = listRepository.getListFeed(row.source, row.authenticated) ?: return null
        val podcasts = feed.podcasts.orEmpty()
            .distinctBy(DiscoverPodcast::uuid)
            .map { it.toTvDiscoverPodcast(isSponsored = row.sponsored) }
        if (podcasts.isEmpty()) return null
        val title = feed.title?.takeIf { it.isNotBlank() } ?: row.title
        return when (row.displayStyle) {
            is DisplayStyle.Carousel -> TvDiscoverRow.FeaturedPodcasts(id = row.rowId(), title = title, podcasts = podcasts)
            is DisplayStyle.SinglePodcast -> TvDiscoverRow.SinglePodcast(id = row.rowId(), title = title, podcasts = podcasts)
            else -> TvDiscoverRow.Podcasts(id = row.rowId(), title = title, podcasts = podcasts)
        }
    }

    private suspend fun loadEpisodesRow(row: DiscoverRow): TvDiscoverRow? {
        val feed = listRepository.getListFeed(row.source, row.authenticated) ?: return null
        val episodes = feed.episodes.orEmpty()
            .distinctBy(DiscoverEpisode::uuid)
            .map { episode ->
                TvDiscoverEpisode(
                    episodeUuid = episode.uuid,
                    episodeTitle = episode.title.orEmpty(),
                    podcastUuid = episode.podcast_uuid,
                    podcastTitle = episode.podcast_title.orEmpty(),
                )
            }
        if (episodes.isEmpty()) return null
        val title = feed.title?.takeIf { it.isNotBlank() } ?: row.title
        return TvDiscoverRow.Episodes(id = row.rowId(), title = title, episodes = episodes)
    }

    private fun DiscoverRow.rowId() = listUuid ?: id ?: title

    private fun DiscoverPodcast.toTvDiscoverPodcast(isSponsored: Boolean) = TvDiscoverPodcast(
        uuid = uuid,
        title = title.orEmpty(),
        author = author.orEmpty(),
        description = description.orEmpty(),
        isSponsored = isSponsored,
    )
}
