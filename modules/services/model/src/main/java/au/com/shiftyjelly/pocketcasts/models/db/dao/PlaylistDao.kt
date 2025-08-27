package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Upsert
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPartialFolderSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistShortcut
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.LongestToShortest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.NewestToOldest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.OldestToNewest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.ShortestToLongest
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.utils.extensions.escapeLike
import java.time.Clock
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
    @Upsert
    abstract suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Upsert
    abstract suspend fun upsertAllPlaylists(playlists: Collection<PlaylistEntity>)

    @Upsert
    abstract suspend fun upsertManualEpisode(episode: ManualPlaylistEpisode)

    @Upsert
    abstract suspend fun upsertManualEpisodes(episode: Collection<ManualPlaylistEpisode>)

    @Query("SELECT uuid FROM playlists ORDER BY sortPosition ASC")
    abstract suspend fun getAllPlaylistUuids(): List<String>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 AND uuid = :uuid")
    abstract fun observeSmartPlaylist(uuid: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE manual != 0 AND deleted = 0 AND draft = 0 AND uuid = :uuid")
    abstract fun observeManualPlaylist(uuid: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun getSmartPlaylists(): List<PlaylistEntity>

    @Query(
        """
        SELECT playlist.uuid, playlist.title, playlist.iconId
        FROM playlists AS playlist
        WHERE manual = 0 AND deleted = 0 AND draft = 0
        ORDER BY sortPosition ASC 
        LIMIT 1
    """,
    )
    abstract fun observerPlaylistShortcut(): Flow<PlaylistShortcut?>

    @Query("SELECT * FROM playlists WHERE uuid IN (:uuids)")
    protected abstract suspend fun getAllPlaylistsUnsafe(uuids: Collection<String>): List<PlaylistEntity>

    @Transaction
    open suspend fun getAllPlaylists(uuids: Collection<String>): List<PlaylistEntity> {
        return uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).flatMap { chunk ->
            getAllPlaylistsUnsafe(chunk)
        }
    }

    @Query("SELECT * FROM playlists WHERE draft = 0 AND manual = 0 AND syncStatus = $SYNC_STATUS_NOT_SYNCED")
    abstract suspend fun getAllUnsynced(): List<PlaylistEntity>

    @Query("UPDATE playlists SET sortPosition = :position, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateSortPosition(uuid: String, position: Int)

    @Query("UPDATE playlists SET sortId = :sortType, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType)

    @Query("UPDATE playlists SET autoDownload = :isEnabled, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean)

    @Query("UPDATE playlists SET autoDownloadLimit = :limit, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownloadLimit(uuid: String, limit: Int)

    @Query("UPDATE playlists SET title = :name, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateName(uuid: String, name: String)

    @Query("UPDATE playlists SET deleted = 1, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun markPlaylistAsDeleted(uuid: String)

    @Query("DELETE FROM playlists WHERE deleted = 1")
    abstract suspend fun deleteMarkedPlaylists()

    @Query("DELETE FROM playlists WHERE uuid IN (:uuids)")
    protected abstract suspend fun deleteAllUnsafe(uuids: Collection<String>)

    @Transaction
    open suspend fun deleteAll(uuids: Collection<String>) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            deleteAllUnsafe(chunk)
        }
    }

    @Query(
        """
        SELECT
          COUNT(*) AS episode_count,
          SUM(MAX(0, IFNULL(podcastEpisode.duration, 0) - IFNULL(podcastEpisode.played_up_to, 0))) AS time_left
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS playlistEpisode ON playlistEpisode.playlist_uuid IS playlist.uuid
        LEFT JOIN podcast_episodes AS podcastEpisode ON podcastEpisode.uuid IS playlistEpisode.episode_uuid
        WHERE
          playlist.uuid IS :playlistId
          AND IFNULL(podcastEpisode.archived, 0) IS 0
    """,
    )
    abstract fun observeManualEpisodeMetadata(playlistId: String): Flow<PlaylistEpisodeMetadata>

    @Query(
        """
        SELECT podcast.uuid, podcast.title, podcast.author
        FROM podcasts AS podcast
        LEFT JOIN folders AS folder ON folder.uuid IS podcast.folder_uuid
        WHERE
          podcast.subscribed IS NOT 0
          AND (CASE 
            WHEN :includeInFolders IS NOT 0 THEN 1 
            ELSE folder.deleted IS NOT 0
          END)
        ORDER BY podcast.title ASC
    """,
    )
    internal abstract suspend fun getPodcastPlaylistSources(includeInFolders: Boolean): List<ManualPlaylistPodcastSource>

    @Query(
        """
        SELECT folder.uuid, folder.name 
        FROM folders AS folder
        WHERE 
          folder.deleted IS 0
          AND (
            SELECT COUNT(*) 
            FROM podcasts AS podcast 
            WHERE podcast.subscribed IS NOT 0 AND podcast.folder_uuid IS folder.uuid 
          ) > 0
        ORDER BY folder.name ASC
    """,
    )
    internal abstract suspend fun getFolderPartialPlaylistSources(): List<ManualPlaylistPartialFolderSource>

    @Query("SELECT uuid FROM podcasts WHERE subscribed IS NOT 0 AND folder_uuid IS :folderUuid")
    internal abstract suspend fun getFolderPodcastUuids(folderUuid: String): List<String>

    @Transaction
    open suspend fun getManualPlaylistEpisodeSources(useFolders: Boolean): List<ManualPlaylistEpisodeSource> {
        val podcasts = getPodcastPlaylistSources(includeInFolders = !useFolders)
        val folders = if (useFolders) {
            getFolderPartialPlaylistSources().map { partialSource ->
                ManualPlaylistFolderSource(
                    uuid = partialSource.uuid,
                    title = partialSource.title,
                    podcastUuids = getFolderPodcastUuids(partialSource.uuid),
                )
            }
        } else {
            emptyList()
        }
        return podcasts + folders
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartEpisodeMetadata(query: RoomRawQuery): Flow<PlaylistEpisodeMetadata>

    fun observeSmartEpisodeMetadata(
        clock: Clock,
        smartRules: SmartRules,
    ): Flow<PlaylistEpisodeMetadata> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "COUNT(*) AS episode_count, SUM(MAX(episode.duration - episode.played_up_to, 0)) AS time_left",
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = null,
            limit = null,
        )
        return observeSmartEpisodeMetadata(RoomRawQuery(query))
    }

    @Query(
        """
        SELECT DISTINCT podcast.uuid
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS playlistEpisode ON playlistEpisode.playlist_uuid IS playlist.uuid
        JOIN podcast_episodes AS podcastEpisode ON podcastEpisode.uuid IS playlistEpisode.episode_uuid
        JOIN podcasts AS podcast ON podcast.uuid IS playlistEpisode.podcast_uuid
        WHERE
          playlist.uuid IS :playlistId
          AND podcastEpisode.archived IS 0
        ORDER BY
          -- newest to oldest
          CASE WHEN playlist.sortId IS 0 THEN podcastEpisode.published_date END DESC,
          CASE WHEN playlist.sortId IS 0 THEN podcastEpisode.added_date END DESC,
          -- oldest to newest
          CASE WHEN playlist.sortId IS 1 THEN podcastEpisode.published_date END ASC,
          CASE WHEN playlist.sortId IS 1 THEN podcastEpisode.added_date END ASC,
          -- shortest to longest
          CASE WHEN playlist.sortId IS 2 THEN podcastEpisode.duration END ASC,
          CASE WHEN playlist.sortId IS 2 THEN podcastEpisode.added_date END DESC,
          -- longest to shortest
          CASE WHEN playlist.sortId IS 3 THEN podcastEpisode.duration END DESC,
          CASE WHEN playlist.sortId IS 4 THEN podcastEpisode.added_date END DESC
        LIMIT 4
    """,
    )
    abstract fun observeManualPlaylistPodcasts(playlistId: String): Flow<List<String>>

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartPlaylistPodcasts(query: RoomRawQuery): Flow<List<String>>

    fun observeSmartPlaylistPodcasts(
        clock: Clock,
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
    ): Flow<List<String>> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "DISTINCT podcast.uuid",
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = sortType.toOrderByClause(),
            limit = limit,
        )
        return observeSmartPlaylistPodcasts(RoomRawQuery(query))
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun observeSmartPlaylistEpisodes(query: RoomRawQuery): Flow<List<PodcastEpisode>>

    fun observeSmartPlaylistEpisodes(
        clock: Clock,
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
        searchTerm: String? = null,
    ): Flow<List<PodcastEpisode>> {
        val escapedTerm = searchTerm?.takeIf(String::isNotBlank)?.escapeLike('\\')
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "episode.*",
            whereClause = buildString {
                append(smartRules.toSqlWhereClause(clock))
                if (escapedTerm != null) {
                    append(" AND (")
                    append("episode.title LIKE '%' || '$escapedTerm' || '%' ESCAPE '\\' COLLATE NOCASE")
                    append(" OR ")
                    append("podcast.title LIKE '%' || '$escapedTerm' || '%' ESCAPE '\\' COLLATE NOCASE")
                    append(')')
                }
            },
            orderByClause = sortType.toOrderByClause(),
            limit = limit,
        )
        return observeSmartPlaylistEpisodes(RoomRawQuery(query))
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
