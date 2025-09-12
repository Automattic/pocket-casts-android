package au.com.shiftyjelly.pocketcasts.models.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "manual_playlist_episodes",
    primaryKeys = ["playlist_uuid", "episode_uuid"],
    indices = [
        Index(value = ["playlist_uuid"], name = "manual_playlist_episodes_playlist_uuid_index", unique = false),
        Index(value = ["episode_uuid"], name = "manual_playlist_episodes_episode_uuid_index", unique = false),
        Index(value = ["podcast_uuid"], name = "manual_playlist_episodes_podcast_uuid_index", unique = false),
        Index(value = ["playlist_uuid", "is_synced"], name = "manual_playlist_episodes_playlist_uuid_is_synced_index", unique = false),
    ],
)
data class ManualPlaylistEpisode(
    @ColumnInfo(name = "playlist_uuid") val playlistUuid: String,
    @ColumnInfo(name = "episode_uuid") val episodeUuid: String,
    @ColumnInfo(name = "podcast_uuid") val podcastUuid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "added_at") val addedAt: Instant,
    @ColumnInfo(name = "published_at") val publishedAt: Instant,
    @ColumnInfo(name = "download_url") val downloadUrl: String?,
    @ColumnInfo(name = "episode_slug") val episodeSlug: String,
    @ColumnInfo(name = "podcast_slug") val podcastSlug: String,
    @ColumnInfo(name = "sort_position") val sortPosition: Int,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean,
) {
    companion object {
        fun test(
            playlistUuid: String = "playlist-id",
            episodeUuid: String = "episode-id",
            podcastUuid: String = "podcast-id",
            title: String = "Episode Title",
            addedAt: Instant = Instant.EPOCH,
            publishedAt: Instant = Instant.EPOCH,
            downloadUrl: String? = null,
            episodeSlug: String = "episode-slug",
            podcastSlug: String = "podcast-slug",
            sortPosition: Int = 0,
            isSynced: Boolean = true,
        ) = ManualPlaylistEpisode(
            playlistUuid = playlistUuid,
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
            title = title,
            addedAt = addedAt,
            publishedAt = publishedAt,
            downloadUrl = downloadUrl,
            episodeSlug = episodeSlug,
            podcastSlug = podcastSlug,
            sortPosition = sortPosition,
            isSynced = isSynced,
        )
    }
}
