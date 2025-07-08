package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastList(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "podcasts") val podcasts: List<ListPodcast>,
    @Json(name = "datetime") val date: String?,
    @Json(name = "h") val hash: String?,
) {
    val fullPodcasts: List<Podcast>
        get() = podcasts.map { it.toPodcast() }
}

@JsonClass(generateAdapter = true)
data class ListPodcast(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String,
    @Json(name = "author") val author: String,
) {
    companion object {
        fun fromPodcast(podcast: Podcast): ListPodcast {
            return ListPodcast(
                uuid = podcast.uuid,
                title = podcast.title,
                author = podcast.author,
            )
        }
    }

    fun toPodcast(): Podcast {
        return Podcast(
            uuid = uuid,
            title = title,
            author = author,
        )
    }
}
