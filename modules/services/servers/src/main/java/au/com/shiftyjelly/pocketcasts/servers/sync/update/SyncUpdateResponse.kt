package au.com.shiftyjelly.pocketcasts.servers.sync.update

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import com.pocketcasts.service.api.SyncUserPodcast
import com.pocketcasts.service.api.autoSkipLastOrNull
import com.pocketcasts.service.api.autoStartFromOrNull
import com.pocketcasts.service.api.dateAddedOrNull
import com.pocketcasts.service.api.episodesSortOrderOrNull
import com.pocketcasts.service.api.folderUuidOrNull
import com.pocketcasts.service.api.podcastOrNull
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.subscribedOrNull
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date

data class SyncUpdateResponse(
    var lastModified: String? = null,
    var token: String? = null,
    val playlists: MutableList<Playlist> = mutableListOf(),
    val episodes: MutableList<EpisodeSync> = mutableListOf(),
    val podcasts: MutableList<PodcastSync> = mutableListOf(),
    val folders: MutableList<Folder> = mutableListOf(),
    val bookmarks: MutableList<Bookmark> = mutableListOf(),
) {

    data class PodcastSync(
        var uuid: String? = null,
        var subscribed: Boolean = false,
        var startFromSecs: Int? = null,
        var episodesSortOrder: Int? = null,
        var skipLastSecs: Int? = null,
        var folderUuid: String? = null,
        var sortPosition: Int? = null,
        var dateAdded: Date? = null,
    ) {
        companion object {
            fun fromSyncUserPodcast(syncUserPodcast: SyncUserPodcast): PodcastSync =
                PodcastSync(
                    uuid = syncUserPodcast.uuid,
                    startFromSecs = syncUserPodcast.autoStartFromOrNull?.value,
                    episodesSortOrder = syncUserPodcast.episodesSortOrderOrNull?.value,
                    skipLastSecs = syncUserPodcast.autoSkipLastOrNull?.value,
                    folderUuid = syncUserPodcast.folderUuidOrNull?.value,
                    sortPosition = syncUserPodcast.sortPositionOrNull?.value,
                    dateAdded = syncUserPodcast.dateAddedOrNull?.toDate(),
                ).apply {
                    syncUserPodcast.subscribedOrNull?.value?.let { subscribed = it }
                }
        }
    }

    data class EpisodeSync(
        var uuid: String? = null,
        var isArchived: Boolean? = null,
        var starred: Boolean? = null,
        var playedUpTo: Double? = null,
        var duration: Double? = null,
        var playingStatus: EpisodePlayingStatus? = null,
    )

    companion object {
        fun fromProtobufSyncUpdateResponse(
            source: com.pocketcasts.service.api.SyncUpdateResponse,
        ): SyncUpdateResponse {
            val podcasts = source.recordsList
                .mapNotNull { it.podcastOrNull }
                .map { PodcastSync.fromSyncUserPodcast(it) }

            return SyncUpdateResponse(
                lastModified = run {
                    val epochMilli = source.lastModified
                    val instant = Instant.ofEpochMilli(epochMilli)
                    DateTimeFormatter.ISO_INSTANT.format(instant)
                },
                podcasts = podcasts.toMutableList(),
            )
        }
    }
}
