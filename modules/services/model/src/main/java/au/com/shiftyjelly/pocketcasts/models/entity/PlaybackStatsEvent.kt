package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playback_stats_events",
    indices = [
        Index(value = ["started_at_ms"], name = "playback_stats_events_started_at_ms"),
        Index(value = ["podcast_uuid"], name = "playback_stats_events_podcast_uuid"),
        Index(value = ["podcast_category"], name = "playback_stats_events_podcast_category"),
    ],
)
data class PlaybackStatsEvent(
    @PrimaryKey @ColumnInfo(name = "uuid") val uuid: String,
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String,
    @ColumnInfo(name = "episode_title") val episodeTitle: String,
    @ColumnInfo(name = "podcast_title") val podcastTitle: String,
    @ColumnInfo(name = "podcast_category") val podcastCategory: String,
    @ColumnInfo(name = "started_at_ms") val startedAtMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean,
)
