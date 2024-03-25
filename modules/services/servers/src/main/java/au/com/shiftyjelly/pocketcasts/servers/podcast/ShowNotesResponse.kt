package au.com.shiftyjelly.pocketcasts.servers.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShowNotesResponse(
    @Json(name = "podcast") val podcast: ShowNotesPodcast?,
) {
    fun findEpisode(episodeUuid: String): ShowNotesEpisode? {
        return podcast?.episodes?.find { it.uuid == episodeUuid }
    }
}

@JsonClass(generateAdapter = true)
data class ShowNotesPodcast(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "episodes") val episodes: List<ShowNotesEpisode>?,
)

@JsonClass(generateAdapter = true)
data class ShowNotesEpisode(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "show_notes") val showNotes: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "chapters") val chapters: List<ShowNotesChapter>? = null,
    @Json(name = "chapters_url") val chaptersUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ShowNotesChapter(
    @Json(name = "startTime") val startTime: Double,
    @Json(name = "endTime") val endTime: Double? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "img") val image: String? = null,
    @Json(name = "url") val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class RawChaptersResponse(
    @Json(name = "chapters") val chapters: List<ShowNotesChapter>? = null,
)
