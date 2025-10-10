package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem

interface ImprovedSearchManager {
    suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem>
    suspend fun combinedSearch(term: String): List<ImprovedSearchResultItem>
}
