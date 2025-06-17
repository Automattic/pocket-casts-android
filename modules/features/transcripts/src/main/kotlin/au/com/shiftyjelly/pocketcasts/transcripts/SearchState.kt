package au.com.shiftyjelly.pocketcasts.transcripts

import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches

data class SearchState(
    val searchTerm: String,
    val matches: SearchMatches,
) {
    companion object {
        val Empty = SearchState(
            searchTerm = "",
            matches = SearchMatches(
                selectedCoordinate = null,
                matchingCoordinates = emptyMap(),
            ),
        )
    }
}
