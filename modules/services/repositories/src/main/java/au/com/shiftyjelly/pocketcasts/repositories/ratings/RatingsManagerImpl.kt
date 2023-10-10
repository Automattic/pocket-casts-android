package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class RatingsManagerImpl @Inject constructor(
    private val cacheServerManager: PodcastCacheServerManager,
    appDatabase: AppDatabase,
) : RatingsManager, CoroutineScope {
    private val podcastRatingsDao = appDatabase.podcastRatingsDao()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun podcastRatings(podcastUuid: String) =
        podcastRatingsDao.podcastRatings(podcastUuid)
            .map { it.firstOrNull() ?: noRatings(podcastUuid) }

    override suspend fun refreshPodcastRatings(podcastUuid: String) {
        val ratings = cacheServerManager.getPodcastRatings(podcastUuid)
        podcastRatingsDao.insert(
            PodcastRatings(
                podcastUuid = podcastUuid,
                total = ratings.total,
                average = ratings.average
            )
        )
    }

    companion object {
        private fun noRatings(podcastUuid: String) = PodcastRatings(
            podcastUuid = podcastUuid,
            average = 0.0,
            total = 0
        )
    }
}
