package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FilterListResponse(
    @Json(name = "playlists") val filters: List<FilterResponse>?,
)

@JsonClass(generateAdapter = true)
data class FilterResponse(
    @Json(name = "uuid") val uuid: String?,
    @Json(name = "isDeleted") val deleted: Boolean?,
    @Json(name = "title") val title: String?,
    @Json(name = "audioVideo") val audioVideo: Int?,
    @Json(name = "notDownloaded") val notDownloaded: Boolean?,
    @Json(name = "downloaded") val downloaded: Boolean?,
    @Json(name = "downloading") val downloading: Boolean?,
    @Json(name = "finished") val finished: Boolean?,
    @Json(name = "partiallyPlayed") val partiallyPlayed: Boolean?,
    @Json(name = "unplayed") val unplayed: Boolean?,
    @Json(name = "starred") val starred: Boolean?,
    @Json(name = "manual") val manual: Boolean?,
    @Json(name = "sortPosition") val sortPosition: Int?,
    @Json(name = "sortType") val sortType: Int?,
    @Json(name = "iconId") val iconId: Int?,
    @Json(name = "allPodcasts") val allPodcasts: Boolean?,
    @Json(name = "filterHours") val filterHours: Int?,
    @Json(name = "podcastUuids") val podcastUuids: String?,
    @Json(name = "episodeUuids") val episodeUuids: String?,
    @Json(name = "filterDuration") val filterDuration: Boolean?,
    @Json(name = "longerThan") val longerThan: Int?,
    @Json(name = "shorterThan") val shorterThan: Int?,
) {

    fun toFilter(): PlaylistEntity? {
        if (uuid == null) {
            return null
        }
        val filter = PlaylistEntity(uuid = uuid)
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
        notDownloaded?.let { filter.notDownloaded = it }
        sortType?.let(PlaylistEpisodeSortType::fromServerId)?.let { filter.sortType = it }
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
