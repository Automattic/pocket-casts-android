package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

sealed interface ManualPlaylistEpisodeSource

data class ManualPlaylistPodcastSource(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "author") val author: String,
) : ManualPlaylistEpisodeSource

data class ManualPlaylistFolderSource(
    val uuid: String,
    val title: String,
    val podcastSources: List<ManualPlaylistPodcastSource>,
) : ManualPlaylistEpisodeSource

internal data class ManualPlaylistPartialFolderSource(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "name") val title: String,
)
