package au.com.shiftyjelly.pocketcasts.discover.worker

import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class CuratedPodcastsCrawler @Inject constructor(
    private val service: ListWebService,
) {
    suspend fun crawl(platform: String): Result<List<CuratedPodcast>> = coroutineScope {
        runCatching { service.getDiscoverFeedSuspend(platform) }.mapCatching { discover ->
            val feeds = discover.layout
                .filterDisplayablePodcasts()
                .map { fetchFeed(it.source) }
                .awaitAll()
            feeds.forEach { feed ->
                feed.onFailure { Timber.d(it, "Failed to fetch a feed") }
            }
            if (feeds.isNotEmpty() && feeds.all { it.isFailure }) {
                throw RuntimeException("Failed to fetch any feed")
            } else {
                feeds.mapNotNull { result -> result.getOrNull() }.toCuratedPodcasts()
            }
        }
    }

    private fun List<ListFeed>.toCuratedPodcasts() = flatMap { feed ->
        val feedId = feed.listId ?: return@flatMap emptyList()
        val feedTitle = feed.title ?: return@flatMap emptyList()
        val podcasts = feed.podcasts ?: return@flatMap emptyList()

        podcasts.mapNotNull { podcast ->
            podcast.title?.let { podcastTitle ->
                CuratedPodcast(
                    listId = feedId,
                    listTitle = feedTitle,
                    podcastId = podcast.uuid,
                    podcastTitle = podcastTitle,
                    podcastDescription = podcast.description,
                )
            }
        }
    }

    private fun List<DiscoverRow>.filterDisplayablePodcasts() = filter { row ->
        val isSpecialList = row.id in CuratedPodcast.specialListIds
        (isSpecialList || row.curated) && !row.sponsored && row.type == ListType.PodcastList
    }

    private fun CoroutineScope.fetchFeed(url: String): Deferred<Result<ListFeed>> {
        return async { runCatching { service.getListFeedSuspend(url) } }
    }
}
