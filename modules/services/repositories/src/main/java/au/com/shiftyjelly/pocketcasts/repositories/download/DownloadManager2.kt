package au.com.shiftyjelly.pocketcasts.repositories.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.ConstraintListener
import androidx.work.impl.constraints.trackers.ConstraintTracker
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UpdateShowNotesTask
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Power
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class DownloadManager2 @Inject constructor(
    appDatabase: AppDatabase,
    settings: Settings,
    @ApplicationContext context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : DownloadQueue,
    DownloadStatusObserver {

    private val workManager = WorkManager.getInstance(context)
    private val prerequisitesProvider = DownloadPrerequisitesProvider(workManager, context)
    private val queueController = DownloadQueueController(appDatabase, settings, workManager, context)
    private val statusController = DownloadStatusController(appDatabase, context)

    private val isMonitoring = AtomicBoolean()

    override fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType) {
        scope.launch { queueController.addToQueue(episodeUuids, downloadType) }
    }

    override fun cancelAll(episodeUuids: Collection<String>) {
        queueController.removeFromQueue(episodeUuids)
    }

    override fun cancelAll(podcastUuid: String) {
        queueController.removeFromQueue(podcastUuid)
    }

    @OptIn(FlowPreview::class)
    override fun monitorDownloadStatus() {
        if (isMonitoring.getAndSet(true)) {
            return
        }
        scope.launch {
            val constraintsFlow = prerequisitesProvider.getConstraintsFlow()
            val workInfosFlow = workManager.getWorkInfosByTagFlow(DownloadEpisodeWorker.WORKER_TAG)
                .conflate()
                .map { infos -> infos.mapNotNull(DownloadEpisodeWorker::mapToDownloadWorkInfo) }

            combine(workInfosFlow, constraintsFlow, ::Pair).collect { (infos, constraints) ->
                statusController.updateStatuses(infos, constraints)
                queueController.cancelDownloadsExceedingMaxAttempts(infos)
            }
        }
    }
}

private class DownloadQueueController(
    appDatabase: AppDatabase,
    private val settings: Settings,
    private val workManager: WorkManager,
    private val context: Context,
) {
    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

    suspend fun addToQueue(episodeUuids: Collection<String>, downloadType: DownloadType) {
        var episodes = episodeDao.findByUuids(episodeUuids) + userEpisodeDao.findEpisodesByUuids(episodeUuids)
        episodes = episodes.filter { episode ->
            when (episode) {
                is PodcastEpisode -> !episode.isDownloaded
                is UserEpisode -> !episode.isDownloaded && episode.isUploaded
            }
        }
        if (episodes.isEmpty()) {
            return
        }

        val pendingWorks = workManager.getWorkInfosByTagFlow(DownloadEpisodeWorker.WORKER_TAG)
            .firstOrNull()
            ?.mapNotNull(DownloadEpisodeWorker::mapToDownloadWorkInfo)
            ?.filterIsInstance<DownloadWorkInfo.Pending>()
            .orEmpty()
            .associateBy(DownloadWorkInfo::episodeUuid)

        episodes.forEach { episode ->
            val pendingWork = pendingWorks[episode.uuid]
            val args = episode.toDownloadArgs(downloadType)
            val (request, constraints) = DownloadEpisodeWorker.createWorkRequest(args)

            val operation = workManager.enqueueUniqueWork(
                uniqueWorkName = DownloadEpisodeWorker.episodeTag(episode.uuid),
                existingWorkPolicy = when {
                    pendingWork == null -> ExistingWorkPolicy.KEEP
                    !args.waitForWifi && pendingWork.isWifiRequired -> ExistingWorkPolicy.REPLACE
                    !args.waitForPower && pendingWork.isPowerRequired -> ExistingWorkPolicy.REPLACE
                    else -> ExistingWorkPolicy.KEEP
                },
                request = request,
            )
            val operationState = operation.state.asFlow().firstOrNull()
            if (operationState != null && operationState !is Operation.State.FAILURE) {
                enqueueShowNotesUpdate(episode, constraints)
            }
        }
    }

    fun removeFromQueue(episodeUuids: Collection<String>) {
        episodeUuids.forEach { episodeUuid ->
            workManager.cancelAllWorkByTag(DownloadEpisodeWorker.episodeTag(episodeUuid))
            workManager.cancelAllWorkByTag(UpdateShowNotesTask.episodeTag(episodeUuid))
        }
    }

    fun removeFromQueue(podcastUuid: String) {
        workManager.cancelAllWorkByTag(DownloadEpisodeWorker.podcastTag(podcastUuid))
        workManager.cancelAllWorkByTag(UpdateShowNotesTask.podcastTag(podcastUuid))
    }

    fun cancelDownloadsExceedingMaxAttempts(workInfos: Collection<DownloadWorkInfo>) {
        // We normally check the retry/attempt count inside the download worker.
        // However, the worker may never actually reach doWork().
        //
        // If constraints (e.g. network) keep changing while the episode is queued,
        // WorkManager can cancel and reschedule the work. Each reschedule increases
        // the run attempt count.
        //
        // In flaky network conditions this can result in the work being repeatedly
        // rescheduled without ever executing, leaving the episode stuck in the queue.
        //
        // With exponential backoff enabled, the delay grows after each attempt,
        // making the problem progressively worse.
        val episodesToCancel = workInfos.mapNotNull { info ->
            (info as? DownloadWorkInfo.Pending)
                ?.takeIf { it.runAttemptCount >= MAX_DOWNLOAD_ATTEMPT_COUNT }
                ?.episodeUuid
        }
        removeFromQueue(episodesToCancel)
    }

    private fun BaseEpisode.toDownloadArgs(downloadType: DownloadType) = when (this) {
        is PodcastEpisode -> DownloadEpisodeWorker.Args(
            episodeUuid = uuid,
            podcastUuid = podcastUuid,
            waitForWifi = when (downloadType) {
                DownloadType.UserTriggered -> false
                DownloadType.Automatic -> settings.autoDownloadUnmeteredOnly.value
            },
            waitForPower = when (downloadType) {
                DownloadType.UserTriggered -> false
                DownloadType.Automatic -> settings.autoDownloadOnlyWhenCharging.value
            },
        )

        is UserEpisode -> DownloadEpisodeWorker.Args(
            episodeUuid = uuid,
            podcastUuid = podcastOrSubstituteUuid,
            waitForWifi = when (downloadType) {
                DownloadType.UserTriggered -> false
                DownloadType.Automatic -> settings.cloudDownloadOnlyOnWifi.value
            },
            waitForPower = false,
        )
    }

    private fun enqueueShowNotesUpdate(episode: BaseEpisode, constraints: Constraints) {
        when (episode) {
            is PodcastEpisode -> UpdateShowNotesTask.enqueue(episode, constraints, context)
            is UserEpisode -> Unit
        }
    }
}

private class DownloadStatusController(
    private val appDatabase: AppDatabase,
    private val context: Context,
) {
    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

    private val pendingStatuses = EpisodeDownloadStatus.entries.filter { it.isPending }

    suspend fun updateStatuses(infos: List<DownloadWorkInfo>, constraints: DownloadPrerequisites) {
        val tooManyAttemptsError = context.getString(LR.string.error_download_too_many_attempts)
        val defaultErrorMessage = context.getString(LR.string.error_download_generic_failure, "").trim()
        val statusUpdates = infos.associate { info ->
            val update = info.toStatusUpdate(
                constraints = constraints,
                tooManyAttemptsErrorMessage = tooManyAttemptsError,
                defaultErrorMessage = defaultErrorMessage,
            )
            info.episodeUuid to update
        }

        appDatabase.withTransaction {
            val staleEpisodeUuids = episodeDao.getEpisodeUuidsWithDownloadStatus(pendingStatuses) - statusUpdates.keys
            episodeDao.updateDownloadStatuses(staleEpisodeUuids.associateWith { DownloadStatusUpdate.Idle })
            episodeDao.updateDownloadStatuses(statusUpdates)

            val staleUserEpisodeUuids = userEpisodeDao.getEpisodeUuidsWithDownloadStatus(pendingStatuses) - statusUpdates.keys
            userEpisodeDao.updateDownloadStatuses(staleUserEpisodeUuids.associateWith { DownloadStatusUpdate.Idle })
            userEpisodeDao.updateDownloadStatuses(statusUpdates)
        }
    }

    private fun DownloadWorkInfo.toStatusUpdate(
        constraints: DownloadPrerequisites,
        tooManyAttemptsErrorMessage: String,
        defaultErrorMessage: String,
    ): DownloadStatusUpdate {
        return when (this) {
            is DownloadWorkInfo.Cancelled -> {
                if (runAttemptCount >= MAX_DOWNLOAD_ATTEMPT_COUNT) {
                    DownloadStatusUpdate.Failure(tooManyAttemptsErrorMessage)
                } else {
                    DownloadStatusUpdate.Idle
                }
            }

            is DownloadWorkInfo.Pending -> {
                when {
                    !constraints.isNetworkAvailable -> DownloadStatusUpdate.WaitingForWifi
                    isWifiRequired && !constraints.isUnmeteredAvailable -> DownloadStatusUpdate.WaitingForWifi
                    isPowerRequired && !constraints.isPowerAvailable -> DownloadStatusUpdate.WaitingForPower
                    isStorageRequired && !constraints.isStorageAvailable -> DownloadStatusUpdate.WaitingForStorage
                    else -> DownloadStatusUpdate.Enqueued
                }
            }

            is DownloadWorkInfo.InProgress -> {
                DownloadStatusUpdate.InProgress
            }

            is DownloadWorkInfo.Success -> {
                DownloadStatusUpdate.Success(downloadFile)
            }

            is DownloadWorkInfo.Failure -> {
                DownloadStatusUpdate.Failure(errorMessage ?: defaultErrorMessage)
            }
        }
    }
}

private data class DownloadPrerequisites(
    val isPowerAvailable: Boolean,
    val isNetworkAvailable: Boolean,
    val isUnmeteredAvailable: Boolean,
    val isStorageAvailable: Boolean,
)

@SuppressLint("RestrictedApi")
private class DownloadPrerequisitesProvider(
    private val workManager: WorkManager,
    private val context: Context,
) {
    fun getConstraintsFlow(): Flow<DownloadPrerequisites> {
        // We intentionally try to use WorkManager's internal constraint trackers when available,
        // because they encapsulate platform/version/device-specific edge cases (API-level quirks,
        // OEM behavior, etc.). There is currently no public WorkManager API that exposes whether
        // constraints are currently met at runtime.
        //
        // This is a temporary workaround until WorkManager exposes a supported public API.
        // See: https://issuetracker.google.com/issues/483092904
        val impl = workManager as? WorkManagerImpl
        val constraintsFlow = if (impl != null) {
            getWorkManagerConstraintsFlow(impl)
        } else {
            getInAppConstraintsFlow()
        }
        return constraintsFlow.distinctUntilChanged()
    }

    private fun getWorkManagerConstraintsFlow(workManager: WorkManagerImpl): Flow<DownloadPrerequisites> {
        fun <T> ConstraintTracker<T>.asFlow() = callbackFlow {
            val listener = object : ConstraintListener<T> {
                override fun onConstraintChanged(newValue: T) {
                    trySend(newValue)
                }
            }
            addListener(listener)
            trySend(state)
            awaitClose { removeListener(listener) }
        }

        val trackers = workManager.trackers
        val powerFlow = trackers.batteryChargingTracker.asFlow()
        val networkFlow = trackers.networkStateTracker.asFlow().map { it.isConnected to it.isMetered }
        val storageTracker = trackers.storageNotLowTracker.asFlow()
        return combine(powerFlow, networkFlow, storageTracker) { isCharging, (isConnected, isMetered), isStorageNotLow ->
            DownloadPrerequisites(
                isPowerAvailable = isCharging,
                isNetworkAvailable = isConnected,
                isUnmeteredAvailable = !isMetered,
                isStorageAvailable = isStorageNotLow,
            )
        }
    }

    private fun getInAppConstraintsFlow(): Flow<DownloadPrerequisites> {
        return flow {
            while (true) {
                emit(
                    DownloadPrerequisites(
                        isPowerAvailable = Power.isConnected(context),
                        isNetworkAvailable = Network.isConnected(context),
                        isUnmeteredAvailable = Network.isUnmeteredConnection(context),
                        isStorageAvailable = true, // We don't have a call to detect low storage
                    ),
                )
                delay(10.seconds)
            }
        }
    }
}
