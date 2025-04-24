package au.com.shiftyjelly.pocketcasts.discover.worker

import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.BuildConfig
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class CuratedPodcastsCrawler(
    private val listRepository: ListRepository,
    private val staticHostUrl: String,
) {
    @Inject constructor(
        listRepository: ListRepository,
    ) : this(listRepository, BuildConfig.SERVER_STATIC_URL)

    suspend fun crawl(): Result<List<CuratedPodcast>> = coroutineScope {
        runCatching { listRepository.getDiscoverFeed() }.mapCatching { discover ->
            val feeds = discover.layout
                .filterDisplayablePodcasts()
                .mapNotNull { row -> row.id?.let { id -> fetchFeed(id, row.source) } }
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

    private fun CoroutineScope.fetchFeed(id: String, url: String): Deferred<Result<ListFeed>> {
        val engageUrl = if (id == CuratedPodcast.FEATURED_LIST_ID) {
            "$staticHostUrl/engage/featured.json"
        } else {
            url
        }
        return async {
            runCatching {
                checkNotNull(listRepository.getListFeed(engageUrl))
            }
        }
    }
}
