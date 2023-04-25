package au.com.shiftyjelly.pocketcasts.repositories.download

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.toLiveData
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationId
import au.com.shiftyjelly.pocketcasts.repositories.R
import au.com.shiftyjelly.pocketcasts.repositories.download.task.DownloadEpisodeTask
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UpdateEpisodeTask
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UpdateShowNotesTask
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageException
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Power
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.subjects.ReplaySubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR

class DownloadManagerImpl @Inject constructor(
    private val fileStorage: FileStorage,
    private val settings: Settings,
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context,
    private val episodeAnalytics: EpisodeAnalytics
) : DownloadManager, CoroutineScope {

    companion object {
        private const val MIN_TIME_BETWEEN_UPDATE_REPORTS: Long = 500 // 500ms;
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastReportedNotificationTime: Long = 0
    private var notificationBuilder: NotificationCompat.Builder? = null
    private lateinit var podcastManager: PodcastManager
    private lateinit var episodeManager: EpisodeManager
    private lateinit var playlistManager: PlaylistManager
    private lateinit var playbackManager: PlaybackManager

    private val pendingQueue = HashMap<String, DownloadingInfo>()
    private val downloadingQueue = ArrayList<DownloadingInfo>()
    private val downloadsCoroutineContext = Dispatchers.Default

    override val progressUpdates: MutableMap<String, DownloadProgressUpdate> = mutableMapOf()
    override val progressUpdateRelay: Subject<DownloadProgressUpdate> = ReplaySubject.createWithSize(20)

    private var workManagerListener: LiveData<Pair<List<WorkInfo>, Map<String?, String>>>? = null

    override fun setup(episodeManager: EpisodeManager, podcastManager: PodcastManager, playlistManager: PlaylistManager, playbackManager: PlaybackManager) {
        this.episodeManager = episodeManager
        this.podcastManager = podcastManager
        this.playlistManager = playlistManager
        this.playbackManager = playbackManager

        progressUpdateRelay
            .sample(MIN_TIME_BETWEEN_UPDATE_REPORTS, TimeUnit.MILLISECONDS)
            .doOnNext { updateNotification() }
            .subscribe()
    }

    private fun updateProgress(downloadProgressUpdate: DownloadProgressUpdate) {
        launch(Dispatchers.Main) {
            progressUpdates[downloadProgressUpdate.episodeUuid] = downloadProgressUpdate
            progressUpdateRelay.toSerialized().onNext(downloadProgressUpdate)
        }
    }

    override fun beginMonitoringWorkManager(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val episodeFlowable = episodeManager.observeDownloadingEpisodesRx()
            .distinctUntilChanged { t1, t2 -> // We only really need to make sure we have all the downloading episodes available, we don't care when their metadata changes
                t1.map { it.uuid }.toSet() == t2.map { it.uuid }.toSet()
            }
            .map { list ->
                list.associateBy({ it.downloadTaskId }, { it.uuid }) // Convert to map for easy lookup
            }

        launch(downloadsCoroutineContext) {
            cleanUpStaleDownloads(workManager)
        }

        val episodeLiveData = episodeFlowable.toLiveData()
        workManagerListener = workManager.getWorkInfosByTagLiveData(DownloadManager.WORK_MANAGER_DOWNLOAD_TAG).combineLatest(episodeLiveData)

        workManagerListener?.observeForever { (tasks, episodeUuids) ->
            tasks.forEach { workInfo ->
                val taskId = workInfo.id.toString()
                val episodeUUID = episodeUuids[taskId]
                if (episodeUUID != null) {
                    val info = DownloadingInfo(episodeUUID, workInfo.id)
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                            launch(downloadsCoroutineContext) {
                                pendingQueue[episodeUUID] = DownloadingInfo(episodeUUID, workInfo.id)
                                episodeManager.findEpisodeByUuid(episodeUUID)?.let { episode ->

                                    // FIXME this is a hack to avoid an issue where this listener says downloads
                                    //  on the watch app are enqueued when they are actually still running.
                                    val queriedState = workManager.getWorkInfoById(workInfo.id).get().state
                                    if (Util.isWearOs(context) && queriedState == WorkInfo.State.RUNNING) {
                                        getRequirementsAsync(episode)
                                    } else {
                                        getRequirementsAndSetStatusAsync(episode)
                                    }
                                }
                                synchronized(downloadingQueue) {
                                    if (downloadingQueue.contains(info)) {
                                        downloadingQueue.remove(info)
                                    }
                                }
                            }
                        }
                        WorkInfo.State.RUNNING -> {
                            pendingQueue.remove(episodeUUID)
                            launch(downloadsCoroutineContext) {
                                synchronized(downloadingQueue) {
                                    if (!downloadingQueue.contains(info)) {
                                        downloadingQueue.add(info)
                                    }
                                }
                                workInfo.progress.toDownloadProgressUpdate()?.let {
                                    updateProgress(it)
                                }
                            }
                        }
                        WorkInfo.State.CANCELLED -> {
                            pendingQueue.remove(episodeUUID)
                            launch(downloadsCoroutineContext) {
                                synchronized(downloadingQueue) {
                                    downloadingQueue.remove(info)
                                }
                                stopDownloadingEpisode(episodeUUID, "work manager cancel status")

                                episodeManager.findEpisodeByUuid(episodeUUID)?.let {
                                    episodeManager.updateDownloadTaskId(it, null)
                                    if (!it.isDownloaded && it.episodeStatus != EpisodeStatusEnum.NOT_DOWNLOADED) {
                                        episodeManager.updateEpisodeStatus(it, EpisodeStatusEnum.NOT_DOWNLOADED)
                                    }
                                }
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            launch(downloadsCoroutineContext) {
                                synchronized(downloadingQueue) {
                                    downloadingQueue.remove(info)
                                }

                                val errorMessage = workInfo.outputData.getString(DownloadEpisodeTask.OUTPUT_ERROR_MESSAGE)
                                episodeDidDownload(DownloadResult.failedResult(workInfo.id, errorMessage, episodeUUID))
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            launch(downloadsCoroutineContext) {
                                Timber.d("Worker succeeded: $episodeUUID")
                                synchronized(downloadingQueue) {
                                    downloadingQueue.remove(info)
                                }

                                val wasCancelled = workInfo.outputData.getBoolean(
                                    DownloadEpisodeTask.OUTPUT_CANCELLED, false
                                )
                                if (!wasCancelled) {
                                    episodeDidDownload(DownloadResult.successResult(workInfo.id, episodeUUID))
                                }
                            }
                        }
                        else -> {
                            Timber.d("Work manager update: $episodeUUID is ${workInfo.state}")
                        }
                    }
                }
            }

            updateNotification()
        }
    }

    // Due to a previous bug it is possible to have episodes with workmanager task ids that aren't in workmanager
    // this causes them to not download. We clean them up here.
    private suspend fun cleanUpStaleDownloads(workManager: WorkManager) = withContext(downloadsCoroutineContext) {
        val staleDownloads = episodeManager.findStaleDownloads()

        for (episode in staleDownloads) {
            val taskId = episode.downloadTaskId ?: continue
            val uuid = UUID.fromString(taskId)

            try {
                val state = workManager.getWorkInfoById(uuid).get()
                if (state == null) {
                    episodeManager.updateDownloadTaskId(episode, null)
                    LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Cleaned up old workmanager task for ${episode.uuid}.")
                } else {
                    // This should not happen
                    LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Workmanager knows about ${episode.uuid} but it is marked as not downloaded.")
                }
            } catch (e: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not clean up stale download ${episode.uuid}.", e)
            }
        }
    }

    override fun hasPendingOrRunningDownloads(): Boolean {
        return pendingQueue.isNotEmpty() || downloadingQueue.isNotEmpty()
    }

    override fun stopAllDownloads() {
        pendingQueue.toList().forEach { (_, info) -> stopDownloadingEpisode(info.episodeUUID, "Cancel all") }
        synchronized(downloadingQueue) {
            downloadingQueue.toList().forEach { stopDownloadingEpisode(it.episodeUUID, "Cancel all") }
        }
        launch {
            val downloadingEpisodes = episodeManager.findEpisodesDownloading()
            downloadingEpisodes.forEach {
                stopDownloadingEpisode(it.uuid, "Cancel all")
                episodeManager.updateEpisodeStatus(it, EpisodeStatusEnum.NOT_DOWNLOADED)
            }
        }
        WorkManager.getInstance(context).cancelAllWorkByTag(DownloadManager.WORK_MANAGER_DOWNLOAD_TAG)
        updateNotification()
    }

    private val addDownloadMutex = Mutex()

    // We only want to be able to queue one download at a time
    override fun addEpisodeToQueue(episode: BaseEpisode, from: String, fireEvent: Boolean) {
        launch(downloadsCoroutineContext) {
            addDownloadMutex.withLock {
                val updatedEpisode = episodeManager.findEpisodeByUuid(episode.uuid) ?: return@launch // Get the latest episode so we can check if it's downloaded
                // if this episode is already in the queue or downloading, ignore it
                if (updatedEpisode.isDownloaded || updatedEpisode.downloadTaskId != null) {
                    LogBuffer.i(
                        LogBuffer.TAG_BACKGROUND_TASKS,
                        "Attempted to add episode to downloads from $from but it was rejected. " +
                            "isDownloaded: ${updatedEpisode.isDownloaded} " +
                            "pendingQueueContains: ${pendingQueue.containsKey(episode.uuid)} " +
                            "episodeTaskId: ${updatedEpisode.downloadTaskId}"
                    )
                    return@launch
                }

                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Added episode to downloads. ${episode.uuid} podcast: ${(episode as? PodcastEpisode)?.podcastUuid} from: $from")
                val networkRequirements = getRequirementsAndSetStatusAsync(episode)
                episodeManager.updateLastDownloadAttemptDate(episode)
                addWorkManagerTask(episode, networkRequirements)
            }

            updateNotification()

            // Mark as unplayed, which will also unarchive the episode
            if (episode.playingStatus == EpisodePlayingStatus.COMPLETED) {
                episodeManager.markAsNotPlayed(episode)
            } else {
                episodeManager.unarchive(episode)
            }
        }
    }

    private suspend fun getRequirementsAsync(episode: BaseEpisode): NetworkRequirements =
        withContext(downloadsCoroutineContext) {
            networkRequiredForEpisode(episode)
        }

    override suspend fun getRequirementsAndSetStatusAsync(episode: BaseEpisode): NetworkRequirements {
        return withContext(downloadsCoroutineContext) {
            val networkRequirements = networkRequiredForEpisode(episode)
            updateEpisodeStatusAsync(episode, networkRequirements).await()
            return@withContext networkRequirements
        }
    }

    private fun addWorkManagerTask(episode: BaseEpisode, networkRequirements: NetworkRequirements) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkRequirements.toWorkManagerEnum())
                .setRequiresCharging(networkRequirements.requiresPower)
                .build()
            val updateData = Data.Builder()
                .putString(UpdateEpisodeTask.INPUT_EPISODE_UUID, episode.uuid)
                .putString(UpdateEpisodeTask.INPUT_PODCAST_UUID, (episode as? PodcastEpisode)?.podcastUuid)
                .build()
            val updateTask = OneTimeWorkRequestBuilder<UpdateEpisodeTask>()
                .setInputData(updateData)
                .setConstraints(constraints)
                .addTag(episode.uuid)
                .build()

            val downloadData = Data.Builder()
                .putString(DownloadEpisodeTask.INPUT_EPISODE_UUID, episode.uuid)
                .putString(DownloadEpisodeTask.INPUT_PATH_TO_SAVE_TO, DownloadHelper.pathForEpisode(episode, fileStorage))
                .putString(DownloadEpisodeTask.INPUT_TEMP_PATH, DownloadHelper.tempPathForEpisode(episode, fileStorage))
                .build()
            val downloadTask = OneTimeWorkRequestBuilder<DownloadEpisodeTask>()
                .setInputData(downloadData)
                .setConstraints(constraints)
                .addTag(DownloadManager.WORK_MANAGER_DOWNLOAD_TAG)
                .addTag(episode.uuid)
                .build()

            val cacheShowNotesData = Data.Builder()
                .putString(UpdateShowNotesTask.INPUT_EPISODE_UUID, episode.uuid)
                .build()
            val cacheShowNotesTask = OneTimeWorkRequestBuilder<UpdateShowNotesTask>()
                .setInputData(cacheShowNotesData)
                .addTag(episode.uuid)
                .build()

            episodeManager.updateDownloadTaskId(episode, downloadTask.id.toString())
            WorkManager.getInstance(context).beginWith(updateTask).then(listOf(downloadTask, cacheShowNotesTask)).enqueue()
        } catch (storageException: StorageException) {
            launch(downloadsCoroutineContext) {
                episodeDidDownload(DownloadResult.failedResult(null, "Insufficient storage space", episode.uuid))
            }
        }
    }

    private fun updateEpisodeStatusAsync(episode: BaseEpisode, networkRequirements: NetworkRequirements): Deferred<Unit> {
        return async {
            val status = getEpisodeStatusForRequirements(networkRequirements)
            if (status != episode.episodeStatus) {
                episodeManager.updateEpisodeStatus(episode, status)
            }
        }
    }

    private fun getEpisodeStatusForRequirements(networkRequirements: NetworkRequirements): EpisodeStatusEnum {
        return when {
            networkRequirements.requiresUnmetered && !Network.isUnmeteredConnection(context) -> EpisodeStatusEnum.WAITING_FOR_WIFI
            networkRequirements.requiresPower && !Power.isConnected(context) -> EpisodeStatusEnum.WAITING_FOR_POWER
            else -> EpisodeStatusEnum.QUEUED
        }
    }

    override fun removeEpisodeFromQueue(episode: BaseEpisode, from: String) {
        launch(downloadsCoroutineContext) {
            episode.downloadTaskId?.let {
                WorkManager.getInstance(context).cancelWorkById(UUID.fromString(it))
            }
            stopDownloadingEpisode(episode.uuid, from)

            updateNotification()
        }
    }

    private fun stopDownloadingJob(info: DownloadingInfo) {
        WorkManager.getInstance(context).cancelWorkById(info.jobId)
        WorkManager.getInstance(context).cancelAllWorkByTag(info.episodeUUID)
        progressUpdates.remove(info.episodeUUID)
    }

    private suspend fun episodeDidDownload(result: DownloadResult) = withContext(downloadsCoroutineContext) {
        val episode = episodeManager.findEpisodeByUuid(result.episodeUuid)

        try {
            pendingQueue.remove(result.episodeUuid)
            progressUpdates.remove(result.episodeUuid)
            if (episode == null) {
                removeDownloadingEpisode(result.episodeUuid)
                return@withContext
            }

            episodeManager.updateDownloadTaskId(episode, null)

            if (result.success) {
                episodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADED)
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_DOWNLOAD_FINISHED, uuid = episode.uuid)

                RefreshPodcastsThread.updateNotifications(settings.getNotificationLastSeen(), settings, podcastManager, episodeManager, notificationHelper, context)
            } else {
                episodeManager.setDownloadFailed(
                    episode,
                    result.errorMessage?.split(":")?.last()
                        ?: "Download failed"
                )
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_DOWNLOAD_FAILED, uuid = episode.uuid)
            }
        } catch (t: Throwable) {
            Timber.e(t)
        }

        // remove this episode from the downloading queue
        episode?.let { removeDownloadingEpisode(it.uuid) }
        updateNotification()
    }

    private fun removeDownloadingEpisode(episodeUuid: String) {
        synchronized(downloadingQueue) {
            val iterator = downloadingQueue.listIterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.episodeUUID == episodeUuid) {
                    iterator.remove()
                }
            }
        }
    }

    private fun stopDownloadingEpisode(episodeUuid: String, from: String) {
        launch(downloadsCoroutineContext) {
            if (pendingQueue.containsKey(episodeUuid)) {
                pendingQueue.remove(episodeUuid)?.let { stopDownloadingJob(it) }
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removed episode from downloads. $episodeUuid from: $from")
            }

            synchronized(downloadingQueue) {
                if (downloadingQueue.size > 0) {
                    val index = downloadingQueue.indexOfFirst { it.episodeUUID == episodeUuid }
                    if (index >= 0) {
                        // episode being downloaded, cancel
                        val task = downloadingQueue.removeAt(index)
                        stopDownloadingJob(task)
                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removed episode from downloads. $episodeUuid from: $from")
                    }
                }
            }

            progressUpdates.remove(episodeUuid)
        }
    }

    private fun networkRequiredForEpisode(episode: BaseEpisode): NetworkRequirements {
        // user has tapped download
        if (!episode.isAutoDownloaded) {
            // user said yes to warning dialog
            return if (episode.isManualDownloadOverridingWifiSettings || !settings.warnOnMeteredNetwork()) {
                NetworkRequirements.runImmediately()
            } else NetworkRequirements.needsUnmetered()
        } else if (episode is UserEpisode) {
            // UserEpisodes have their own auto download setting
            return if (settings.getCloudOnlyWifi()) {
                NetworkRequirements.needsUnmetered()
            } else {
                NetworkRequirements.runImmediately()
            }
        }

        val networkRequirements = NetworkRequirements.mostStringent()

        networkRequirements.requiresUnmetered = settings.isPodcastAutoDownloadUnmeteredOnly()
        networkRequirements.requiresPower = settings.isPodcastAutoDownloadPowerOnly()

        return networkRequirements
    }

    private fun updateNotification() {

        // Don't show these notifications on wear os
        if (Util.isWearOs(context)) return

        launch(downloadsCoroutineContext) {
            var progress = 0.0
            var max = 0.0

            var count: Int
            var firstUuid: String
            synchronized(downloadingQueue) {
                if (downloadingQueue.isEmpty()) {
                    notificationManager.cancel(NotificationId.DOWNLOADING.value)
                    return@launch
                }

                for (task in downloadingQueue) {
                    progressUpdates[task.episodeUUID]?.let { downloadProgress ->
                        val (_, _, _, _, downloadedSoFar, totalToDownload) = downloadProgress
                        progress += downloadedSoFar.toDouble()
                        max += totalToDownload.toDouble()
                    }
                }

                firstUuid = downloadingQueue.first().episodeUUID
                count = downloadingQueue.size
            }

            val episodeOne: PodcastEpisode = episodeManager.findByUuid(firstUuid) ?: return@launch
            val podcastOneName = podcastManager.findPodcastByUuid(episodeOne.podcastUuid)?.title
                ?: ""

            val title: String
            val text: String
            if (count == 1) {
                title = podcastOneName.ifBlank { "Downloading episode" }
                text = episodeOne.title
            } else {
                title = String.format("Downloading %d episodes", count)
                text = truncateString(podcastOneName, 20) + " + ${count - 1} more"
            }

            val info = if (max == 0.0) null else (progress / max * 100).toInt().toString() + "%"

            val notificationBuilder = getNotificationBuilder()
            notificationBuilder.setContentTitle(title)
            notificationBuilder.setContentText(text)
            notificationBuilder.setContentInfo(info)

            if (progress != 0.0) {
                progress /= 1000
            }
            max /= 1000
            notificationBuilder.setProgress(max.toInt(), progress.toInt(), false)

            if (System.currentTimeMillis() - lastReportedNotificationTime > MIN_TIME_BETWEEN_UPDATE_REPORTS) {
                notificationManager.notify(NotificationId.DOWNLOADING.value, notificationBuilder.build())
                lastReportedNotificationTime = System.currentTimeMillis()
            }
        }
    }

    override fun getNotificationBuilder(): NotificationCompat.Builder {
        var notificationBuilder = this.notificationBuilder
        if (notificationBuilder == null) {
            val color = ContextCompat.getColor(context, R.color.notification_color)

            notificationBuilder = notificationHelper.downloadChannelBuilder()
                .setSmallIcon(IR.drawable.notification_download)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setColor(color)
                .setOngoing(true)
                .setContentIntent(openDownloadingPageIntent())

            this.notificationBuilder = notificationBuilder

            return notificationBuilder
        }

        return notificationBuilder
    }

    private fun truncateString(value: String?, length: Int): String {
        if (value == null) {
            return ""
        }
        return if (value.length <= length) {
            value
        } else value.substring(0, length - 1) + ".."
    }

    private fun openDownloadingPageIntent(): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.action = Settings.INTENT_OPEN_APP_DOWNLOADING
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT.or(PendingIntent.FLAG_IMMUTABLE))
    }

    internal data class DownloadingInfo(val episodeUUID: String, val jobId: UUID)
}
