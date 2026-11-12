package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo

/**
 * A podcast's listening in a year, for Playback: time listened and the episodes played.
 */
data class PlaybackPodcastStats(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "played_seconds") val playedSeconds: Double,
    @ColumnInfo(name = "played_episode_count") val playedEpisodeCount: Int,
)

/**
 * When an episode was last played and how far in, for grouping listening time by month.
 */
data class PlaybackListeningTime(
    @ColumnInfo(name = "interaction_epoch_ms") val interactionEpochMs: Long,
    @ColumnInfo(name = "played_seconds") val playedSeconds: Double,
)

/**
 * Episodes listened to in a year, and how many of them were finished.
 */
data class PlaybackCompletion(
    @ColumnInfo(name = "started") val started: Int,
    @ColumnInfo(name = "completed") val completed: Int,
)
