package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import kotlinx.coroutines.flow.Flow

interface RatingsManager {
    fun podcastRatings(podcastUuid: String): Flow<PodcastRatings>
    suspend fun refreshPodcastRatings(podcastUuid: String)
    suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResult
    suspend fun submitPodcastRating(podcastUuid: String, rate: Int): PodcastRatingResult
}
