package au.com.shiftyjelly.pocketcasts.discover.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import timber.log.Timber
import androidx.work.ListenableWorker.Result as WorkerResult

@HiltWorker
class CuratedPodcastsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val crawler: CuratedPodcastsCrawler,
    private val manager: PodcastManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): WorkerResult {
        Timber.tag(TAG).d("Starting crawling curated podcasts")
        return crawler.crawl("android")
            .map { manager.replaceCuratedPodcasts(it) }
            .onSuccess { Timber.tag(TAG).d("Curated podcasts crawling finished") }
            .onFailure { Timber.tag(TAG).d(it, "Failed to crawl curated podcasts") }
            .fold(
                onSuccess = { WorkerResult.success() },
                onFailure = { WorkerResult.failure() },
            )
    }

    companion object {
        private const val TAG = "CuratedCrawler"
        private const val PERIODIC_WORK_NAME = "CuratedPodcastsSyncOneOffPeriodic"

        fun enqueuPeriodicWork(context: Context) {
            val tag = PERIODIC_WORK_NAME
            val request = PeriodicWorkRequestBuilder<CuratedPodcastsSyncWorker>(1, TimeUnit.DAYS)
                .addTag(tag)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
