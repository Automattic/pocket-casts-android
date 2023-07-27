package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.db.helper.PodcastBookmark
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

    @Query("SELECT * FROM bookmarks WHERE uuid = :uuid")
    abstract suspend fun findByUuid(uuid: String): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND time = :timeSecs LIMIT 1")
    abstract suspend fun findByEpisodeTime(podcastUuid: String, episodeUuid: String, timeSecs: Int): Bookmark?

    @Query(
        "SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND deleted = :deleted " +
            "ORDER BY " +
            "CASE WHEN :isAsc = 1 THEN created_at END ASC, " +
            "CASE WHEN :isAsc = 0 THEN created_at END DESC"
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
        """SELECT bookmarks.*, podcast_episodes.title as episodeTitle
            FROM bookmarks
            JOIN podcast_episodes ON bookmarks.episode_uuid = podcast_episodes.uuid 
            WHERE podcast_uuid = :podcastUuid AND deleted = :deleted"""
    )
    abstract fun findByPodcastFlow(
        podcastUuid: String,
        deleted: Boolean = false,
    ): Flow<List<PodcastBookmark>>

    @Query("UPDATE bookmarks SET deleted = :deleted, deleted_modified = :deletedModified, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateDeleted(uuid: String, deleted: Boolean, deletedModified: Long, syncStatus: SyncStatus)

    @Query("UPDATE bookmarks SET title = :title, title_modified = :titleModified, sync_status = :syncStatus WHERE uuid = :bookmarkUuid")
    abstract suspend fun updateTitle(bookmarkUuid: String, title: String, titleModified: Long, syncStatus: SyncStatus)

    @Query("SELECT * FROM bookmarks WHERE sync_status = :syncStatus")
    abstract fun findNotSynced(syncStatus: SyncStatus = SyncStatus.NOT_SYNCED): List<Bookmark>
}
