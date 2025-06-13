package au.com.shiftyjelly.pocketcasts.transcripts

data class SearchState(
    val searchTerm: String?,
    val selectedSearchCoordinates: SearchCoordinates?,
    val searchResultIndices: Map<Int, List<Int>>,
)

data class SearchCoordinates(
    val lineIndex: Int,
    val matchIndex: Int,
)
