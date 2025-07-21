package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

data class EpisodeUuids(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String,
)
