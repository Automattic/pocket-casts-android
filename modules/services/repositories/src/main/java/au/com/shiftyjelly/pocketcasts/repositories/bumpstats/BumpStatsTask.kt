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
import au.com.shiftyjelly.pocketcasts.models.db.dao.BumpStatsDao
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComServiceManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import kotlin.time.Duration.Companion.days
import timber.log.Timber

@HiltWorker
class BumpStatsTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val wpComServiceManager: WpComServiceManager,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return run(appDatabase, wpComServiceManager, clock)
    }

    companion object {
        private const val TAG = "BumpStatsTask"
        private val VALID_EVENT_NAME_REGEX = Regex("^[a-z_][a-z0-9_]*$")

        private const val BATCH_SIZE = 500
        private const val MAX_STORED_BUMP_STATS = 5_000
        private val MAX_BUMP_STAT_AGE = 30.days

        internal suspend fun run(
            appDatabase: AppDatabase,
            wpComServiceManager: WpComServiceManager,
            clock: Clock,
        ): Result {
            val bumpStatsDao = appDatabase.bumpStatsDao()
            pruneStoredBumpStats(bumpStatsDao, clock)

            repeat(MAX_STORED_BUMP_STATS / BATCH_SIZE) {
                val bumpStats = bumpStatsDao.getOldest(BATCH_SIZE)
                if (bumpStats.isEmpty()) {
                    return Result.success()
                }

                val (validBumpStats, invalidBumpStats) = bumpStats.partition { VALID_EVENT_NAME_REGEX.matches(it.name) }
                if (invalidBumpStats.isNotEmpty()) {
                    LogBuffer.i(TAG, "removing ${invalidBumpStats.size} bump stats with invalid event names")
                    bumpStatsDao.deleteAll(invalidBumpStats)
                }
                if (validBumpStats.isEmpty()) {
                    return@repeat
                }

                val response = wpComServiceManager.bumpStatAnonymously(validBumpStats)
                if (!response.isSuccessful || response.body() != "Accepted") {
                    LogBuffer.i(TAG, "Failed to send bump stats")
                    return Result.failure()
                }
                Timber.i("$TAG, successfully sent ${validBumpStats.size} bump stats")
                bumpStatsDao.deleteAll(validBumpStats)
            }
            return Result.success()
        }

        private suspend fun pruneStoredBumpStats(bumpStatsDao: BumpStatsDao, clock: Clock) {
            val cutoff = clock.millis() - MAX_BUMP_STAT_AGE.inWholeMilliseconds
            val expiredCount = bumpStatsDao.deleteOlderThan(cutoff)
            val overflowCount = bumpStatsDao.deleteAllExceptNewest(MAX_STORED_BUMP_STATS)
            if (expiredCount > 0 || overflowCount > 0) {
                LogBuffer.i(TAG, "pruned $expiredCount expired and $overflowCount overflow bump stats")
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
