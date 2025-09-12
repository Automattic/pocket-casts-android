package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

data class PlaylistEpisodeMetadata(
    @ColumnInfo("episode_count") val episodeCount: Int,
    @ColumnInfo("archived_episode_count") val archivedEpisodeCount: Int,
    @ColumnInfo("time_left") val timeLeftSeconds: Double,
) {
    companion object {
        val Empty = PlaylistEpisodeMetadata(
            episodeCount = 0,
            archivedEpisodeCount = 0,
            timeLeftSeconds = 0.0,
        )
    }
}
