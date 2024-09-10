package au.com.shiftyjelly.pocketcasts.engage

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.engage.EngageSdkBridge.Companion.TAG
import au.com.shiftyjelly.pocketcasts.repositories.nova.ExternalDataManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.android.engage.service.AppEngageErrorCode
import com.google.android.engage.service.AppEngageException
import com.google.android.engage.service.AppEngagePublishClient
import com.google.android.gms.tasks.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import androidx.work.ListenableWorker.Result as WorkerResult

internal object EngageSdkWorkers {
    private const val ONE_OFF_RECOMMENDATIONS_WORK_NAME = "EngageSdkOneOffRecommendations"
    private const val ONE_OFF_CONTINUATION_WORK_NAME = "EngageSdkOneOffContinuation"
    private const val ONE_OFF_FEATURED_WORK_NAME = "EngageSdkOneOffFeatured"
    private const val PERIODIC_RECOMMENDATIONS_WORK_NAME = "EngageSdkPeriodicRecommendations"
    private const val PERIODIC_CONTINUATION_WORK_NAME = "EngageSdkPeriodicContinuation"
    private const val PERIODIC_FEATURED_WORK_NAME = "EngageSdkPeriodicFeatured"

    fun enqueueOneOffWork(context: Context) {
        enqueueOneOffRecommendationsWork(context)
        enqueueOneOffContinuationWork(context)
        enqueueOneOffFeaturedWork(context)
    }

    fun enqueuePeriodicWork(context: Context) {
        enqueuePeriodicWork<RecommendationsSyncWorker>(context, PERIODIC_RECOMMENDATIONS_WORK_NAME)
        enqueuePeriodicWork<ContinuationSyncWorker>(context, PERIODIC_CONTINUATION_WORK_NAME)
        enqueuePeriodicWork<FeaturedWorker>(context, PERIODIC_FEATURED_WORK_NAME)
    }

    fun enqueueOneOffRecommendationsWork(context: Context) {
        enqueueOneOffWork<RecommendationsSyncWorker>(context, ONE_OFF_RECOMMENDATIONS_WORK_NAME)
    }

    fun enqueueOneOffContinuationWork(context: Context) {
        enqueueOneOffWork<ContinuationSyncWorker>(context, ONE_OFF_CONTINUATION_WORK_NAME)
    }

    fun enqueueOneOffFeaturedWork(context: Context) {
        enqueueOneOffWork<FeaturedWorker>(context, ONE_OFF_FEATURED_WORK_NAME)
    }

    private inline fun <reified T : ClusterSyncWorker> enqueueOneOffWork(context: Context, tag: String) {
        val request = OneTimeWorkRequestBuilder<T>()
            .addTag(tag)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(tag, ExistingWorkPolicy.KEEP, request)
    }

    private inline fun <reified T : ClusterSyncWorker> enqueuePeriodicWork(context: Context, tag: String) {
        val request = PeriodicWorkRequestBuilder<T>(6, TimeUnit.HOURS)
            .addTag(tag)
            .setInitialDelay(5, TimeUnit.MINUTES) // Set initial delay so that the work doesn't start immmediately with the app launch
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

@HiltWorker
internal class RecommendationsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val externalDataManager: ExternalDataManager,
    private val syncManager: SyncManager,
) : ClusterSyncWorker(context, params, "Recommendations") {

    override suspend fun submitCluster(service: ClusterService): Task<Void> {
        return service.updateRecommendations(getEngageData(externalDataManager, syncManager))
    }
}

@HiltWorker
internal class ContinuationSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val externalDataManager: ExternalDataManager,
    private val syncManager: SyncManager,
) : ClusterSyncWorker(context, params, "Continuation") {

    override suspend fun submitCluster(service: ClusterService): Task<Void> {
        return service.updateContinuation(getEngageData(externalDataManager, syncManager))
    }
}

@HiltWorker
internal class FeaturedWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val externalDataManager: ExternalDataManager,
    private val syncManager: SyncManager,
) : ClusterSyncWorker(context, params, "Featured") {

    override suspend fun submitCluster(service: ClusterService): Task<Void> {
        return service.updateFeatured(getEngageData(externalDataManager, syncManager))
    }
}

private suspend fun getEngageData(dataManager: ExternalDataManager, syncManager: SyncManager): EngageData {
    // Do not use isLoggedIn() method https://github.com/Automattic/pocket-casts-android/issues/2409
    val isSignedIn = syncManager.isLoggedInObservable.value == true
    return dataManager.getEngageData(isSignedIn)
}

internal abstract class ClusterSyncWorker(
    context: Context,
    params: WorkerParameters,
    protected val type: String,
) : CoroutineWorker(context, params) {
    private val client = AppEngagePublishClient(context)
    private val service = ClusterService(context, client)

    abstract suspend fun submitCluster(service: ClusterService): Task<Void>

    final override suspend fun doWork(): WorkerResult {
        Timber.tag(TAG).d("Syncing '$type' cluster")
        return when {
            runAttemptCount > 5 -> {
                Timber.tag(TAG).d("Max retry attempt count exceeded. Current attempt: $runAttemptCount. Failing '$type' cluster sync.")
                Result.failure()
            }
            !client.isServiceAvailable.await() -> {
                Timber.tag(TAG).d("Engage SDK service is not avaialable. Failing '$type' cluster sync.")
                Result.failure()
            }
            else -> submitCluster(service).awaitSafe()
        }
    }

    private suspend fun <T> Task<T>.awaitSafe(): WorkerResult {
        return try {
            await()
            Timber.tag(TAG).d("Synced '$type' cluster successfully.")
            WorkerResult.success()
        } catch (e: Throwable) {
            Timber.tag(TAG).d(e, "Failed to sync '$type' cluster.")
            if (e.isRecoverable) {
                Timber.tag(TAG).d("Will retry to sync '$type' cluster.")
                WorkerResult.retry()
            } else {
                WorkerResult.failure()
            }
        }
    }

    private val Throwable.isRecoverable get() = when (this) {
        is AppEngageException -> when (errorCode) {
            AppEngageErrorCode.SERVICE_CALL_EXECUTION_FAILURE,
            AppEngageErrorCode.SERVICE_CALL_INTERNAL,
            AppEngageErrorCode.SERVICE_CALL_RESOURCE_EXHAUSTED,
            -> true
            else -> false
        }
        else -> false
    }
}

internal suspend fun ExternalDataManager.getEngageData(isSignedIn: Boolean) = coroutineScope {
    val recentlyPlayedPodcasts = async { getRecentlyPlayedPodcasts(limit = 50) }
    val newReleases = async { getNewEpisodes(limit = 50) }
    val discoverRecommendations = async { getCuratedPodcastGroups(limitPerGroup = 50) }
    val inProgressEpisodes = async { getInProgressEpisodes(limit = 10) }

    EngageData.create(
        recentlyPlayed = recentlyPlayedPodcasts.await(),
        newReleases = newReleases.await(),
        continuation = inProgressEpisodes.await(),
        curatedPodcasts = discoverRecommendations.await(),
        isSignedIn = isSignedIn,
    )
}
