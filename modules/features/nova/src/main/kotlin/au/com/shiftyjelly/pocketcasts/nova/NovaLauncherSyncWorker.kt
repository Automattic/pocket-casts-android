package au.com.shiftyjelly.pocketcasts.nova

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.nova.NovaLauncherManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.branch.engage.conduit.source.BranchDynamicData
import java.util.concurrent.TimeUnit

@HiltWorker
internal class NovaLauncherSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val manager: NovaLauncherManager,
) : CoroutineWorker(context, params) {
    private val catalogFactory = CatalogFactory(context)

    override suspend fun doWork(): Result {
        if (!FeatureFlag.isEnabled(Feature.NOVA_LAUNCHER)) {
            return Result.failure()
        }
        if (!applicationContext.isNovaLauncherInstalled) {
            return Result.failure()
        }

        val launcherBridge = BranchDynamicData.getOrInit(applicationContext)
        val subscribedPodcasts = catalogFactory.subscribedPodcasts(manager.getSubscribedPodcasts())

        return try {
            launcherBridge.submitUserData(listOf(subscribedPodcasts))
            Result.success()
        } catch (e: Throwable) {
            Result.failure()
        }
    }

    companion object {
        private const val ONE_OFF_WORK_NAME = "NovaLauncherOneOffSyncWorker"
        private const val PERIODIC_WORK_NAME = "NovaLauncherPeriodicSyncWorker"

        fun enqueueOneOffWork(context: Context) {
            val name = ONE_OFF_WORK_NAME
            val request = OneTimeWorkRequestBuilder<NovaLauncherSyncWorker>()
                .addTag(name)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)
        }

        fun cancelOneOffWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(ONE_OFF_WORK_NAME)
        }

        fun enqueuePeriodicWork(context: Context) {
            val name = PERIODIC_WORK_NAME
            val request = PeriodicWorkRequestBuilder<NovaLauncherSyncWorker>(6, TimeUnit.HOURS)
                .addTag(name)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PERIODIC_WORK_NAME)
        }
    }
}
