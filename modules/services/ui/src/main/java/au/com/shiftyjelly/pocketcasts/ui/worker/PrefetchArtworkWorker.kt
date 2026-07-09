package au.com.shiftyjelly.pocketcasts.ui.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Keeps the artwork of subscribed podcasts in Coil's disk cache so covers still show when the
 * network or CDN is unavailable. Artwork is cached when subscribing to a podcast, but the disk
 * cache can be evicted (LRU pressure or the system trimming the app's cache directory), so this
 * worker periodically re-fetches any artwork that is no longer cached.
 */
@HiltWorker
class PrefetchArtworkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val podcastManager: PodcastManager,
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "PrefetchArtworkWorkerPeriodic"

        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<PrefetchArtworkWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        val imageLoader = applicationContext.imageLoader
        val diskCache = imageLoader.diskCache ?: return Result.success()
        val isWearOs = Util.isWearOs(applicationContext)
        var fetched = 0
        var failed = 0
        try {
            withContext(Dispatchers.IO) {
                for (podcast in podcastManager.findSubscribedNoOrder()) {
                    for (url in PodcastImage.getArtworkUrls(uuid = podcast.uuid, isWearOS = isWearOs)) {
                        // The disk cache key is the URL because the image loader doesn't register a
                        // custom Keyer or diskCacheKey. CoilManager.clearCache relies on this too.
                        val isCached = diskCache.openSnapshot(url)?.use { } != null
                        if (isCached) {
                            continue
                        }
                        val request = ImageRequest.Builder(applicationContext)
                            .data(url)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                        val result = imageLoader.execute(request)
                        if (result is ErrorResult) {
                            failed++
                            Timber.i("Could not prefetch artwork from $url. ${result.throwable.message}")
                        } else {
                            fetched++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to prefetch podcast artwork.")
            return Result.retry()
        }
        Timber.i("Prefetched $fetched missing podcast artwork images ($failed failed).")
        // Retry with backoff so the cache heals shortly after connectivity or the CDN recovers,
        // instead of waiting for the next periodic run.
        return if (failed > 0) Result.retry() else Result.success()
    }
}
