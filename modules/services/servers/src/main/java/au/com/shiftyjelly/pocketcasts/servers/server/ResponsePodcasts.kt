package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResponsePodcasts(@Json(name = "result") val result: Result?)

@JsonClass(generateAdapter = true)
data class Result(@Json(name = "podcasts") val podcasts: List<ResultPodcast>?)

@JsonClass(generateAdapter = true)
data class ResultPodcast(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "media_type") val mediaType: String?,
) {

    fun toPodcast(): Podcast {
        return Podcast(
            uuid = uuid,
            title = title ?: "",
            podcastUrl = url,
            author = author ?: "",
            podcastCategory = category ?: "",
            podcastDescription = description ?: "",
            podcastLanguage = language ?: "",
            mediaType = mediaType,
        )
    }
}
