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
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComServerManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class BumpStatsTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val wpComServerManager: WpComServerManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val bumpStatsDao = appDatabase.bumpStatsDao()
        val bumpStats = bumpStatsDao.get()

        return if (bumpStats.isNotEmpty()) {
            val response = wpComServerManager.bumpStatAnonymously(bumpStats)
            if (response.isSuccessful && response.body() == "Accepted") {
                Timber.i("$TAG, successfully sent bump stats")

                // Remove the bump stat events that were successfully sent from the db
                bumpStatsDao.deleteAll(bumpStats)

                Result.success()
            } else {
                LogBuffer.i(TAG, "Failed to send bump stats")
                Result.failure()
            }
        } else {
            Timber.i("$TAG, no bump stat events to send")
            Result.success()
        }
    }

    companion object {
        private const val TAG = "BumpStatsTask"

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
