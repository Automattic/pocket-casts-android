package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo

data class NovaLauncherRecentlyPlayedPodcast(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "podcast_category") val categories: String,
    @ColumnInfo(name = "initial_release_timestamp") val initialReleaseTimestamp: Long?,
    @ColumnInfo(name = "latest_release_timestamp") val latestReleaseTimestamp: Long?,
    @ColumnInfo(name = "last_used_timestamp") val lastUsedTimestamp: Long,
)
