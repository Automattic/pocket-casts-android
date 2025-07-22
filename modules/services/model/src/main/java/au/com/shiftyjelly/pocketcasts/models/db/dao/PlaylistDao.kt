package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Upsert
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeImageData
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.LastDownloadAttempt
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.LongestToShortest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.NewestToOldest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.OldestToNewest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.ShortestToLongest
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import java.time.Clock
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
    @Upsert
    abstract suspend fun upsertSmartPlaylist(playlist: SmartPlaylist)

    @Upsert
    abstract suspend fun upsertSmartPlaylists(playlists: List<SmartPlaylist>)

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun observeSmartPlaylists(): Flow<List<SmartPlaylist>>

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeEpisodeUuids(query: RoomRawQuery): Flow<List<EpisodeImageData>>

    fun observeSmartPlaylistEpisodeUuids(
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
    ): (Clock, playlistId: Long?) -> Flow<List<EpisodeImageData>> = { clock, playlistId ->
        val orderByClause = when (sortType) {
            NewestToOldest -> "published_date DESC, episode.added_date DESC"
            OldestToNewest -> "episode.published_date ASC, episode.added_date ASC"
            ShortestToLongest -> "duration ASC, episode.added_date DESC"
            LongestToShortest -> "duration DESC, episode.added_date DESC"
            LastDownloadAttempt -> "IFNULL(episode.last_download_attempt_date, -1) DESC, episode.published_date DESC"
        }
        val query = RoomRawQuery(
            sql = """
                |SELECT
                |  episode.uuid AS episode_uuid,
                |  episode.podcast_id AS podcast_uuid,
                |  episode.image_url AS image_url
                |FROM
                |  podcast_episodes AS episode
                |JOIN
                |  podcasts AS podcast
                |  ON episode.podcast_id = podcast.uuid  
                |WHERE
                |  ${smartRules.toSqlWhereClause(clock, playlistId)}
                |ORDER BY
                |  $orderByClause
                |LIMIT
                |  $limit
            """.trimMargin(),
        )
        observeEpisodeUuids(query)
    }
}
