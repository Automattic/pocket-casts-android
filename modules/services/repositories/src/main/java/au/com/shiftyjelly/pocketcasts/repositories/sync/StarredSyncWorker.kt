package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale
import kotlinx.coroutines.coroutineScope

@HiltWorker
class StarredSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val starredSync: StarredSync,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORKER_TAG = "pocket_casts_starred_sync_worker_tag"

        fun enqueue(syncManager: SyncManager, context: Context): Operation? {
            if (!syncManager.isLoggedIn()) {
                return null
            }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - scheduled")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<StarredSyncWorker>()
                .addTag(WORKER_TAG)
                .setConstraints(constraints)
                .build()
            return WorkManager.getInstance(context).enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.KEEP, workRequest)
        }
    }

    override suspend fun doWork() = coroutineScope {
        val startTime = SystemClock.elapsedRealtime()
        try {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - started")
            performSync()
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - finished - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
            Result.success()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "StarredSyncWorker - failed - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
            Result.failure()
        }
    }

    private suspend fun performSync() {
        val serverEpisodes = syncManager.getStarredEpisodesOrThrow().episodesList
        starredSync.syncStarredEpisodes(serverEpisodes = serverEpisodes)
    }
}
