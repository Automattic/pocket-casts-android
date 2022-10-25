package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat

@Dao
abstract class BumpStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(bumpStat: AnonymousBumpStat)

    @Query("SELECT * FROM bump_stats")
    abstract suspend fun get(): List<AnonymousBumpStat>

    @Delete
    abstract suspend fun deleteAll(bumpStats: List<AnonymousBumpStat>)
}
