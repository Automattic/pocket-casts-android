package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

data class EpisodeImageData(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
)
