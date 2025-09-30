package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class AutoCompleteResponse(
    val results: List<AutoCompleteResult>,
)

sealed class AutoCompleteResult {
    @JsonClass(generateAdapter = true)
    data class TermResult(
        val value: String,
    ) : AutoCompleteResult()

    @JsonClass(generateAdapter = true)
    data class PodcastResult(
        val value: PodcastResultValue,
    ) : AutoCompleteResult()

    companion object {
        val jsonAdapter = PolymorphicJsonAdapterFactory.of(AutoCompleteResult::class.java, "type")
            .withSubtype(TermResult::class.java, "term")
            .withSubtype(PodcastResult::class.java, "podcast")
    }
}

@JsonClass(generateAdapter = true)
data class PodcastResultValue(
    val uuid: String,
    val title: String,
    val author: String,
)
