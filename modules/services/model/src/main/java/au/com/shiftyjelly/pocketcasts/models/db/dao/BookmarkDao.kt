package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid AND deleted = 0")
    abstract fun findByEpisodeFlow(podcastUuid: String, episodeUuid: String): Flow<List<Bookmark>>

    @Query("UPDATE bookmarks SET deleted = :deleted, deleted_modified = :deletedModified, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateDeleted(uuid: String, deleted: Boolean, deletedModified: Long, syncStatus: SyncStatus)

    @Query("SELECT * FROM bookmarks WHERE sync_status = :syncStatus")
    abstract fun findNotSynced(syncStatus: SyncStatus = SyncStatus.NOT_SYNCED): List<Bookmark>
}
