package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FilterListResponse(
    @field:Json(name = "playlists") val filters: List<FilterResponse>?
)

@JsonClass(generateAdapter = true)
data class FilterResponse(
    @field:Json(name = "uuid") val uuid: String?,
    @field:Json(name = "isDeleted") val deleted: Boolean?,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "audioVideo") val audioVideo: Int?,
    @field:Json(name = "notDownloaded") val notDownloaded: Boolean?,
    @field:Json(name = "downloaded") val downloaded: Boolean?,
    @field:Json(name = "downloading") val downloading: Boolean?,
    @field:Json(name = "finished") val finished: Boolean?,
    @field:Json(name = "partiallyPlayed") val partiallyPlayed: Boolean?,
    @field:Json(name = "unplayed") val unplayed: Boolean?,
    @field:Json(name = "starred") val starred: Boolean?,
    @field:Json(name = "manual") val manual: Boolean?,
    @field:Json(name = "sortPosition") val sortPosition: Int?,
    @field:Json(name = "sortType") val sortType: Int?,
    @field:Json(name = "iconId") val iconId: Int?,
    @field:Json(name = "allPodcasts") val allPodcasts: Boolean?,
    @field:Json(name = "filterHours") val filterHours: Int?,
    @field:Json(name = "podcastUuids") val podcastUuids: String?,
    @field:Json(name = "episodeUuids") val episodeUuids: String?,
    @field:Json(name = "filterDuration") val filterDuration: Boolean?,
    @field:Json(name = "longerThan") val longerThan: Int?,
    @field:Json(name = "shorterThan") val shorterThan: Int?
) {

    fun toFilter(): Playlist? {
        if (uuid == null) {
            return null
        }
        val filter = Playlist(uuid = uuid)
        title?.let { filter.title = it }
        sortPosition?.let { filter.sortPosition = it }
        manual?.let { filter.manual = it }
        unplayed?.let { filter.unplayed = it }
        partiallyPlayed?.let { filter.partiallyPlayed = it }
        finished?.let { filter.finished = it }
        audioVideo?.let { filter.audioVideo = it }
        allPodcasts?.let { filter.allPodcasts = it }
        podcastUuids?.let { filter.podcastUuids = it }
        downloaded?.let { filter.downloaded = it }
        downloading?.let { filter.downloading = it }
        notDownloaded?.let { filter.notDownloaded = it }
        sortType?.let { filter.sortId = it }
        iconId?.let { filter.iconId = it }
        filterHours?.let { filter.filterHours = it }
        starred?.let { filter.starred = it }
        deleted?.let { filter.deleted = it }
        filterDuration?.let { filter.filterDuration = it }
        longerThan?.let { filter.longerThan = it }
        shorterThan?.let { filter.shorterThan = it }

        return filter
    }
}
