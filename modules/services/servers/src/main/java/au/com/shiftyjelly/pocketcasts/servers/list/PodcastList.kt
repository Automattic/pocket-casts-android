package au.com.shiftyjelly.pocketcasts.servers.list

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PodcastList(
    @field:Json(name = "title") val title: String,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "podcasts") val podcasts: List<ListPodcast>,
    @field:Json(name = "datetime") val date: String?,
    @field:Json(name = "h") val hash: String?
) {
    val fullPodcasts: List<Podcast>
        get() = podcasts.map { it.toPodcast() }
}

@JsonClass(generateAdapter = true)
data class ListPodcast(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "author") val author: String
) {
    companion object {
        fun fromPodcast(podcast: Podcast): ListPodcast {
            return ListPodcast(
                uuid = podcast.uuid,
                title = podcast.title,
                author = podcast.author
            )
        }
    }

    fun toPodcast(): Podcast {
        return Podcast(
            uuid = uuid,
            title = title,
            author = author
        )
    }
}
