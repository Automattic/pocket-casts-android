package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Observable

@JsonClass(generateAdapter = true)
data class PodcastsResponse(
    @Json(name = "result") val result: Result?,
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
    @Json(name = "podcasts") val podcasts: List<ResultPodcast>?,
)

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
