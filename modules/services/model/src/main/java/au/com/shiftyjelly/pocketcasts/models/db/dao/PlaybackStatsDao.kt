package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent

@Dao
abstract class PlaybackStatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOrIgnore(events: Collection<PlaybackStatsEvent>)

    @Transaction
    @Query("SELECT * FROM playback_stats_events")
    abstract suspend fun selectAll(): List<PlaybackStatsEvent>

    @Query("DELETE FROM playback_stats_events WHERE uuid IN (:uuids)")
    protected abstract suspend fun deleteAllUnsafe(uuids: Collection<String>)

    @Transaction
    open suspend fun deleteAll(uuids: Collection<String>) {
        uuids.chunked(AppDatabase.SQLITE_BIND_ARG_LIMIT).forEach { chunk ->
            deleteAllUnsafe(chunk)
        }
    }
}
