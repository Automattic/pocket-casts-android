package au.com.shiftyjelly.pocketcasts.repositories.transcript

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EpisodeMetaResponse(
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "chapters") val chapters: List<MetaChapter>? = null,
) {
    @JsonClass(generateAdapter = true)
    data class MetaChapter(
        @Json(name = "title") val title: String? = null,
        @Json(name = "startTime") val startTime: Long? = null,
    )
}
