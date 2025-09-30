package au.com.shiftyjelly.pocketcasts.servers.discover

import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import java.io.Serializable

sealed interface AutoCompleteSearch : Serializable {
    val searchTerm: String

    data class Error(
        override val searchTerm: String,
        val error: Throwable,
    ) : AutoCompleteSearch

    data class Success(
        override val searchTerm: String,
        val results: List<SearchAutoCompleteItem>,
    ) : AutoCompleteSearch
}
