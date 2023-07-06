package au.com.shiftyjelly.pocketcasts.servers.sync.update

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.util.Date

data class SyncUpdateResponse(
    var lastModified: String? = null,
    var token: String? = null,
    val playlists: MutableList<Playlist> = mutableListOf(),
    val episodes: MutableList<EpisodeSync> = mutableListOf(),
    val podcasts: MutableList<PodcastSync> = mutableListOf(),
    val folders: MutableList<Folder> = mutableListOf(),
    val bookmarks: MutableList<Bookmark> = mutableListOf()
) {

    data class PodcastSync(
        var uuid: String? = null,
        var subscribed: Boolean = false,
        var startFromSecs: Int? = null,
        var episodesSortOrder: Int? = null,
        var skipLastSecs: Int? = null,
        var folderUuid: String? = null,
        var sortPosition: Int? = null,
        var dateAdded: Date? = null
    )

    data class EpisodeSync(
        var uuid: String? = null,
        var isArchived: Boolean? = null,
        var starred: Boolean? = null,
        var playedUpTo: Double? = null,
        var duration: Double? = null,
        var playingStatus: EpisodePlayingStatus? = null
    )
}
