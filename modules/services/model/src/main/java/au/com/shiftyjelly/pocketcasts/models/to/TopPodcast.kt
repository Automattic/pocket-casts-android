package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo
import kotlin.time.Duration.Companion.seconds

data class TopPodcast(
    @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "playback_time_seconds") private val playbackTimeSeconds: Double,
    @ColumnInfo(name = "played_episode_count") val playedEpisodeCount: Int,
) {
    val playbackTime get() = playbackTimeSeconds.seconds
}
