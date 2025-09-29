package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AutoCompleteResponse(
    val results: List<AutoCompleteResult>,
)

sealed class AutoCompleteResult {
    @JsonClass(generateAdapter = true)
    data class TermResult(
        val value: String,
    ): AutoCompleteResult()

    @JsonClass(generateAdapter = true)
    data class PodcastResult(
        val value: PodcastResultValue
    ) : AutoCompleteResult()
}

@JsonClass(generateAdapter = true)
data class PodcastResultValue(
    val uuid: String,
    val title: String,
    val author: String,
)