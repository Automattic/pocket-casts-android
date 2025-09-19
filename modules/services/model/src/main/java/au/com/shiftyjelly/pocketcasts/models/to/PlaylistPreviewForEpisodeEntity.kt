package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class PlaylistPreviewForEpisodeEntity(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "episode_count") val episodeCount: Int,
    @ColumnInfo(name = "has_episode") val hasEpisode: Boolean,
)
