package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PodcastRatingsDao {
    @Query("SELECT * FROM podcast_ratings WHERE podcast_uuid = :podcastUuid")
    abstract fun podcastRatings(podcastUuid: String): Flow<List<PodcastRatings>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(ratings: PodcastRatings)

    @Delete
    abstract fun delete(ratings: PodcastRatings)
}
