package au.com.shiftyjelly.pocketcasts.servers.discover

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem

data class AutoCompleteSearch(
    val searchTerm: String = "",
    val error: Throwable? = null,
    val results: List<SearchAutoCompleteItem> = emptyList()
)