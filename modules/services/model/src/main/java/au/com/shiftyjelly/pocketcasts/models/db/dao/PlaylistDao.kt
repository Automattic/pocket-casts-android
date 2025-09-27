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
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistPreviewForEpisodeEntity
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistShortcut
import au.com.shiftyjelly.pocketcasts.models.to.RawManualEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.DragAndDrop
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.LongestToShortest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.NewestToOldest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.OldestToNewest
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType.ShortestToLongest
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.utils.extensions.escapeLike
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    @Query("SELECT * FROM playlists WHERE deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query(
        """
        SELECT * 
        FROM playlists
        WHERE deleted IS 0 AND draft IS 0 AND autoDownload IS NOT 0
    """,
    )
    abstract suspend fun getAllAutoDownloadPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE deleted = 0 AND draft = 0 ORDER BY sortPosition ASC")
    abstract fun allPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query(
        """
        SELECT
          playlist.uuid AS uuid,
          playlist.title AS title,
          (
            SELECT
              COUNT(*)
            FROM
              manual_playlist_episodes AS episode
            WHERE
              episode.playlist_uuid IS playlist.uuid
          ) AS episode_count,
          (
            SELECT
              EXISTS(
                SELECT
                  *
                FROM
                  manual_playlist_episodes AS episode
                WHERE
                  episode.playlist_uuid IS playlist.uuid
                  AND episode.episode_uuid IS :episodeUuid
              )
          ) AS has_episode
        FROM
          playlists AS playlist
        WHERE
          deleted IS 0
          AND draft IS 0
          AND playlist.manual IS NOT 0
          AND (
            -- trim isn't really needed because we trim in the application logic but it helps with tests
            TRIM(:searchTerm) IS '' 
            OR playlist.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
          )
        ORDER BY
          sortPosition ASC
    """,
    )
    protected abstract fun playlistPreviewsForEpisodeFlowUnsafe(episodeUuid: String, searchTerm: String): Flow<List<PlaylistPreviewForEpisodeEntity>>

    fun playlistPreviewsForEpisodeFlow(episodeUuid: String, searchTerm: String): Flow<List<PlaylistPreviewForEpisodeEntity>> {
        val escapedTerm = searchTerm.escapeLike('\\')
        return playlistPreviewsForEpisodeFlowUnsafe(episodeUuid, escapedTerm)
    }

    @Query("SELECT * FROM playlists WHERE uuid IN (:uuids)")
    protected abstract suspend fun getAllPlaylistsInUnsafe(uuids: Collection<String>): List<PlaylistEntity>

    @Transaction
    open suspend fun getAllPlaylistsIn(uuids: Collection<String>): List<PlaylistEntity> {
        return uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).flatMap { chunk ->
            getAllPlaylistsInUnsafe(chunk)
        }
    }

    @Query("SELECT * FROM playlists WHERE draft = 0 AND syncStatus = $SYNC_STATUS_NOT_SYNCED")
    abstract suspend fun getAllUnsyncedPlaylists(): List<PlaylistEntity>

    @Query("SELECT uuid FROM playlists ORDER BY sortPosition ASC")
    abstract suspend fun getAllPlaylistUuids(): List<String>

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 AND uuid = :uuid")
    abstract suspend fun getSmartPlaylistFlow(uuid: String): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE manual = 0 AND deleted = 0 AND draft = 0 AND uuid = :uuid")
    abstract fun smartPlaylistFlow(uuid: String): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE manual != 0 AND deleted = 0 AND draft = 0 AND uuid = :uuid")
    abstract fun manualPlaylistFlow(uuid: String): Flow<PlaylistEntity?>

    @Query("UPDATE playlists SET sortPosition = :position, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateSortPosition(uuid: String, position: Int)

    @Query("UPDATE playlists SET title = :name, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateName(uuid: String, name: String)

    @Query("UPDATE playlists SET sortId = :sortType, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType)

    @Query("UPDATE playlists SET autoDownload = :isEnabled, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean)

    @Query("UPDATE playlists SET autoDownloadLimit = :limit, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun updateAutoDownloadLimit(uuid: String, limit: Int)

    @Query("UPDATE playlists SET syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun markPlaylistAsNotSynced(uuid: String)

    @Query(
        """
        UPDATE playlists
        SET showArchivedEpisodes = (CASE 
            WHEN showArchivedEpisodes IS 0 THEN 1 
            ELSE 0
        END)
        WHERE uuid = :uuid    
    """,
    )
    abstract suspend fun toggleIsShowingArchived(uuid: String)

    @Query("UPDATE playlists SET deleted = 1, syncStatus = $SYNC_STATUS_NOT_SYNCED WHERE uuid = :uuid")
    abstract suspend fun markPlaylistAsDeleted(uuid: String)

    @Query("DELETE FROM playlists WHERE deleted = 1")
    abstract suspend fun deleteMarkedPlaylists()

    @Query("DELETE FROM playlists WHERE uuid IN (:uuids)")
    protected abstract suspend fun deleteAllPlaylistsInUnsafe(uuids: Collection<String>)

    @Transaction
    open suspend fun deleteAllPlaylistsIn(uuids: Collection<String>) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            deleteAllPlaylistsInUnsafe(chunk)
        }
    }

    @Query("DELETE FROM manual_playlist_episodes WHERE playlist_uuid IS :playlistUuid")
    abstract suspend fun deleteAllManualEpisodes(playlistUuid: String)

    @Query("DELETE FROM manual_playlist_episodes WHERE playlist_uuid IS :playlistUuid AND episode_uuid IN (:episodeUuids)")
    protected abstract suspend fun deleteAllManualEpisodesInUnsafe(playlistUuid: String, episodeUuids: Collection<String>)

    open suspend fun deleteAllManualEpisodesIn(playlistUuid: String, episodeUuids: Collection<String>) {
        episodeUuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT - 1).forEach { chunk ->
            deleteAllManualEpisodesInUnsafe(playlistUuid, chunk)
        }
    }

    @Query(
        """
        SELECT
          COUNT(*) AS episode_count,
          SUM(IFNULL(podcastEpisode.archived, 0)) AS archived_episode_count,
          SUM(MAX(0, IFNULL(podcastEpisode.duration, 0) - IFNULL(podcastEpisode.played_up_to, 0))) AS time_left
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS playlistEpisode ON playlistEpisode.playlist_uuid IS playlist.uuid
        LEFT JOIN podcast_episodes AS podcastEpisode ON podcastEpisode.uuid IS playlistEpisode.episode_uuid
        WHERE playlist.uuid IS :playlistUuid
    """,
    )
    abstract fun manualPlaylistMetadataFlow(playlistUuid: String): Flow<PlaylistEpisodeMetadata>

    @Query("SELECT episode_uuid FROM manual_playlist_episodes WHERE playlist_uuid IS :playlistUuid")
    abstract suspend fun getManualPlaylistEpisodeUuids(playlistUuid: String): List<String>

    @Query(
        """
        SELECT DISTINCT episode.podcast_uuid
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS episode ON episode.playlist_uuid IS playlist.uuid
        WHERE playlist.deleted IS 0 AND playlist.manual IS NOT 0
    """,
    )
    abstract suspend fun getPodcastsAddedToManualPlaylists(): List<String>

    @Query(
        """
        SELECT DISTINCT episode.episode_uuid
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS episode ON episode.playlist_uuid IS playlist.uuid
        WHERE playlist.deleted IS 0 AND playlist.manual IS NOT 0
    """,
    )
    abstract suspend fun getEpisodesAddedToManualPlaylists(): List<String>

    @Query(
        """
        SELECT manual_episode.*
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS manual_episode ON manual_episode.playlist_uuid IS playlist.uuid
        LEFT JOIN podcast_episodes AS podcast_episode ON podcast_episode.uuid IS manual_episode.episode_uuid
        WHERE playlist.uuid IS :playlistUuid
        ORDER BY
          -- newest to oldest
          CASE WHEN playlist.sortId IS 0 THEN IFNULL(podcast_episode.published_date, manual_episode.published_at) END DESC,
          CASE WHEN playlist.sortId IS 0 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- oldest to newest
          CASE WHEN playlist.sortId IS 1 THEN IFNULL(podcast_episode.published_date, manual_episode.published_at) END ASC,
          CASE WHEN playlist.sortId IS 1 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END ASC,
          -- shortest to longest
          CASE WHEN playlist.sortId IS 2 THEN IFNULL(podcast_episode.duration, 9223372036854775807) END ASC,
          CASE WHEN playlist.sortId IS 2 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- longest to shortest
          CASE WHEN playlist.sortId IS 3 THEN IFNULL(podcast_episode.duration, -9223372036854775808) END DESC,
          CASE WHEN playlist.sortId IS 3 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- drag and drop
          CASE WHEN playlist.sortId IS 4 THEN manual_episode.sort_position END ASC,
          CASE WHEN playlist.sortId IS 4 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC
    """,
    )
    abstract suspend fun getManualPlaylistEpisodes(playlistUuid: String): List<ManualPlaylistEpisode>

    @Query(
        """
        SELECT *
        FROM manual_playlist_episodes
        WHERE manual_playlist_episodes.playlist_uuid IS :playlistUuid
        ORDER BY sort_position ASC
    """,
    )
    abstract suspend fun getManualPlaylistEpisodesForSync(playlistUuid: String): List<ManualPlaylistEpisode>

    @Query(
        """
        SELECT *
        FROM manual_playlist_episodes AS manual_episode
        WHERE NOT EXISTS(
          SELECT 1 
          FROM podcast_episodes AS podcast_episode 
          WHERE podcast_episode.uuid IS manual_episode.episode_uuid
        )
    """,
    )
    abstract suspend fun getAllMissingManualEpisodes(): List<ManualPlaylistEpisode>

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
          AND podcast.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
        ORDER BY podcast.title ASC
    """,
    )
    internal abstract suspend fun getAllPodcastPlaylistSources(
        includeInFolders: Boolean,
        searchTerm: String,
    ): List<ManualPlaylistPodcastSource>

    @Query(
        """
        SELECT podcast.uuid, podcast.title, podcast.author
        FROM podcasts AS podcast
        WHERE
          podcast.subscribed IS NOT 0
          AND podcast.folder_uuid IS (:folderUuid)
          AND podcast.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
        ORDER BY podcast.title ASC
    """,
    )
    protected abstract suspend fun getPodcastPlaylistSourcesForFolderUnsafe(
        folderUuid: String,
        searchTerm: String,
    ): List<ManualPlaylistPodcastSource>

    suspend fun getPodcastPlaylistSourcesForFolder(
        folderUuid: String,
        searchTerm: String,
    ): List<ManualPlaylistPodcastSource> {
        val escapedTerm = searchTerm.escapeLike('\\')
        return getPodcastPlaylistSourcesForFolderUnsafe(folderUuid, escapedTerm)
    }

    @Query(
        """
        SELECT 
          folder.uuid, 
          folder.name, 
          folder.color 
        FROM 
          folders AS folder 
        WHERE 
          folder.deleted IS 0 
          AND (
            SELECT 
              COUNT(*) 
            FROM 
              podcasts AS podcast 
            WHERE 
              podcast.subscribed IS NOT 0 
              AND podcast.folder_uuid IS folder.uuid
          ) > 0 
          AND (
            folder.name LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE 
            OR (
              SELECT 
                COUNT(*) 
              FROM 
                podcasts AS podcast 
              WHERE 
                podcast.subscribed IS NOT 0 
                AND podcast.folder_uuid IS folder.uuid 
                AND podcast.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
            )
          ) 
        ORDER BY 
          folder.name ASC
    """,
    )
    internal abstract suspend fun getFolderPartialPlaylistSources(
        searchTerm: String,
    ): List<ManualPlaylistPartialFolderSource>

    @Transaction
    open suspend fun getManualPlaylistEpisodeSources(useFolders: Boolean, searchTerm: String): List<ManualPlaylistEpisodeSource> {
        val escapedTerm = searchTerm.escapeLike('\\')
        val podcasts = getAllPodcastPlaylistSources(includeInFolders = !useFolders, escapedTerm)
        val folders = if (useFolders) {
            getFolderPartialPlaylistSources(escapedTerm).map { partialSource ->
                ManualPlaylistFolderSource(
                    uuid = partialSource.uuid,
                    title = partialSource.title,
                    color = partialSource.color,
                    podcastSources = getPodcastPlaylistSourcesForFolder(
                        folderUuid = partialSource.uuid,
                        searchTerm = "",
                    ).map(ManualPlaylistPodcastSource::uuid),
                )
            }
        } else {
            emptyList()
        }
        return podcasts + folders
    }

    @Query(
        """
        SELECT episode.*
        FROM podcast_episodes AS episode
        WHERE
          episode.podcast_id IS :podcastUuid 
          AND episode.uuid NOT IN (
            SELECT manual_episode.episode_uuid
            FROM manual_playlist_episodes AS manual_episode
            WHERE manual_episode.playlist_uuid IS :playlistUuid AND manual_episode.podcast_uuid IS :podcastUuid
          )
          AND episode.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
        ORDER BY
          episode.published_date DESC,
          episode.added_date DESC,
          episode.title ASC
    """,
    )
    protected abstract fun notAddedManualEpisodesFlowUnsafe(
        playlistUuid: String,
        podcastUuid: String,
        searchTerm: String,
    ): Flow<List<PodcastEpisode>>

    fun notAddedManualEpisodesFlow(
        playlistUuid: String,
        podcastUuid: String,
        searchTerm: String,
    ): Flow<List<PodcastEpisode>> {
        val escapedTerm = searchTerm.escapeLike('\\')
        return notAddedManualEpisodesFlowUnsafe(playlistUuid, podcastUuid, escapedTerm)
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun smartPlaylistMetadataFlow(query: RoomRawQuery): Flow<PlaylistEpisodeMetadata>

    fun smartPlaylistMetadataFlow(
        clock: Clock,
        smartRules: SmartRules,
    ): Flow<PlaylistEpisodeMetadata> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = """
                COUNT(*) AS episode_count, 
                0 AS archived_episode_count,
                SUM(MAX(episode.duration - episode.played_up_to, 0)) AS time_left
            """.trimIndent(),
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = null,
            limit = null,
        )
        return smartPlaylistMetadataFlow(RoomRawQuery(query))
    }

    @Query(
        """
        SELECT playlistEpisode.podcast_uuid
        FROM playlists AS playlist
        JOIN manual_playlist_episodes AS playlistEpisode ON playlistEpisode.playlist_uuid IS playlist.uuid
        JOIN podcast_episodes AS podcastEpisode ON podcastEpisode.uuid IS playlistEpisode.episode_uuid
        WHERE
          playlist.uuid IS :playlistUuid
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
          CASE WHEN playlist.sortId IS 3 THEN podcastEpisode.added_date END DESC,
          -- drag and drop
          CASE WHEN playlist.sortId IS 4 THEN playlistEpisode.sort_position END ASC,
          CASE WHEN playlist.sortId IS 4 THEN podcastEpisode.added_date END DESC
    """,
    )
    abstract fun manualPlaylistArtworkPodcastsFlow(playlistUuid: String): Flow<List<String>>

    @Query(
        """
        SELECT
          -- playlist episode columns
          manual_episode.playlist_uuid AS m_playlist_uuid,
          manual_episode.episode_uuid AS m_episode_uuid,
          manual_episode.podcast_uuid AS m_podcast_uuid,
          manual_episode.title AS m_title,
          manual_episode.added_at AS m_added_at,
          manual_episode.published_at AS m_published_at,
          manual_episode.download_url AS m_download_url,
          manual_episode.episode_slug AS m_episode_slug,
          manual_episode.podcast_slug AS m_podcast_slug,
          manual_episode.sort_position AS m_sort_position,
          manual_episode.is_synced AS m_is_synced,
          -- podcast episode columns
          podcast_episode.uuid AS p_uuid,
          podcast_episode.episode_description AS p_episode_description,
          podcast_episode.published_date AS p_published_date,
          podcast_episode.title AS p_title,
          podcast_episode.size_in_bytes AS p_size_in_bytes,
          podcast_episode.episode_status AS p_episode_status,
          podcast_episode.file_type AS p_file_type,
          podcast_episode.duration AS p_duration,
          podcast_episode.download_url AS p_download_url,
          podcast_episode.downloaded_file_path AS p_downloaded_file_path,
          podcast_episode.downloaded_error_details AS p_downloaded_error_details,
          podcast_episode.play_error_details AS p_play_error_details,
          podcast_episode.played_up_to AS p_played_up_to,
          podcast_episode.playing_status AS p_playing_status,
          podcast_episode.podcast_id AS p_podcast_id,
          podcast_episode.added_date AS p_added_date,
          podcast_episode.auto_download_status AS p_auto_download_status,
          podcast_episode.starred AS p_starred,
          podcast_episode.thumbnail_status AS p_thumbnail_status,
          podcast_episode.last_download_attempt_date AS p_last_download_attempt_date,
          podcast_episode.playing_status_modified AS p_playing_status_modified,
          podcast_episode.played_up_to_modified AS p_played_up_to_modified,
          podcast_episode.duration_modified AS p_duration_modified,
          podcast_episode.starred_modified AS p_starred_modified,
          podcast_episode.archived AS p_archived,
          podcast_episode.archived_modified AS p_archived_modified,
          podcast_episode.season AS p_season,
          podcast_episode.number AS p_number,
          podcast_episode.type AS p_type,
          podcast_episode.cleanTitle AS p_cleanTitle,
          podcast_episode.last_playback_interaction_date AS p_last_playback_interaction_date,
          podcast_episode.last_playback_interaction_sync_status AS p_last_playback_interaction_sync_status,
          podcast_episode.exclude_from_episode_limit AS p_exclude_from_episode_limit,
          podcast_episode.download_task_id AS p_download_task_id,
          podcast_episode.last_archive_interaction_date AS p_last_archive_interaction_date,
          podcast_episode.image_url AS p_image_url,
          podcast_episode.deselected_chapters AS p_deselected_chapters,
          podcast_episode.deselected_chapters_modified AS p_deselected_chapters_modified,
          podcast_episode.slug AS p_slug
        FROM manual_playlist_episodes AS manual_episode
        LEFT JOIN podcast_episodes AS podcast_episode ON podcast_episode.uuid IS manual_episode.episode_uuid
        LEFT JOIN podcasts AS podcast ON podcast.uuid IS manual_episode.podcast_uuid
        JOIN playlists AS playlist ON playlist.uuid IS :playlistUuid
        WHERE
          manual_episode.playlist_uuid IS :playlistUuid
          AND (CASE 
            WHEN playlist.showArchivedEpisodes IS NOT 0 THEN 1 
            ELSE IFNULL(podcast_episode.archived, 0) IS 0
          END)
          AND (
            -- trim isn't really needed because we trim in the application logic but it helps with tests
            TRIM(:searchTerm) IS '' 
            OR podcast.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
            OR podcast_episode.title LIKE '%' || :searchTerm || '%' ESCAPE '\' COLLATE NOCASE
          )
        ORDER BY
          -- newest to oldest
          CASE WHEN playlist.sortId IS 0 THEN IFNULL(podcast_episode.published_date, manual_episode.published_at) END DESC,
          CASE WHEN playlist.sortId IS 0 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- oldest to newest
          CASE WHEN playlist.sortId IS 1 THEN IFNULL(podcast_episode.published_date, manual_episode.published_at) END ASC,
          CASE WHEN playlist.sortId IS 1 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END ASC,
          -- shortest to longest
          CASE WHEN playlist.sortId IS 2 THEN IFNULL(podcast_episode.duration, 9223372036854775807) END ASC,
          CASE WHEN playlist.sortId IS 2 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- longest to shortest
          CASE WHEN playlist.sortId IS 3 THEN IFNULL(podcast_episode.duration, -9223372036854775808) END DESC,
          CASE WHEN playlist.sortId IS 3 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC,
          -- drag and drop
          CASE WHEN playlist.sortId IS 4 THEN manual_episode.sort_position END ASC,
          CASE WHEN playlist.sortId IS 4 THEN IFNULL(podcast_episode.added_date, manual_episode.added_at) END DESC
    """,
    )
    internal abstract fun manualEpisodesRawFlow(
        playlistUuid: String,
        searchTerm: String,
    ): Flow<List<RawManualEpisode>>

    fun manualEpisodesFlow(
        playlistUuid: String,
        searchTerm: String,
    ) = manualEpisodesRawFlow(playlistUuid, searchTerm.escapeLike('\\')).map { rawEpisodes ->
        rawEpisodes.map(RawManualEpisode::toEpisode)
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun smartPlaylistArtworkPodcastsFlow(query: RoomRawQuery): Flow<List<String>>

    fun smartPlaylistArtworkPodcastsFlow(
        clock: Clock,
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
    ): Flow<List<String>> {
        val query = createSmartPlaylistEpisodeQuery(
            selectClause = "podcast.uuid",
            whereClause = smartRules.toSqlWhereClause(clock),
            orderByClause = sortType.toOrderByClause(),
            limit = limit,
        )
        return smartPlaylistArtworkPodcastsFlow(RoomRawQuery(query))
    }

    @RawQuery(observedEntities = [Podcast::class, PodcastEpisode::class])
    protected abstract fun smartEpisodesFlow(query: RoomRawQuery): Flow<List<PodcastEpisode>>

    fun smartEpisodesFlow(
        clock: Clock,
        smartRules: SmartRules,
        sortType: PlaylistEpisodeSortType,
        limit: Int,
        searchTerm: String? = null,
    ): Flow<List<PlaylistEpisode.Available>> {
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
        return smartEpisodesFlow(RoomRawQuery(query)).map { episodes -> episodes.map(PlaylistEpisode::Available) }
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
        // Drag & drop is not supported for smart playlists.
        // Fall back to newest to oldest instead.
        NewestToOldest, DragAndDrop -> "episode.published_date DESC, episode.added_date DESC"
        OldestToNewest -> "episode.published_date ASC, episode.added_date ASC"
        ShortestToLongest -> "episode.duration ASC, episode.added_date DESC"
        LongestToShortest -> "episode.duration DESC, episode.added_date DESC"
    }

    @Query(
        """
        SELECT
          playlist.uuid,
          playlist.title,
          playlist.manual,
          playlist.iconId
        FROM playlists AS playlist
        WHERE 
          playlist.deleted = 0 
          AND playlist.draft = 0
          AND (CASE 
            WHEN :allowManual IS NOT 0 THEN 1 
            ELSE playlist.manual = 0
          END)
        ORDER BY playlist.sortPosition ASC 
        LIMIT 1
    """,
    )
    abstract fun playlistShortcutFlow(allowManual: Boolean): Flow<PlaylistShortcut?>
}
