package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import kotlinx.coroutines.flow.Flow

interface RatingsManager {
    fun podcastRatings(podcastUuid: String): Flow<PodcastRatings>
    suspend fun refreshPodcastRatings(podcastUuid: String, useCache: Boolean)
    suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResult
    suspend fun submitPodcastRating(rating: UserPodcastRating): PodcastRatingResult
    suspend fun updateUserRatings(ratings: List<UserPodcastRating>)
}
