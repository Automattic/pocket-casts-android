package au.com.shiftyjelly.pocketcasts.transcripts

import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches

data class SearchState(
    val isSearchOpen: Boolean,
    val searchTerm: String,
    val matches: SearchMatches,
) {
    companion object {
        val Empty = SearchState(
            isSearchOpen = false,
            searchTerm = "",
            matches = SearchMatches(
                selectedCoordinate = null,
                matchingCoordinates = emptyMap(),
            ),
        )
    }
}
