package au.com.shiftyjelly.pocketcasts.repositories.search

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem

interface SearchAutoCompleteManager {
    suspend fun autoCompleteSearch(term: String): List<SearchAutoCompleteItem>
}
