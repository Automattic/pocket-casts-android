package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
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

    @Query("SELECT * FROM bookmarks WHERE podcast_uuid = :podcastUuid AND episode_uuid = :episodeUuid")
    abstract fun findByPodcastAndEpisodeFlow(podcastUuid: String, episodeUuid: String): Flow<List<Bookmark>>

    @Query("UPDATE bookmarks SET deleted = :deleted, sync_status = :syncStatus WHERE uuid = :uuid")
    abstract suspend fun updateDeleted(uuid: String, deleted: Boolean, syncStatus: Int)
}
