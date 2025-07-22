package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Upsert
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
    protected abstract fun observeSmartPlaylistEpisodeCount(query: RoomRawQuery): Flow<Int>

    fun observeSmartPlaylistEpisodeCount(
        smartRules: SmartRules,
        limit: Int,
    ): (Clock, playlistId: Long?) -> Flow<Int> = { clock, playlistId ->
        val query = buildString {
            append("SELECT COUNT(*) FROM (")
            append(
                createSmartPlaylistEpisodeQuery(
                    whereClause = smartRules.toSqlWhereClause(clock, playlistId),
                    orderByClause = null,
                    limit = limit,
                ),
            )
            append(')')
        }
        observeSmartPlaylistEpisodeCount(RoomRawQuery(query))
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartPlaylistEpisodes(query: RoomRawQuery): Flow<List<PodcastEpisode>>

    fun observeSmartPlaylistEpisodeUuids(
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
    ): (Clock, playlistId: Long?) -> Flow<List<PodcastEpisode>> = { clock, playlistId ->
        val query = createSmartPlaylistEpisodeQuery(
            whereClause = smartRules.toSqlWhereClause(clock, playlistId),
            orderByClause = sortType.toOrderByClause(),
            limit = limit,
        )
        observeSmartPlaylistEpisodes(RoomRawQuery(query))
    }

    private fun createSmartPlaylistEpisodeQuery(
        whereClause: String,
        orderByClause: String?,
        limit: Int?,
    ) = buildString {
        append("SELECT episode.* ")
        append("FROM podcast_episodes AS episode ")
        append("JOIN podcasts AS podcast ON episode.podcast_id = podcast.uuid ")
        append("WHERE $whereClause ")
        if (orderByClause != null) {
            append("ORDER BY $orderByClause ")
        }
        if (limit != null) {
            append("LIMIT $limit")
        }
    }

    private fun PlaylistEpisodeSortType.toOrderByClause() = when (this) {
        NewestToOldest -> "published_date DESC, episode.added_date DESC"
        OldestToNewest -> "episode.published_date ASC, episode.added_date ASC"
        ShortestToLongest -> "duration ASC, episode.added_date DESC"
        LongestToShortest -> "duration DESC, episode.added_date DESC"
        LastDownloadAttempt -> "IFNULL(episode.last_download_attempt_date, -1) DESC, episode.published_date DESC"
    }
}
