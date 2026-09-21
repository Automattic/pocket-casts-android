package au.com.shiftyjelly.pocketcasts.repositories.bumpstats

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComServiceManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class BumpStatsTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val wpComServiceManager: WpComServiceManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return run(appDatabase, wpComServiceManager)
    }

    companion object {
        private const val TAG = "BumpStatsTask"
        private val VALID_EVENT_NAME_REGEX = Regex("^[a-z_][a-z0-9_]*$")

        internal suspend fun run(
            appDatabase: AppDatabase,
            wpComServiceManager: WpComServiceManager,
        ): Result {
            val bumpStatsDao = appDatabase.bumpStatsDao()
            val bumpStats = bumpStatsDao.get()

            val (validBumpStats, invalidBumpStats) = bumpStats.partition { VALID_EVENT_NAME_REGEX.matches(it.name) }
            if (invalidBumpStats.isNotEmpty()) {
                LogBuffer.i(TAG, "removing ${invalidBumpStats.size} bump stats with invalid event names")
                bumpStatsDao.deleteAll(invalidBumpStats)
            }
            if (validBumpStats.isEmpty()) {
                Timber.i("$TAG, no bump stat events to send")
                return Result.success()
            }

            val response = wpComServiceManager.bumpStatAnonymously(validBumpStats)
            return if (response.isSuccessful && response.body() == "Accepted") {
                Timber.i("$TAG, successfully sent bump stats")
                bumpStatsDao.deleteAll(validBumpStats)
                Result.success()
            } else {
                LogBuffer.i(TAG, "Failed to send bump stats")
                Result.failure()
            }
        }

        fun scheduleToRun(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<BumpStatsTask>()
                .setConstraints(constraints)
                .build()

            WorkManager
                .getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }
}
