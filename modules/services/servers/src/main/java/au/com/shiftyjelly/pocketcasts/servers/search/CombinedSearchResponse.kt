package au.com.shiftyjelly.pocketcasts.servers.search

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import java.util.Date

@JsonClass(generateAdapter = true)
data class CombinedSearchResponse(
    val results: List<CombinedResult>,
)

sealed interface CombinedResult {
    @JsonClass(generateAdapter = true)
    data class PodcastResult(
        val uuid: String,
        val title: String,
        val author: String? = "",
        val slug: String,
    ) : CombinedResult

    @JsonClass(generateAdapter = true)
    data class EpisodeResult(
        val uuid: String,
        val title: String,
        @Json(name = "published_date")
        val publishedDate: Date = Date(),
        val url: String,
        val duration: Long = 0L,
        @Json(name = "podcast_uuid")
        val podcastUuid: String,
        @Json(name = "podcast_title")
        val podcastTitle: String,
        @Json(name = "podcast_slug")
        val podcastSlug: String,
    ) : CombinedResult

    companion object {
        val jsonAdapter = PolymorphicJsonAdapterFactory.of(CombinedResult::class.java, "type")
            .withSubtype(PodcastResult::class.java, "podcast")
            .withSubtype(EpisodeResult::class.java, "episode")
    }
}
