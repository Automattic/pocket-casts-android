package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedResult

interface ImprovedSearchManager {
    suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem>
    suspend fun combinedSearch(term: String): List<CombinedResult>
}
