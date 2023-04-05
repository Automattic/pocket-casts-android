package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import kotlinx.coroutines.flow.Flow

interface RatingsManager {
    fun podcastRatings(podcastUuid: String): Flow<PodcastRatings>
    fun findPodcastRatings(podcastUuid: String): PodcastRatings?
    suspend fun refreshPodcastRatings(podcastUuid: String)
}
