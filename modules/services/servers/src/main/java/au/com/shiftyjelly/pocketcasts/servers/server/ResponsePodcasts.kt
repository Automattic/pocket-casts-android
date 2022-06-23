package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Single

@JsonClass(generateAdapter = true)
data class ResponsePodcasts(@field:Json(name = "result") val result: Result?) {

    fun toPodcasts(): Single<List<Podcast>> {
        val podcasts = result?.podcasts?.map { it.toPodcast() } ?: emptyList()
        return Single.just(podcasts)
    }
}

@JsonClass(generateAdapter = true)
data class Result(@field:Json(name = "podcasts") val podcasts: List<ResultPodcast>?)

@JsonClass(generateAdapter = true)
data class ResultPodcast(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "author") val author: String?,
    @field:Json(name = "category") val category: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "language") val language: String?,
    @field:Json(name = "media_type") val mediaType: String?
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
            mediaType = mediaType
        )
    }
}
