package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteSearchService
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedResult
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedSearchRequest
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class ImprovedSearchManagerImpl @Inject constructor(
    private val autoCompleteSearchService: AutoCompleteSearchService,
    private val combinedSearchService: PodcastCacheService,
) : ImprovedSearchManager {
    override suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem> {
        val response = autoCompleteSearchService.autoCompleteSearch(query = term, termsLimit = null, podcastsLimit = null)
        return response.results.map {
            when (it) {
                is AutoCompleteResult.TermResult -> SearchAutoCompleteItem.Term(term = it.value)
                is AutoCompleteResult.PodcastResult -> SearchAutoCompleteItem.Podcast(uuid = it.value.uuid, title = it.value.title, author = it.value.author.orEmpty())
            }
        }
    }

    override suspend fun combinedSearch(term: String): List<ImprovedSearchResultItem> {
        val response = combinedSearchService.combinedSearch(CombinedSearchRequest(term))
        return response.results.map {
            when (it) {
                is CombinedResult.PodcastResult -> ImprovedSearchResultItem.PodcastItem(
                    uuid = it.uuid,
                    title = it.title,
                    author = it.author.orEmpty(),
                    isFollowed = false,
                )
                is CombinedResult.EpisodeResult -> ImprovedSearchResultItem.EpisodeItem(
                    uuid = it.uuid,
                    title = it.title,
                    podcastUuid = it.podcastUuid,
                    publishedDate = it.publishedDate,
                    duration = it.duration.seconds,
                )
            }
        }
    }
}
