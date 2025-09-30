package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.SearchService
import javax.inject.Inject

class SearchAutoCompleteManagerImpl @Inject constructor(
    private val searchService: SearchService,
    private val settings: Settings,
) : SearchAutoCompleteManager {
    override suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem> {
        val response = searchService.autoCompleteSearch(query = term, termsLimit = TERM_LIMIT, podcastsLimit = PODCAST_LIMIT, language = settings.discoverCountryCode.value)
        return response.results.map {
            when (it) {
                is AutoCompleteResult.TermResult -> SearchAutoCompleteItem.Term(term = it.value)
                is AutoCompleteResult.PodcastResult -> SearchAutoCompleteItem.Podcast(uuid = it.value.uuid, title = it.value.title, author = it.value.author)
            }
        }
    }

    private companion object {
        const val TERM_LIMIT = 4
        const val PODCAST_LIMIT = 4
    }
}
