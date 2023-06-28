package au.com.shiftyjelly.pocketcasts.servers.podcast

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ShowNotesResponse(
    @field:Json(name = "podcast") val podcast: ShowNotesPodcast?
) {
    fun findEpisode(episodeUuid: String): ShowNotesEpisode? {
        return podcast?.episodes?.find { it.uuid == episodeUuid }
    }
}

@JsonClass(generateAdapter = true)
data class ShowNotesPodcast(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "episodes") val episodes: List<ShowNotesEpisode>?
)

@JsonClass(generateAdapter = true)
data class ShowNotesEpisode(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "show_notes") val showNotes: String?
)
