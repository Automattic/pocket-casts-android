package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.nova.NovaLauncherManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.branch.engage.conduit.source.BranchDynamicData
import io.branch.engage.conduit.source.CatalogSubmission
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@HiltWorker
internal class NovaLauncherSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val manager: NovaLauncherManager,
) : CoroutineWorker(context, params) {
    private val catalogFactory = CatalogFactory(context)
    private val name get() = requireNotNull(inputData.getString(WORKER_NAME_KEY)) { "Missing worker name" }

    override suspend fun doWork(): Result {
        logInfo("Staring Nova Launcher sync")
        if (!FeatureFlag.isEnabled(Feature.NOVA_LAUNCHER)) {
            logInfo("Nova launcher sync cancelled because the feature flag is disabled")
            return Result.failure()
        }
        if (!applicationContext.isNovaLauncherInstalled) {
            logError("Nova Launcher sync cancelled because it is not installed on the device")
            return Result.failure()
        }

        return coroutineScope {
            try {
                val subscribedPodcasts = async { catalogFactory.subscribedPodcasts(manager.getSubscribedPodcasts(limit = 200)) }
                val trendingPodcasts = async { catalogFactory.trendingPodcasts(manager.getTrendingPodcasts(limit = 25)) }

                val catalogSubmission = CatalogSubmission()
                    .setUserLibrary(listOf(subscribedPodcasts.await()))
                    .setTrending(listOf(trendingPodcasts.await()))

                val isDataSubmitted = BranchDynamicData.getOrInit(applicationContext).submit(catalogSubmission).isSuccess

                val results = listOf(
                    SubmissionResult("Subscribed podcasts", subscribedPodcasts.await().size),
                    SubmissionResult("Trending podcasts", trendingPodcasts.await().size),
                )

                logInfo("Nova Launcher sync complete. ${if (isDataSubmitted) "Success" else "Failure"}: $results")
                Result.success()
            } catch (e: Throwable) {
                logError("Nova Launcher sync failed", e)
                Result.failure()
            }
        }
    }

    private fun logInfo(message: String) = logInfo(id, name, message)

    private fun logError(message: String, throwable: Throwable? = null) = logError(id, name, message, throwable)

    private class SubmissionResult(
        val label: String,
        val itemCount: Int,
    ) {
        override fun toString() = "$label: $itemCount"
    }

    companion object {
        private const val ONE_OFF_WORK_NAME = "NovaLauncherOneOffSyncWorker"
        private const val PERIODIC_WORK_NAME = "NovaLauncherPeriodicSyncWorker"
        private const val WORKER_NAME_KEY = "worker_name"

        fun enqueueOneOffWork(context: Context) {
            val name = ONE_OFF_WORK_NAME
            val request = OneTimeWorkRequestBuilder<NovaLauncherSyncWorker>()
                .addTag(name)
                .setInputData(Data.Builder().putString(WORKER_NAME_KEY, name).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
            logInfo(request.id, name, "Enqueued Nova Launcher one-off sync")
        }

        fun cancelOneOffWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(ONE_OFF_WORK_NAME)
        }

        fun enqueuePeriodicWork(context: Context) {
            val name = PERIODIC_WORK_NAME
            val request = PeriodicWorkRequestBuilder<NovaLauncherSyncWorker>(6, TimeUnit.HOURS)
                .addTag(name)
                .setInputData(Data.Builder().putString(WORKER_NAME_KEY, name).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)
            logInfo(request.id, name, "Enqueued Nova Launcher periodic sync")
        }

        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_NAME)
        }

        private fun logInfo(id: UUID, name: String, message: String) = LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$name (Worker ID: $id) - $message")

        private fun logError(id: UUID, name: String, message: String, throwable: Throwable? = null) = if (throwable == null) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "$name (Worker ID: $id) - $message")
        } else {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "$name (Worker ID: $id) - $message")
        }
    }
}
