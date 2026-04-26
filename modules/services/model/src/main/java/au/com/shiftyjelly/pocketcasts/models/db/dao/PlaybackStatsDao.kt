package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.PlaybackStatsEvent

@Dao
abstract class PlaybackStatsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertOrIgnore(events: Collection<PlaybackStatsEvent>)
}
