package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Upsert
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
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

    @Query("SELECT uuid FROM smart_playlists")
    abstract suspend fun getAllPlaylistUuids(): List<String>

    @Query("SELECT * FROM smart_playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun observeSmartPlaylists(): Flow<List<SmartPlaylist>>

    @Query("UPDATE smart_playlists SET deleted = 1, syncStatus = ${SYNC_STATUS_NOT_SYNCED} WHERE uuid = :uuid")
    abstract suspend fun markPlaylistAsDeleted(uuid: String)

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartPlaylistEpisodeCount(query: RoomRawQuery): Flow<Int>

    fun observeSmartPlaylistEpisodeCount(
        clock: Clock,
        smartRules: SmartRules,
    ): Flow<Int> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "COUNT(*)",
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = null,
            limit = null,
        )
        return observeSmartPlaylistEpisodeCount(RoomRawQuery(query))
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartPlaylistPodcasts(query: RoomRawQuery): Flow<List<Podcast>>

    fun observeSmartPlaylistPodcasts(
        clock: Clock,
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
    ): Flow<List<Podcast>> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "DISTINCT podcast.*",
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = sortType.toOrderByClause(),
            limit = limit,
        )
        return observeSmartPlaylistPodcasts(RoomRawQuery(query))
    }

    private fun createSmartPlaylistEpisodeQuery(
        selectClause: String,
        whereClause: String,
        orderByClause: String?,
        limit: Int?,
    ) = buildString {
        append("SELECT $selectClause ")
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
        NewestToOldest -> "episode.published_date DESC, episode.added_date DESC"
        OldestToNewest -> "episode.published_date ASC, episode.added_date ASC"
        ShortestToLongest -> "episode.duration ASC, episode.added_date DESC"
        LongestToShortest -> "episode.duration DESC, episode.added_date DESC"
    }
}
