package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PendingEpisodeTask
import java.time.Instant

@Dao
abstract class PendingEpisodeTaskDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(tasks: List<PendingEpisodeTask>)

    @Query("SELECT episode_uuid FROM pending_episode_tasks WHERE task IS :task ORDER BY created_at ASC")
    abstract suspend fun findEpisodeUuids(task: PendingEpisodeTask.Type): List<String>

    @Query("DELETE FROM pending_episode_tasks WHERE created_at < :createdAt")
    abstract suspend fun deleteCreatedBefore(createdAt: Instant)

    suspend fun delete(task: PendingEpisodeTask.Type, episodeUuids: Collection<String>) {
        episodeUuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT - 1).forEach { chunk ->
            deleteChunk(task, chunk)
        }
    }

    @Query("DELETE FROM pending_episode_tasks WHERE task IS :task AND episode_uuid IN (:episodeUuids)")
    protected abstract suspend fun deleteChunk(task: PendingEpisodeTask.Type, episodeUuids: List<String>)
}
