package au.com.shiftyjelly.pocketcasts.transcripts

data class SearchState(
    val searchTerm: String?,
    val selectedSearchCoordinates: Pair<Int, Int>?,
    val searchResultIndices: Map<Int, List<Int>>,
)
