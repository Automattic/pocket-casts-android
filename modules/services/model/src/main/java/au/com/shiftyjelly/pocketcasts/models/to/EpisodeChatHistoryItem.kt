package au.com.shiftyjelly.pocketcasts.models.to

import androidx.room.ColumnInfo
import java.util.Date

data class EpisodeChatHistoryItem(
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "episode_title") val episodeTitle: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String,
    @ColumnInfo(name = "episode_duration") val episodeDuration: Double,
    @ColumnInfo(name = "last_message_at") val lastMessageAt: Date,
) {
    val episodeDurationMs get() = (episodeDuration * 1000).toInt()
}
