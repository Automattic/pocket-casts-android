package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.db.helper.PodcastBookmark
import au.com.shiftyjelly.pocketcasts.models.db.helper.ProfileBookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(bookmark: Bookmark)

    @Update
    abstract suspend fun update(bookmark: Bookmark)

    @Delete
    abstract suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE uuid = :uuid")
    abstract suspend fun deleteByUuid(uuid: String)

    @Query("SELECT * FROM bookmarks WHERE uuid = :uuid AND deleted = :deleted")
    abstract suspend fun findByUuid(
        uuid: String,
        deleted: Boolean,
    ): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND time = :timeSecs AND deleted = :deleted LIMIT 1")
    abstract suspend fun findByEpisodeTime(
        podcastUuid: String,
        episodeUuid: String,
        timeSecs: Int,
        deleted: Boolean = false,
    ): Bookmark?

    @Query(
        "SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND deleted = :deleted " +
            "ORDER BY " +
            "CASE WHEN :isAsc = 1 THEN created_at END ASC, " +
            "CASE WHEN :isAsc = 0 THEN created_at END DESC",
    )
    abstract fun findByEpisodeOrderCreatedAtFlow(
        podcastUuid: String,
        episodeUuid: String,
        deleted: Boolean = false,
        isAsc: Boolean,
    ): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND deleted = :deleted ORDER BY time ASC")
    abstract fun findByEpisodeOrderTimeFlow(
        podcastUuid: String,
        episodeUuid: String,
        deleted: Boolean = false,
    ): Flow<List<Bookmark>>

    @Query(
        """SELECT bookmarks.*, podcast_episodes.title as episodeTitle, podcast_episodes.published_date as publishedDate
            FROM bookmarks
            JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid 
            WHERE podcast_uuid = :podcastUuid AND deleted = :deleted
            ORDER BY 
            CASE WHEN :isAsc = 1 THEN created_at END ASC, 
            CASE WHEN :isAsc = 0 THEN created_at END DESC""",
    )
    abstract fun findByPodcastOrderCreatedAtFlow(
        podcastUuid: String,
        isAsc: Boolean,
        deleted: Boolean = false,
    ): Flow<List<PodcastBookmark>>

    @Query(
        """SELECT bookmarks.*, podcast_episodes.title as episodeTitle, podcast_episodes.published_date as publishedDate
            FROM bookmarks
            JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid 
            WHERE podcast_uuid = :podcastUuid AND deleted = :deleted
            ORDER BY publishedDate DESC, time ASC""",
    )
    abstract fun findByPodcastOrderEpisodeAndTimeFlow(
        podcastUuid: String,
        deleted: Boolean = false,
    ): Flow<List<PodcastBookmark>>

    @Query(
        """SELECT *
            FROM bookmarks
            WHERE deleted = :deleted""",
    )
    abstract fun findBookmarksFlow(deleted: Boolean = false): Flow<List<Bookmark>>

    @Query(
        """SELECT *
            FROM bookmarks
            WHERE deleted = :deleted
            ORDER BY 
            CASE WHEN :isAsc = 1 THEN created_at END ASC, 
            CASE WHEN :isAsc = 0 THEN created_at END DESC""",
    )
    abstract fun findAllBookmarksOrderByCreatedAtFlow(
        isAsc: Boolean,
        deleted: Boolean = false,
    ): Flow<List<Bookmark>>

    // Sorts the array by podcast name, episodes release date, and the bookmarks timestamp
    @Query(
        """SELECT 
              bookmarks.*, 
              podcasts.title as podcastTitle,
              CASE 
                WHEN podcast_episodes.title IS NULL 
                THEN user_episodes.title 
                ELSE podcast_episodes.title 
              END as episodeTitle,
              CASE 
                WHEN podcast_episodes.published_date IS NULL 
                THEN user_episodes.published_date 
                ELSE podcast_episodes.published_date 
              END as publishedDate
            FROM 
              bookmarks 
              LEFT JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid
              LEFT JOIN user_episodes ON bookmarks.episode_uuid = user_episodes.uuid 
              LEFT JOIN podcasts ON bookmarks.podcast_uuid = podcasts.uuid 
            WHERE 
              deleted = :deleted 
            ORDER BY
              CASE WHEN podcasts.title is NULL THEN 1 ELSE 0 end, /* prioritizes podcast episodes with title */
              CASE WHEN episodeTitle is NULL THEN 1 ELSE 0 END, /* prioritizes episodes with title */
              (CASE WHEN podcasts.title is NOT NULL THEN /* sort by podcast title if podcast title is not null */
                (CASE
                      WHEN UPPER(podcasts.title) LIKE 'THE %' THEN SUBSTR(UPPER(podcasts.title), 5)
                      WHEN UPPER(podcasts.title) LIKE 'A %' THEN SUBSTR(UPPER(podcasts.title), 3)
                      WHEN UPPER(podcasts.title) LIKE 'AN %' THEN SUBSTR(UPPER(podcasts.title), 4)
                      ELSE UPPER(podcasts.title)
                END) 
              ELSE /* sort by episode title if podcast title is null */
                (CASE
                      WHEN UPPER(episodeTitle) LIKE 'THE %' THEN SUBSTR(UPPER(episodeTitle), 5)
                      WHEN UPPER(episodeTitle) LIKE 'A %' THEN SUBSTR(UPPER(episodeTitle), 3)
                      WHEN UPPER(episodeTitle) LIKE 'AN %' THEN SUBSTR(UPPER(episodeTitle), 4)
                      ELSE UPPER(episodeTitle)
                  END) 
              END) ASC,
              publishedDate DESC,
              bookmarks.time ASC
        """,
    )
    abstract fun findAllBookmarksByOrderPodcastAndEpisodeFlow(
        deleted: Boolean = false,
    ): Flow<List<ProfileBookmark>>

    @Query(
        """SELECT bookmarks.*
            FROM bookmarks
            LEFT JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid 
            WHERE bookmarks.podcast_uuid = :podcastUuid 
            AND (UPPER(bookmarks.title) LIKE UPPER(:title) OR UPPER(podcast_episodes.title) LIKE UPPER(:title))
            AND deleted = :deleted""",
    )
    abstract suspend fun searchInPodcastByTitle(
        podcastUuid: String,
        title: String,
        deleted: Boolean = false,
    ): List<Bookmark>

    @Query(
        """
        SELECT 
          bookmarks.* 
        FROM 
          bookmarks 
          LEFT JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid 
        WHERE 
          (
            UPPER(bookmarks.title) LIKE UPPER(:title) 
            OR UPPER(podcast_episodes.title) LIKE UPPER(:title)
          ) 
          AND deleted = :deleted""",
    )
    abstract suspend fun searchByBookmarkOrEpisodeTitle(
        title: String,
        deleted: Boolean = false,
    ): List<Bookmark>

    @Query("UPDATE bookmarks SET deleted = :deleted, deleted_modified = :deletedModified, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateDeleted(uuid: String, deleted: Boolean, deletedModified: Long, syncStatus: SyncStatus)

    @Query("UPDATE bookmarks SET title = :title, title_modified = :titleModified, sync_status = :syncStatus WHERE uuid = :bookmarkUuid")
    abstract suspend fun updateTitle(bookmarkUuid: String, title: String, titleModified: Long, syncStatus: SyncStatus)

    @Query("SELECT * FROM bookmarks WHERE sync_status = :syncStatus")
    abstract fun findNotSynced(syncStatus: SyncStatus = SyncStatus.NOT_SYNCED): List<Bookmark>

    @Query(
        """SELECT bookmarks.*
            FROM bookmarks
            JOIN user_episodes ON bookmarks.episode_uuid = user_episodes.uuid 
            AND deleted = :deleted""",
    )
    abstract fun findUserEpisodesBookmarksFlow(deleted: Boolean = false): Flow<List<Bookmark>>
}
