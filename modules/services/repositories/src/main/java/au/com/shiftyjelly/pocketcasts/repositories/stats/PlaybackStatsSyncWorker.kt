package au.com.shiftyjelly.pocketcasts.repositories.stats

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaybackStatsDao
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.ListeningTimeEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltWorker
class PlaybackStatsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncManager: SyncManager,
    private val playbackStatsDao: PlaybackStatsDao,
    private val eventHorizon: EventHorizon,
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        if (!syncManager.isLoggedIn()) {
            return Result.success()
        }

        syncMutex.withLock {
            val events = playbackStatsDao.selectAll()
            val eventIds = events.map { dbEvent ->
                val event = ListeningTimeEvent(
                    startedAtMs = dbEvent.startedAtMs,
                    durationMs = dbEvent.durationMs,
                    eventUuid = dbEvent.uuid,
                    podcastUuid = dbEvent.podcastUuid,
                    episodeUuid = dbEvent.episodeUuid,
                    deviceType = Util.getAppPlatform(applicationContext).analyticsValue,
                )
                eventHorizon.track(event)
                dbEvent.uuid
            }
            playbackStatsDao.deleteAll(eventIds)
        }

        return Result.success()
    }

    companion object {
        private val syncMutex = Mutex()

        private const val WORKER_TAG = "playback_stats_sync_worker"

        fun scheduleOneTimeWork(context: Context): Operation {
            val request = OneTimeWorkRequestBuilder<PlaybackStatsSyncWorker>()
                .addTag(WORKER_TAG)
                .build()
            return WorkManager
                .getInstance(context)
                .enqueueUniqueWork("$WORKER_TAG-one-time", ExistingWorkPolicy.KEEP, request)
        }

        fun schedulePeriodicWork(context: Context): Operation {
            val request =
                PeriodicWorkRequestBuilder<PlaybackStatsSyncWorker>(6, TimeUnit.HOURS)
                    .addTag(WORKER_TAG)
                    .build()
            return WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork("$WORKER_TAG-periodic", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
