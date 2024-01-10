package au.com.shiftyjelly.pocketcasts.repositories.refresh

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltWorker
class RefreshPodcastsTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val params: WorkerParameters,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : Worker(context, params) {
    private var refreshRunnable: RefreshPodcastsThread? = null

    override fun doWork(): Result {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - Start")
        val refresh = RefreshPodcastsThread(
            context = this.applicationContext,
            applicationScope = applicationScope,
            runNow = false,
        )
        this.refreshRunnable = refresh
        val result = refresh.run()
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - Finished $result")
        return result
    }

    override fun onStopped() {
        super.onStopped()
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - onStopped")
        this.refreshRunnable?.cancelExecution()
    }

    companion object {
        private const val TAG_REFRESH_TASK = "au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask"
        private const val REFRESH_EVERY_HOURS = 1L

        fun scheduleOrCancel(context: Context, settings: Settings) {
            val workManager = WorkManager.getInstance(context)

            if (!settings.backgroundRefreshPodcasts.value) {
                workManager.cancelAllWorkByTag(TAG_REFRESH_TASK)
                return
            }

            val syncNetworkConstraint = settings.getWorkManagerNetworkTypeConstraint()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(syncNetworkConstraint)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<RefreshPodcastsTask>(REFRESH_EVERY_HOURS, TimeUnit.HOURS)
                .addTag(TAG_REFRESH_TASK)
                .setConstraints(constraints)
                .setInitialDelay(REFRESH_EVERY_HOURS, TimeUnit.HOURS)
                .build()

            workManager.enqueueUniquePeriodicWork(TAG_REFRESH_TASK, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Set up periodic refresh")
        }

        fun runNow(context: Context, applicationScope: CoroutineScope) {
            applicationScope.launch {
                runNowSync(context, applicationScope)
            }
        }

        private val refreshMutex = Mutex()
        private var refreshJob: Deferred<Result>? = null
        suspend fun runNowSync(context: Context, applicationScope: CoroutineScope) = withContext(Dispatchers.Default) {
            refreshMutex.withLock {
                if (refreshJob != null) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - runNow - Already running, joining.")
                    refreshJob?.await()
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - runNow - Already running, join complete.")
                    return@withContext
                }

                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - runNow - Start")
                val refreshThread = RefreshPodcastsThread(
                    context = context.applicationContext,
                    applicationScope = applicationScope,
                    runNow = true,
                )
                if (!refreshThread.isAllowedToRun(runNow = true)) {
                    return@withContext
                }
                refreshJob = async { refreshThread.run() }
            }

            try {
                val result = refreshJob?.await()
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "RefreshPodcastsTask - runNow - Finished $result")
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "RefreshPodcastsTask - runNow - Exception")
            } finally {
                refreshJob = null
            }
        }
    }
}
