package au.com.shiftyjelly.pocketcasts.models.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PodcastRatingsDao {
    @Query("SELECT * FROM podcast_ratings WHERE podcast_uuid = :podcastUuid")
    abstract fun podcastRatingsFlow(podcastUuid: String): Flow<List<PodcastRatings>>

    @Insert(onConflict = REPLACE)
    abstract suspend fun insert(ratings: PodcastRatings)

    @Query("SELECT * FROM user_podcast_ratings")
    abstract suspend fun getAllUserRatings(): List<UserPodcastRating>

    @Insert(onConflict = REPLACE)
    abstract suspend fun insertOrReplaceUserRatings(ratings: List<UserPodcastRating>)

    @Transaction
    open suspend fun updateUserRatings(ratings: List<UserPodcastRating>) {
        val localRatings = getAllUserRatings().associate { it.podcastUuid to it.modifiedAt }
        val filteredRatings = ratings.filter { rating ->
            val modifiedAt = localRatings[rating.podcastUuid] ?: return@filter true
            rating.modifiedAt >= modifiedAt
        }
        insertOrReplaceUserRatings(filteredRatings)
    }
}
