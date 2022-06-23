package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Observable

@JsonClass(generateAdapter = true)
data class PodcastsResponse(
    @field:Json(name = "result") val result: Result?
) {

    fun toPodcasts(): Observable<Podcast> {
        val podcasts = mutableListOf<Podcast>()
        result?.podcasts?.forEach {
            podcasts.add(it.toPodcast())
        }
        return Observable.fromIterable(podcasts)
    }
}

@JsonClass(generateAdapter = true)
data class Result(
    @field:Json(name = "podcasts") val podcasts: List<ResultPodcast>?
)

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
        val podcast = Podcast()
        podcast.uuid = uuid
        podcast.title = title ?: ""
        podcast.podcastUrl = url
        podcast.author = author ?: ""
        podcast.podcastCategory = category ?: ""
        podcast.podcastDescription = description ?: ""
        podcast.podcastLanguage = language ?: ""
        podcast.mediaType = mediaType
        return podcast
    }
}
