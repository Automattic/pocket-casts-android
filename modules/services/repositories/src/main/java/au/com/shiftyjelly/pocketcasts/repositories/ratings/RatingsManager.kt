package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import io.reactivex.Flowable

interface RatingsManager {
    fun observePodcastRatings(podcastUuid: String): Flowable<PodcastRatings>
    fun findPodcastRatings(podcastUuid: String): PodcastRatings?
    suspend fun refreshPodcastRatings(podcastUuid: String)
}
