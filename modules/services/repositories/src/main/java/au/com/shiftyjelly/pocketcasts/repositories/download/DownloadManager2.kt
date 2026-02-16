package au.com.shiftyjelly.pocketcasts.repositories.download

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.asFlow
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
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
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Power
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
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
    private val queueController = DownloadQueueController(appDatabase, settings, workManager, context, scope)
    private val statusController = DownloadStatusController(appDatabase, context)

    private val isMonitoring = AtomicBoolean()

    override fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType) {
        scope.launch { queueController.addToQueue(episodeUuids, downloadType) }
    }

    override fun cancelAll(episodeUuids: Collection<String>, disableAutoDownload: Boolean) {
        scope.launch { queueController.removeFromQueue(episodeUuids, disableAutoDownload) }
    }

    override fun cancelAll(podcastUuid: String, disableAutoDownload: Boolean) {
        scope.launch { queueController.removeFromQueue(podcastUuid, disableAutoDownload) }
    }

    @OptIn(FlowPreview::class)
    override fun monitorDownloadStatus() {
        if (isMonitoring.getAndSet(true)) {
            return
        }

        scope.launch {
            queueController.clearStaleTasks()

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
    private val appDatabase: AppDatabase,
    private val settings: Settings,
    private val workManager: WorkManager,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()
    private val fileStorage = FileStorage(settings, context)

    private val pendingStatuses = EpisodeDownloadStatus.entries.filter { it.isPending }

    suspend fun clearStaleTasks() {
        val pendingEpisodes = workManager.getDownloadWorkInfos<DownloadWorkInfo.Pending>().keys
        appDatabase.withTransaction {
            val staleEpisodes = episodeDao.getEpisodeUuidsWithDownloadStatus(pendingStatuses) - pendingEpisodes
            episodeDao.updateDownloadStatuses(staleEpisodes.associateWith { DownloadStatusUpdate.Idle })

            val staleUserEpisodes = userEpisodeDao.getEpisodeUuidsWithDownloadStatus(pendingStatuses) - pendingEpisodes
            userEpisodeDao.updateDownloadStatuses(staleUserEpisodes.associateWith { DownloadStatusUpdate.Idle })
        }
    }

    suspend fun addToQueue(episodeUuids: Collection<String>, downloadType: DownloadType) {
        val podcastEpisodes = episodeDao.findByUuids(episodeUuids)
        val userEpisodes = userEpisodeDao.findEpisodesByUuids(episodeUuids)
        val episodes = (podcastEpisodes + userEpisodes).filter { episode -> canDownload(episode, downloadType) }
        if (episodes.isEmpty()) {
            return
        }

        val pendingWorks = workManager.getDownloadWorkInfos<DownloadWorkInfo.Pending>()
        val downloadCommands = episodes.map { episode -> episode.toDownloadCommand(downloadType, pendingWorks) }

        val associatedWorkers = downloadCommands.associate { command -> command.episodeUuid to command.request.id }
        applyQueueTransition(QueueTransition.Enqueue(associatedWorkers))

        downloadCommands.forEach { command ->
            val operation = command.enqueue(workManager)
            val operationState = operation.state.asFlow().firstOrNull()
            if (operationState != null && operationState !is Operation.State.FAILURE) {
                enqueueShowNotesUpdate(command.episode, command.constraints)
            }
        }
    }

    suspend fun removeFromQueue(episodeUuids: Collection<String>, disableAutoDownload: Boolean) {
        applyQueueTransition(QueueTransition.Remove(episodeUuids, disableAutoDownload))

        episodeUuids.forEach { episodeUuid ->
            workManager.cancelAllWorkByTag(DownloadEpisodeWorker.episodeTag(episodeUuid))
            workManager.cancelAllWorkByTag(UpdateShowNotesTask.episodeTag(episodeUuid))
        }
    }

    suspend fun removeFromQueue(podcastUuid: String, disableAutoDownload: Boolean) {
        val episodeUuids = episodeDao.findByPodcastUuid(podcastUuid)
        applyQueueTransition(QueueTransition.Remove(episodeUuids, disableAutoDownload))

        workManager.cancelAllWorkByTag(DownloadEpisodeWorker.podcastTag(podcastUuid))
        workManager.cancelAllWorkByTag(UpdateShowNotesTask.podcastTag(podcastUuid))
    }

    suspend fun cancelDownloadsExceedingMaxAttempts(workInfos: Collection<DownloadWorkInfo>) {
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
        removeFromQueue(episodesToCancel, disableAutoDownload = false)
    }

    private suspend fun applyQueueTransition(transition: QueueTransition) {
        appDatabase.withTransaction {
            when (transition) {
                is QueueTransition.Enqueue -> {
                    episodeDao.setReadyForDownload(transition.episodeToWorkerUuids)
                    userEpisodeDao.setReadyForDownload(transition.episodeToWorkerUuids)
                }

                is QueueTransition.Remove -> {
                    episodeDao.setDownloadCancelled(transition.episodeUuids, transition.disableAutoDownload)
                    userEpisodeDao.setDownloadCancelled(transition.episodeUuids, transition.disableAutoDownload)
                }
            }
        }
        when (transition) {
            is QueueTransition.Enqueue -> {}

            is QueueTransition.Remove -> {
                deleteAllDownloadFiles(transition.episodeUuids)
            }
        }
    }

    private fun deleteAllDownloadFiles(episodeUuids: Collection<String>) {
        coroutineScope.launch {
            val podcastEpisodes = episodeDao.findByUuids(episodeUuids)
            val userEpisodes = userEpisodeDao.findEpisodesByUuids(episodeUuids)
            val episodes = podcastEpisodes + userEpisodes

            for (episode in episodes) {
                launch {
                    runCatching {
                        DownloadHelper.pathForEpisode(episode, fileStorage)
                            ?.let(::File)
                            ?.delete()
                    }
                }
            }
        }
    }

    private fun canDownload(episode: BaseEpisode, downloadType: DownloadType): Boolean {
        val isFileAvailable = when (episode) {
            is PodcastEpisode -> true
            is UserEpisode -> episode.isUploaded
        }
        val isDownloadTypeAllowed = when (downloadType) {
            DownloadType.UserTriggered -> true
            DownloadType.Automatic -> !episode.isExemptFromAutoDownload
        }
        return !episode.isDownloaded && isFileAvailable && isDownloadTypeAllowed
    }

    private fun BaseEpisode.toDownloadCommand(
        downloadType: DownloadType,
        pendingWorks: Map<String, DownloadWorkInfo.Pending>,
    ): DownloadCommand {
        val pendingWork = pendingWorks[uuid]
        val args = toDownloadArgs(downloadType)
        val (request, constraints) = DownloadEpisodeWorker.createWorkRequest(args)
        val policy = when {
            pendingWork == null -> ExistingWorkPolicy.KEEP
            !args.waitForWifi && pendingWork.isWifiRequired -> ExistingWorkPolicy.REPLACE
            !args.waitForPower && pendingWork.isPowerRequired -> ExistingWorkPolicy.REPLACE
            else -> ExistingWorkPolicy.KEEP
        }

        return DownloadCommand(
            episode = this,
            request = request,
            constraints = constraints,
            policy = policy,
        )
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

    private data class DownloadCommand(
        val episode: BaseEpisode,
        val request: OneTimeWorkRequest,
        val constraints: Constraints,
        val policy: ExistingWorkPolicy,
    ) {
        val episodeUuid get() = episode.uuid

        fun enqueue(manager: WorkManager): Operation {
            return manager.enqueueUniqueWork(
                uniqueWorkName = DownloadEpisodeWorker.episodeTag(episode.uuid),
                existingWorkPolicy = policy,
                request = request,
            )
        }
    }

    private sealed interface QueueTransition {
        val episodeUuids: Collection<String>

        data class Enqueue(
            val episodeToWorkerUuids: Map<String, UUID>,
        ) : QueueTransition {
            override val episodeUuids get() = episodeToWorkerUuids.keys
        }

        data class Remove(
            override val episodeUuids: Collection<String>,
            val disableAutoDownload: Boolean,
        ) : QueueTransition
    }
}

private class DownloadStatusController(
    private val appDatabase: AppDatabase,
    private val context: Context,
) {
    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

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
            episodeDao.updateDownloadStatuses(statusUpdates)
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
                    DownloadStatusUpdate.Failure(id, tooManyAttemptsErrorMessage)
                } else {
                    DownloadStatusUpdate.Idle
                }
            }

            is DownloadWorkInfo.Pending -> {
                when {
                    !constraints.isNetworkAvailable -> DownloadStatusUpdate.WaitingForWifi(id)
                    isWifiRequired && !constraints.isUnmeteredAvailable -> DownloadStatusUpdate.WaitingForWifi(id)
                    isPowerRequired && !constraints.isPowerAvailable -> DownloadStatusUpdate.WaitingForPower(id)
                    isStorageRequired && !constraints.isStorageAvailable -> DownloadStatusUpdate.WaitingForStorage(id)
                    else -> DownloadStatusUpdate.Enqueued(id)
                }
            }

            is DownloadWorkInfo.InProgress -> {
                DownloadStatusUpdate.InProgress(id)
            }

            is DownloadWorkInfo.Success -> {
                DownloadStatusUpdate.Success(id, downloadFile)
            }

            is DownloadWorkInfo.Failure -> {
                DownloadStatusUpdate.Failure(id, errorMessage ?: defaultErrorMessage)
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

private suspend inline fun <reified T : DownloadWorkInfo> WorkManager.getDownloadWorkInfos() = getWorkInfosByTagFlow(DownloadEpisodeWorker.WORKER_TAG)
    .firstOrNull()
    ?.asSequence()
    ?.mapNotNull(DownloadEpisodeWorker::mapToDownloadWorkInfo)
    ?.filterIsInstance<T>()
    ?.associateBy(DownloadWorkInfo::episodeUuid)
    .orEmpty()
