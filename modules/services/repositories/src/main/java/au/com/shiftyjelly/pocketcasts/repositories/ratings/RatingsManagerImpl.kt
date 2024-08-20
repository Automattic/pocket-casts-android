package au.com.shiftyjelly.pocketcasts.repositories.ratings

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastRatings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber

class RatingsManagerImpl @Inject constructor(
    private val cacheServerManager: PodcastCacheServerManager,
    private val syncManager: SyncManager,
    appDatabase: AppDatabase,
) : RatingsManager, CoroutineScope {
    private val podcastRatingsDao = appDatabase.podcastRatingsDao()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override fun podcastRatings(podcastUuid: String) =
        podcastRatingsDao.podcastRatings(podcastUuid)
            .map { it.firstOrNull() ?: noRatings(podcastUuid) }

    override suspend fun refreshPodcastRatings(podcastUuid: String, useCache: Boolean) {
        try {
            // The server asks for the ratings to be cached for a period of time. After a user rates ignore the cache to get the new rating.
            val ratings = cacheServerManager.getPodcastRatings(podcastUuid, useCache)
            podcastRatingsDao.insert(
                PodcastRatings(
                    podcastUuid = podcastUuid,
                    total = ratings.total,
                    average = ratings.average,
                ),
            )
        } catch (e: Exception) {
            val message = "Failed to refresh podcast ratings"
            // don't report missing rating or network errors to Sentry
            if (e is HttpException || e is IOException) {
                Timber.i(e, message)
            } else {
                LogBuffer.e(LogBuffer.TAG_CRASH, e, message)
            }
        }
    }

    override suspend fun submitPodcastRating(podcastUuid: String, rate: Int): PodcastRatingResult = try {
        syncManager.addPodcastRating(podcastUuid, rate)
        PodcastRatingResult.Success(rate.toDouble())
    } catch (e: Exception) {
        PodcastRatingResult.Error(e)
    }

    override suspend fun getPodcastRating(podcastUuid: String): PodcastRatingResult = try {
        val rate = syncManager.getPodcastRating(podcastUuid)
        PodcastRatingResult.Success(rate.podcastRating.toDouble())
    } catch (e: HttpException) {
        if (e.code() == 404) {
            PodcastRatingResult.NotFound
        } else {
            PodcastRatingResult.Error(e)
        }
    } catch (e: Exception) {
        PodcastRatingResult.Error(e)
    }

    companion object {
        private fun noRatings(podcastUuid: String) = PodcastRatings(
            podcastUuid = podcastUuid,
            average = 0.0,
            total = 0,
        )
    }
}

sealed class PodcastRatingResult {
    data class Success(val rating: Double) : PodcastRatingResult()
    data class Error(val exception: Exception) : PodcastRatingResult()
    data object NotFound : PodcastRatingResult()
}
