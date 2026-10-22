package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat

@Dao
abstract class BumpStatsDao {

    @Insert
    abstract suspend fun insert(bumpStat: AnonymousBumpStat)

    @Query("SELECT * FROM bump_stats ORDER BY event_time ASC, id ASC LIMIT :limit")
    abstract suspend fun getOldest(limit: Int): List<AnonymousBumpStat>

    @Delete
    abstract suspend fun deleteAll(bumpStats: List<AnonymousBumpStat>)

    @Query("DELETE FROM bump_stats WHERE event_time < :timestamp")
    abstract suspend fun deleteOlderThan(timestamp: Long): Int

    @Query("DELETE FROM bump_stats WHERE id NOT IN (SELECT id FROM bump_stats ORDER BY event_time DESC, id DESC LIMIT :count)")
    abstract suspend fun deleteAllExceptNewest(count: Int): Int
}
