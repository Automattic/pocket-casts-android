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
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UpdateShowNotesTask
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Power
import au.com.shiftyjelly.pocketcasts.utils.extensions.toUuidOrNull
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
class DownloadManager @Inject constructor(
    appDatabase: AppDatabase,
    settings: Settings,
    clock: Clock,
    progressCache: DownloadProgressCache,
    tracker: AnalyticsTracker,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) : DownloadQueue,
    DownloadStatusObserver {
    private val workManager by lazy { WorkManager.getInstance(context) }

    private val downloadDao = EpisodeDownloadDao(appDatabase, clock)

    private val analytics = DownloadAnalytics(tracker)

    private val prerequisitesProvider = DownloadPrerequisitesProvider(context)

    private val statusController = DownloadStatusController(
        downloadDao = downloadDao,
        analytics = analytics,
        clock = clock,
        context = context,
    )

    private val queueController = DownloadQueueController(
        downloadDao = downloadDao,
        transcriptDao = appDatabase.transcriptDao(),
        progressCache = progressCache,
        settings = settings,
        analytics = analytics,
        context = context,
        coroutineScope = scope,
    )

    private val isMonitoring = AtomicBoolean()

    @Volatile
    override var size: Int = 0
        private set

    override fun enqueueAll(episodeUuids: Collection<String>, downloadType: DownloadType, sourceView: SourceView): Job {
        return scope.launch { queueController.addToQueue(episodeUuids, downloadType, sourceView) }
    }

    override fun cancelAll(episodeUuids: Collection<String>, sourceView: SourceView): Job {
        return scope.launch { queueController.removeFromQueue(episodeUuids, sourceView) }
    }

    override fun cancelAll(podcastUuid: String, sourceView: SourceView): Deferred<Collection<BaseEpisode>> {
        return scope.async { queueController.removeFromQueue(podcastUuid, sourceView) }
    }

    override fun cancelAll(sourceView: SourceView): Deferred<Collection<BaseEpisode>> {
        return scope.async { queueController.clearQueue(sourceView) }
    }

    override fun clearAllDownloadErrors(): Job {
        return scope.launch { statusController.clearAllErrors() }
    }

    @OptIn(FlowPreview::class)
    override fun monitorDownloadStatus() {
        if (isMonitoring.getAndSet(true)) {
            return
        }

        scope.launch {
            queueController.cleanUpStaleDownloads()

            val constraintsFlow = prerequisitesProvider.getConstraintsFlow()
            val workInfosFlow = workManager
                .getWorkInfosByTagFlow(DownloadEpisodeWorker.WORKER_TAG)
                .conflate()
                .map { infos -> infos.mapNotNull(DownloadEpisodeWorker::mapToDownloadWorkInfo) }
            combine(workInfosFlow, constraintsFlow, ::Pair).collect { (infos, constraints) ->
                size = infos.count(DownloadWorkInfo::isCancellable)

                statusController.updateStatuses(infos, constraints)
                queueController.cancelExcessiveDownloads(infos)
            }
        }
    }
}

private class DownloadQueueController(
    private val downloadDao: EpisodeDownloadDao,
    private val transcriptDao: TranscriptDao,
    private val progressCache: DownloadProgressCache,
    private val settings: Settings,
    private val analytics: DownloadAnalytics,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    suspend fun cleanUpStaleDownloads() {
        val cancellableEpisodes = downloadDao.findCancellableEpisodes()
        val pendingWorks = workManager
            .getDownloadWorkInfos<DownloadWorkInfo>()
            .filterValues(DownloadWorkInfo::isCancellable)

        downloadDao.withTransaction {
            cancellableEpisodes.forEach { episode ->
                val work = pendingWorks[episode.uuid]
                val taskId = episode.downloadTaskId?.toUuidOrNull()
                if (work == null && taskId != null) {
                    clearStaleDownload(episode, taskId)
                }
            }
            clearCancellableDownloads()
        }
    }

    suspend fun addToQueue(episodeUuids: Collection<String>, downloadType: DownloadType, sourceView: SourceView) {
        val episodes = downloadDao
            .findEpisodes(episodeUuids)
            .filter { episode -> canDownload(episode, downloadType) }
        if (episodes.isEmpty()) {
            return
        }

        val pendingWorks = workManager.getDownloadWorkInfos<DownloadWorkInfo.Pending>()
        val downloadCommands = episodes.map { episode -> episode.toDownloadCommand(pendingWorks, downloadType, sourceView) }
        val appliedCommands = saveDownloadCommands(downloadCommands)
        analytics.trackDownloadQueued(appliedCommands.map(DownloadCommand::episode), downloadType, sourceView)

        appliedCommands.forEach { command ->
            val operation = command.enqueue(workManager)
            val operationState = operation.state.asFlow().firstOrNull()
            if (operationState != null && operationState !is Operation.State.FAILURE) {
                enqueueShowNotesUpdate(command.episode, command.constraints)
            }
        }
    }

    private suspend fun saveDownloadCommands(commands: Collection<DownloadCommand>): List<DownloadCommand> {
        return if (commands.isNotEmpty()) {
            downloadDao.withTransaction {
                buildList {
                    commands.forEach { command ->
                        val isReady = setReadyForDownload(
                            episode = command.episode,
                            taskId = command.request.id,
                            forceNewDownload = command.policy == ExistingWorkPolicy.REPLACE,
                        )
                        if (isReady) {
                            add(command)
                        }
                    }
                }
            }
        } else {
            emptyList()
        }
    }

    suspend fun removeFromQueue(episodeUuids: Collection<String>, sourceView: SourceView): Collection<BaseEpisode> {
        if (episodeUuids.isEmpty()) {
            return emptySet()
        }

        val resetEpisodes = downloadDao.withTransaction {
            val episodes = findEpisodes(episodeUuids)
            buildMap {
                episodes.forEach { episode ->
                    val isReset = resetDownloadStatus(episode)
                    if (isReset) {
                        put(episode.uuid, episode)
                    }
                }
            }
        }
        val cancelledEpisodes = resetEpisodes.values.filter { it.downloadStatus.isCancellable }
        analytics.trackDownloadCancelled(cancelledEpisodes, sourceView)
        cleanUpDownloads(resetEpisodes, sourceView)

        // Use provided episode UUIDs directly in case our DB is desynchronized
        // with the WorkManager's DB. After cancellation from Work Manager
        // the state will be eventually consistent thanks to status updates.
        episodeUuids.forEach { episodeUuid ->
            workManager.cancelAllWorkByTag(DownloadEpisodeWorker.episodeTag(episodeUuid))
            workManager.cancelAllWorkByTag(UpdateShowNotesTask.episodeTag(episodeUuid))
        }
        return resetEpisodes.values
    }

    private fun cleanUpDownloads(episodes: Map<String, BaseEpisode>, sourceView: SourceView) {
        coroutineScope.launch {
            progressCache.clearProgress(episodes.keys)
            transcriptDao.deleteForEpisodes(episodes.keys)
            val deletedEpisodes = episodes.mapNotNull { (_, episode) ->
                val deletedEpisode = runCatching {
                    val isFileDeleted = episode.downloadedFilePath?.let(::File)?.delete() == true
                    if (isFileDeleted) episode else null
                }
                if (episode is UserEpisode) {
                    episode.artworkUrl
                        ?.takeIf { it.startsWith('/') }
                        ?.let(FileUtil::deleteFileByPath)
                }
                deletedEpisode.getOrNull()
            }
            analytics.trackDownloadDeleted(deletedEpisodes, sourceView)
        }
    }

    suspend fun removeFromQueue(podcastUuid: String, sourceView: SourceView): Collection<BaseEpisode> {
        val episodeUuids = downloadDao.findPodcastEpisodesUuids(podcastUuid)
        val removedEpisodes = removeFromQueue(episodeUuids, sourceView)
        workManager.cancelAllWorkByTag(DownloadEpisodeWorker.podcastTag(podcastUuid))
        workManager.cancelAllWorkByTag(UpdateShowNotesTask.podcastTag(podcastUuid))
        return removedEpisodes
    }

    suspend fun clearQueue(sourceView: SourceView): Collection<BaseEpisode> {
        val episodeUuids = downloadDao.findCancellableEpisodes().map(BaseEpisode::uuid)
        val removedEpisodes = removeFromQueue(episodeUuids, sourceView)
        workManager.cancelAllWorkByTag(DownloadEpisodeWorker.WORKER_TAG)
        workManager.cancelAllWorkByTag(UpdateShowNotesTask.WORKER_TAG)
        return removedEpisodes
    }

    fun cancelExcessiveDownloads(workInfos: Collection<DownloadWorkInfo>) {
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
        val infos = workInfos
            .filterIsInstance<DownloadWorkInfo.Pending>()
            .filter(DownloadWorkInfo::isTooManyAttempts)

        for (info in infos) {
            workManager.cancelAllWorkByTag(DownloadEpisodeWorker.episodeTag(info.episodeUuid))
            workManager.cancelAllWorkByTag(UpdateShowNotesTask.episodeTag(info.episodeUuid))
        }
    }

    private fun canDownload(episode: BaseEpisode, downloadType: DownloadType): Boolean {
        val isFileAvailable = when (episode) {
            is PodcastEpisode -> true
            is UserEpisode -> episode.isUploaded
        }
        val isDownloadTypeAllowed = when (downloadType) {
            is DownloadType.UserTriggered -> true
            is DownloadType.Automatic -> !episode.isDownloadFailure && (downloadType.bypassAutoDownloadStatus || !episode.isAutoDownloadDisabled)
        }
        return !episode.isDownloaded && isFileAvailable && isDownloadTypeAllowed
    }

    private fun BaseEpisode.toDownloadCommand(
        pendingWorks: Map<String, DownloadWorkInfo.Pending>,
        downloadType: DownloadType,
        sourceView: SourceView,
    ): DownloadCommand {
        val pendingWork = pendingWorks[uuid]
        val args = toDownloadArgs(downloadType, sourceView)
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

    private fun BaseEpisode.toDownloadArgs(
        downloadType: DownloadType,
        sourceView: SourceView,
    ) = when (this) {
        is PodcastEpisode -> DownloadEpisodeWorker.Args(
            episodeUuid = uuid,
            podcastUuid = podcastUuid,
            waitForWifi = when (downloadType) {
                is DownloadType.UserTriggered -> downloadType.waitForWifi
                is DownloadType.Automatic -> settings.autoDownloadUnmeteredOnly.value
            },
            waitForPower = when (downloadType) {
                is DownloadType.UserTriggered -> false
                is DownloadType.Automatic -> settings.autoDownloadOnlyWhenCharging.value
            },
            sourceView = sourceView,
        )

        is UserEpisode -> DownloadEpisodeWorker.Args(
            episodeUuid = uuid,
            podcastUuid = podcastOrSubstituteUuid,
            waitForWifi = when (downloadType) {
                is DownloadType.UserTriggered -> downloadType.waitForWifi
                is DownloadType.Automatic -> settings.cloudDownloadOnlyOnWifi.value
            },
            waitForPower = false,
            sourceView = sourceView,
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
        fun enqueue(manager: WorkManager): Operation {
            return manager.enqueueUniqueWork(
                uniqueWorkName = DownloadEpisodeWorker.episodeTag(episode.uuid),
                existingWorkPolicy = policy,
                request = request,
            )
        }
    }
}

private class DownloadStatusController(
    private val downloadDao: EpisodeDownloadDao,
    private val analytics: DownloadAnalytics,
    private val clock: Clock,
    private val context: Context,
) {
    suspend fun updateStatuses(infos: List<DownloadWorkInfo>, constraints: DownloadPrerequisites) {
        if (infos.isEmpty()) {
            return
        }

        val tooManyAttemptsError = context.getString(LR.string.error_download_too_many_attempts)
        val defaultErrorMessage = context.getString(LR.string.error_download_generic_failure, "").trim()
        val workUpdates = infos.associateWith { info ->
            info.toStatusUpdate(
                constraints = constraints,
                tooManyAttemptsErrorMessage = tooManyAttemptsError,
                defaultErrorMessage = defaultErrorMessage,
            )
        }

        val results = DownloadWorkResults(infos.size, context)
        downloadDao.withTransaction {
            workUpdates.forEach { (info, update) ->
                val isStatusUpdated = updateDownloadStatus(info.episodeUuid, update)
                if (isStatusUpdated) {
                    results.add(info)
                }
            }
        }
        analytics.trackDownloadFinished(results.successes)
        analytics.trackDownloadFailed(results.failures)
    }

    suspend fun clearAllErrors() {
        downloadDao.clearDownloadFailures()
    }

    private fun DownloadWorkInfo.toStatusUpdate(
        constraints: DownloadPrerequisites,
        tooManyAttemptsErrorMessage: String,
        defaultErrorMessage: String,
    ): DownloadStatusUpdate {
        val now = clock.instant()
        return when (this) {
            is DownloadWorkInfo.Cancelled -> {
                if (isTooManyAttempts) {
                    DownloadStatusUpdate.Failure(
                        taskId = id,
                        issuedAt = now,
                        errorMessage = tooManyAttemptsErrorMessage,
                    )
                } else {
                    DownloadStatusUpdate.Cancelled(
                        taskId = id,
                        issuedAt = now,
                    )
                }
            }

            is DownloadWorkInfo.Pending -> {
                when {
                    !constraints.isNetworkAvailable -> DownloadStatusUpdate.WaitingForWifi(
                        taskId = id,
                        issuedAt = now,
                    )

                    isWifiRequired && !constraints.isUnmeteredAvailable -> DownloadStatusUpdate.WaitingForWifi(
                        taskId = id,
                        issuedAt = now,
                    )

                    isPowerRequired && !constraints.isPowerAvailable -> DownloadStatusUpdate.WaitingForPower(
                        taskId = id,
                        issuedAt = now,
                    )

                    isStorageRequired && !constraints.isStorageAvailable -> DownloadStatusUpdate.WaitingForStorage(
                        taskId = id,
                        issuedAt = now,
                    )

                    else -> DownloadStatusUpdate.Enqueued(
                        taskId = id,
                        issuedAt = now,
                    )
                }
            }

            is DownloadWorkInfo.InProgress -> {
                DownloadStatusUpdate.InProgress(
                    taskId = id,
                    issuedAt = now,
                )
            }

            is DownloadWorkInfo.Success -> {
                DownloadStatusUpdate.Success(
                    taskId = id,
                    issuedAt = now,
                    outputFile = downloadFile,
                )
            }

            is DownloadWorkInfo.Failure -> {
                DownloadStatusUpdate.Failure(
                    taskId = id,
                    issuedAt = now,
                    errorMessage = errorMessage ?: defaultErrorMessage,
                )
            }
        }
    }

    private class DownloadWorkResults(initialCapacity: Int, context: Context) {
        private val _successes = ArrayList<DownloadWorkInfo.Success>(initialCapacity)
        val successes: List<DownloadWorkInfo.Success> get() = _successes

        private val _failures = ArrayList<DownloadWorkInfo.Failure>(initialCapacity)
        val failures: List<DownloadWorkInfo.Failure> get() = _failures

        private val isCellularConnection = Network.isCellularConnection(context)
        private val isProxyConnection = Network.isVpnConnection(context)

        fun add(info: DownloadWorkInfo) {
            when (info) {
                is DownloadWorkInfo.Success -> _successes.add(info)

                is DownloadWorkInfo.Failure -> _failures.add(info)

                is DownloadWorkInfo.Cancelled -> {
                    if (info.isTooManyAttempts) {
                        _failures.add(info.synthesizeFailure())
                    }
                }

                is DownloadWorkInfo.InProgress, is DownloadWorkInfo.Pending -> Unit
            }
        }

        private fun DownloadWorkInfo.Cancelled.synthesizeFailure() = DownloadWorkInfo.Failure(
            id = id,
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
            runAttemptCount = runAttemptCount,
            sourceView = sourceView,
            error = EpisodeDownloadError(
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                reason = EpisodeDownloadError.Reason.TooManyAttempts,
                isCellular = isCellularConnection,
                isProxy = isProxyConnection,
            ),
            errorMessage = null,
        )
    }
}

private class EpisodeDownloadDao(
    private val appDatabase: AppDatabase,
    private val clock: Clock,
) {
    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()
    private val cancellableStatuses = EpisodeDownloadStatus.entries.filter { it.isCancellable }

    suspend fun <R> withTransaction(block: suspend EpisodeDownloadDao.() -> R) = appDatabase.withTransaction {
        block()
    }

    suspend fun findCancellableEpisodes() = appDatabase.withTransaction<List<BaseEpisode>> {
        buildList {
            addAll(episodeDao.getEpisodesWithDownloadStatus(cancellableStatuses))
            addAll(userEpisodeDao.getEpisodesWithDownloadStatus(cancellableStatuses))
        }
    }

    suspend fun findPodcastEpisodes(uuids: Collection<String>) = episodeDao.findByUuids(uuids)

    suspend fun findUserEpisodes(uuids: Collection<String>) = userEpisodeDao.findEpisodesByUuids(uuids)

    suspend fun findEpisodes(uuids: Collection<String>) = withTransaction<List<BaseEpisode>> {
        findPodcastEpisodes(uuids) + findUserEpisodes(uuids)
    }

    suspend fun findPodcastEpisodesUuids(podcastUuid: String) = episodeDao.findByPodcastUuid(podcastUuid)

    suspend fun setReadyForDownload(episode: BaseEpisode, taskId: UUID, forceNewDownload: Boolean): Boolean {
        val now = clock.instant()
        return when (episode) {
            is PodcastEpisode -> episodeDao.setReadyForDownload(episode.uuid, taskId, now, forceNewDownload)
            is UserEpisode -> userEpisodeDao.setReadyForDownload(episode.uuid, taskId, now, forceNewDownload)
        }
    }

    suspend fun resetDownloadStatus(episode: BaseEpisode) = when (episode) {
        is PodcastEpisode -> episodeDao.resetDownloadStatus(episode.uuid)
        is UserEpisode -> userEpisodeDao.resetDownloadStatus(episode.uuid)
    }

    suspend fun updateDownloadStatus(episodeUuid: String, update: DownloadStatusUpdate) = withTransaction {
        val isPodcastEpisodeUpdated = episodeDao.updateDownloadStatus(episodeUuid, update)
        val isUserEpisodeUpdated = if (!isPodcastEpisodeUpdated) {
            userEpisodeDao.updateDownloadStatus(episodeUuid, update)
        } else {
            false
        }
        isPodcastEpisodeUpdated || isUserEpisodeUpdated
    }

    suspend fun clearStaleDownload(episode: BaseEpisode, taskId: UUID) = when (episode) {
        is PodcastEpisode -> episodeDao.clearDownloadForEpisode(episode.uuid, taskId.toString())
        is UserEpisode -> userEpisodeDao.clearDownloadForEpisode(episode.uuid, taskId.toString())
    }

    suspend fun clearCancellableDownloads() = withTransaction {
        episodeDao.clearDownloadsWithoutTaskId(cancellableStatuses)
        userEpisodeDao.clearDownloadsWithoutTaskId(cancellableStatuses)
    }

    suspend fun clearDownloadFailures() = withTransaction {
        val statuses = setOf(EpisodeDownloadStatus.DownloadFailed)
        episodeDao.clearDownloadsWithoutTaskId(statuses)
        userEpisodeDao.clearDownloadsWithoutTaskId(statuses)
    }
}

private class DownloadAnalytics(
    private val tracker: AnalyticsTracker,
) {
    fun trackDownloadQueued(episodes: Collection<BaseEpisode>, downloadType: DownloadType, sourceView: SourceView) {
        for (episode in episodes) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download queued. Episode: ${episode.uuid}, Download type: $downloadType, Source: $sourceView.")
        }
        when (episodes.size) {
            0 -> Unit

            1 -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED,
                    mapOf(
                        "episode_uuid" to episodes.first().uuid,
                        "source" to sourceView.analyticsValue,
                    ),
                )
            }

            else -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_BULK_DOWNLOAD_QUEUED,
                    mapOf(
                        "count" to episodes.size,
                        "source" to sourceView.analyticsValue,
                    ),
                )
            }
        }
    }

    fun trackDownloadFinished(infos: Collection<DownloadWorkInfo.Success>) {
        for (info in infos) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download finished. Episode: ${info.episodeUuid}, Source: ${info.sourceView}.")
            tracker.track(
                AnalyticsEvent.EPISODE_DOWNLOAD_FINISHED,
                mapOf(
                    "episode_uuid" to info.episodeUuid,
                    "source" to info.sourceView.analyticsValue,
                ),
            )
        }
    }

    fun trackDownloadFailed(infos: Collection<DownloadWorkInfo.Failure>) {
        for (info in infos) {
            val errorProperties = info.error.toProperties()
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download failed. Episode: ${info.episodeUuid}, Error: $errorProperties, Source: ${info.sourceView}.")
            tracker.track(
                AnalyticsEvent.EPISODE_DOWNLOAD_FAILED,
                errorProperties,
            )
        }
    }

    fun trackDownloadCancelled(episodes: Collection<BaseEpisode>, sourceView: SourceView) {
        for (episode in episodes) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download cancelled. Episode: ${episode.uuid}, Source: $sourceView.")
        }
        when (episodes.size) {
            0 -> Unit

            1 -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_DOWNLOAD_CANCELLED,
                    mapOf(
                        "episode_uuid" to episodes.first().uuid,
                        "source" to sourceView.analyticsValue,
                    ),
                )
            }

            else -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_BULK_DOWNLOAD_CANCELLED,
                    mapOf(
                        "count" to episodes.size,
                        "source" to sourceView.analyticsValue,
                    ),
                )
            }
        }
    }

    fun trackDownloadDeleted(episodes: Collection<BaseEpisode>, sourceView: SourceView) {
        for (episode in episodes) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download deleted. Episode: ${episode.uuid}, Source: $sourceView.")
        }
        when (episodes.size) {
            0 -> Unit

            1 -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                    mapOf(
                        "episode_uuid" to episodes.first().uuid,
                        "source" to sourceView.analyticsValue,
                    ),
                )
            }

            else -> {
                tracker.track(
                    AnalyticsEvent.EPISODE_BULK_DOWNLOAD_DELETED,
                    mapOf(
                        "count" to episodes.size,
                        "source" to sourceView.analyticsValue,
                    ),
                )
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
    private val context: Context,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

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
