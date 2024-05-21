package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

data class NovaLauncherNewEpisode(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "podcast_id") val podcastId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "duration") val duration: Long,
    @ColumnInfo(name = "current_position") val currentPosition: Long,
    @ColumnInfo(name = "season_number") val seasonNumber: Int?,
    @ColumnInfo(name = "episode_number") val episodeNumber: Int?,
    @ColumnInfo(name = "release_timestamp") val releaseTimestamp: Long,
    @ColumnInfo(name = "last_used_timestamp") val lastUsedTimestamp: Long?,
)
