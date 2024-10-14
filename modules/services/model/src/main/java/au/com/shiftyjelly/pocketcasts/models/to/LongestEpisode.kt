package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo
import kotlin.time.Duration.Companion.seconds

data class LongestEpisode(
    @ColumnInfo(name = "uuid") val episodeId: String,
    @ColumnInfo(name = "title") val episodeTitle: String,
    @ColumnInfo(name = "podcast_uuid") val podcastId: String,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String,
    @ColumnInfo(name = "duration_seconds") private val durationSeconds: Double,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
) {
    val duration get() = durationSeconds.seconds
}
