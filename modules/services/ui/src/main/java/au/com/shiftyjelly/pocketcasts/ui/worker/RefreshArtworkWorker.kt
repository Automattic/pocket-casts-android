package au.com.shiftyjelly.pocketcasts.ui.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class RefreshArtworkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    val settings: Settings,
    val podcastManager: PodcastManager,
    val colorManager: ColorManager,
    val coilManager: CoilManager,
) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 5

        fun start(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<RefreshArtworkWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        var successful = 0
        var failed = 0
        withContext(Dispatchers.IO) {
            // Do not clear entries restored before a prior attempt returned Result.retry().
            if (runAttemptCount == 0) {
                coilManager.clearAll()
            }
            val podcasts = podcastManager.findSubscribedNoOrder()
            val isWearOs = Util.isWearOs(applicationContext)
            for (podcast in podcasts) {
                for (url in PodcastImage.getArtworkUrls(uuid = podcast.uuid, isWearOS = isWearOs)) {
                    try {
                        val request = ImageRequest.Builder(applicationContext)
                            .data(url)
                            // The original bytes are still cached; only the discarded decode is sampled.
                            .size(1, 1)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .build()
                        val result = coilManager.imageLoader.execute(request)
                        if (result is ErrorResult) {
                            failed++
                            Timber.i("Could not refresh podcast artwork from $url. ${result.throwable.message}")
                        } else {
                            successful++
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                        Timber.e(e, "Could not refresh podcast artwork from $url")
                    }
                }
            }
            colorManager.updateColors(podcasts)
        }

        val summary =
            "Artwork refresh attempt ${runAttemptCount + 1}: $successful image requests succeeded ($failed failed)."
        return when {
            failed == 0 -> {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, summary)
                Result.success()
            }

            runAttemptCount < MAX_RETRY_ATTEMPTS -> {
                LogBuffer.w(LogBuffer.TAG_BACKGROUND_TASKS, "$summary Retrying.")
                Result.retry()
            }

            else -> {
                LogBuffer.w(
                    LogBuffer.TAG_BACKGROUND_TASKS,
                    "$summary Giving up after $MAX_RETRY_ATTEMPTS retries.",
                )
                Result.failure()
            }
        }
    }
}
