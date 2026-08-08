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
        fun start(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<RefreshArtworkWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            coilManager.clearAll()
            val podcasts = podcastManager.findSubscribedNoOrder()
            colorManager.updateColors(podcasts)
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
                            Timber.i("Could not refresh podcast artwork from $url. ${result.throwable.message}")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.e(e, "Could not refresh podcast artwork from $url")
                    }
                }
            }
        }

        Timber.i("Successfully refreshed the podcasts artwork.")

        return Result.success()
    }
}
