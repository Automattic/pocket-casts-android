package au.com.shiftyjelly.pocketcasts.ui.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
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
    private val imageLoader: ImageLoader,
) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5
        private const val WORK_NAME = "PrefetchArtworkWorkerPeriodic"

        fun enqueuePeriodicWork(context: Context, settings: Settings) {
            val request = buildPeriodicWorkRequest(settings)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        internal fun buildPeriodicWorkRequest(settings: Settings): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<PrefetchArtworkWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(settings.getWorkManagerNetworkTypeConstraint())
                        .setRequiresBatteryNotLow(true)
                        .setRequiresStorageNotLow(true)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
        }
    }

    override suspend fun doWork(): Result {
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
                            // The original bytes are still written to disk; only the discarded decode is sampled.
                            .size(1, 1)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                        val result = imageLoader.execute(request)
                        if (result is ErrorResult) {
                            failed++
                            Timber.i("Could not prefetch artwork from $url. ${result.throwable.message}")
                        } else if (diskCache.openSnapshot(url)?.use { } == null) {
                            // Coil can return a successful network result even if it couldn't open a
                            // disk-cache editor. Only report success when the offline copy exists.
                            failed++
                            Timber.i("Could not store prefetched artwork from $url in the disk cache.")
                        } else {
                            fetched++
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to prefetch podcast artwork.")
            return retryOrResumeDailySchedule()
        }
        LogBuffer.i(
            LogBuffer.TAG_BACKGROUND_TASKS,
            "Prefetched $fetched missing podcast artwork images ($failed failed).",
        )
        // Retry with backoff so the cache heals shortly after connectivity or the CDN recovers,
        // instead of waiting for the next periodic run. Stop retrying after a few attempts so a
        // permanently missing image doesn't prevent the normal daily schedule from resuming.
        return if (failed > 0) retryOrResumeDailySchedule() else Result.success()
    }

    private fun retryOrResumeDailySchedule(): Result {
        return if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            Result.retry()
        } else {
            LogBuffer.w(
                LogBuffer.TAG_BACKGROUND_TASKS,
                "Artwork prefetch failed after $MAX_RETRY_ATTEMPTS retries. Resuming the daily schedule.",
            )
            Result.success()
        }
    }
}
