package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UploadEpisodeTask
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.FileAccount
import au.com.shiftyjelly.pocketcasts.servers.sync.FilePost
import au.com.shiftyjelly.pocketcasts.servers.sync.FileUploadData
import au.com.shiftyjelly.pocketcasts.servers.sync.ServerFile
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

interface UserEpisodeManager {
    suspend fun add(episode: UserEpisode, playbackManager: PlaybackManager)
    suspend fun addAll(episodes: List<UserEpisode>)
    suspend fun update(episode: UserEpisode)
    suspend fun delete(episode: UserEpisode, playbackManager: PlaybackManager)
    suspend fun deleteAll(episodes: List<UserEpisode>, playbackManager: PlaybackManager)
    fun observeUserEpisodes(): Flowable<List<UserEpisode>>
    suspend fun findUserEpisodes(): List<UserEpisode>
    fun observeEpisodeRx(uuid: String): Flowable<UserEpisode>
    fun observeEpisode(uuid: String): Flow<UserEpisode>
    fun findEpisodeByUuidRx(uuid: String): Maybe<UserEpisode>
    suspend fun findEpisodeByUuid(uuid: String): UserEpisode?
    fun uploadToServer(userEpisode: UserEpisode, waitForWifi: Boolean)
    fun performUploadToServer(userEpisode: UserEpisode, playbackManager: PlaybackManager): Completable
    fun removeFromCloud(userEpisode: UserEpisode)
    fun cancelUpload(userEpisode: UserEpisode)
    suspend fun syncFiles(playbackManager: PlaybackManager)
    fun getPlaybackUrl(userEpisode: UserEpisode): Single<String>
    fun observeDownloadUserEpisodes(): Flowable<List<UserEpisode>>
    suspend fun updateDownloadedFilePath(episode: UserEpisode, filePath: String)
    suspend fun updateFileType(episode: UserEpisode, fileType: String)
    suspend fun updateSizeInBytes(episode: UserEpisode, sizeInBytes: Long)
    suspend fun updateLastDownloadDate(episode: UserEpisode, date: Date)
    suspend fun updateEpisodeStatus(episode: UserEpisode, status: EpisodeStatusEnum)
    suspend fun updateDownloadErrorDetails(episode: UserEpisode, errorDetails: String?)
    suspend fun updateDownloadTaskId(episode: UserEpisode, taskId: String?)
    fun observeAccountUsage(): Flowable<Optional<FileAccount>>
    fun observeUserEpisodesSorted(sortOrder: Settings.CloudSortOrder): Flowable<List<UserEpisode>>
    suspend fun deletePlayedEpisodeIfReq(episode: UserEpisode, playbackManager: PlaybackManager)
    fun autoUploadToCloudIfReq(episode: UserEpisode)
    fun downloadMissingUserEpisode(uuid: String, placeholderTitle: String?, placeholderPublished: Date?): Maybe<UserEpisode>
    fun syncFilesInBackground(playbackManager: PlaybackManager)
    fun uploadImageToServer(userEpisode: UserEpisode, imageFile: File): Completable
    suspend fun updateFiles(files: List<UserEpisode>)
    suspend fun deleteImageFromServer(userEpisode: UserEpisode)
    suspend fun updateServerStatus(userEpisode: UserEpisode, serverStatus: UserEpisodeServerStatus)
    fun monitorUploads(context: Context)
    suspend fun removeCloudStatusFromFiles(playbackManager: PlaybackManager)
    suspend fun markAllAsPlayed(episodes: List<UserEpisode>, playbackManager: PlaybackManager)
    suspend fun markAllAsUnplayed(episodes: List<UserEpisode>)
}

object UploadProgressManager {
    internal val uploadObservers = mutableMapOf<String, MutableList<Consumer<Float>>>()

    fun observeUploadProgress(uuid: String, consumer: Consumer<Float>) {
        if (!uploadObservers.containsKey(uuid)) {
            uploadObservers[uuid] = mutableListOf()
        }
        uploadObservers[uuid]?.add(consumer)
    }

    fun stopObservingUpload(uuid: String, consumer: Consumer<Float>) {
        uploadObservers[uuid]?.remove(consumer)
    }
}

private const val WORK_MANAGER_UPLOAD_TASK = "uploadTask"

class UserEpisodeManagerImpl @Inject constructor(
    appDatabase: AppDatabase,
    val syncManager: SyncManager,
    val settings: Settings,
    val subscriptionManager: SubscriptionManager,
    val downloadManager: DownloadManager,
    @ApplicationContext val context: Context,
    val episodeAnalytics: EpisodeAnalytics
) : UserEpisodeManager, CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val usageRelay = BehaviorRelay.create<Optional<FileAccount>>()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

    override fun monitorUploads(context: Context) {
        WorkManager.getInstance(context).getWorkInfosByTagLiveData(WORK_MANAGER_UPLOAD_TASK)
            .observeForever { tasks ->
                launch {
                    tasks.forEach { task ->
                        userEpisodeDao.findByUploadTaskId(task.id.toString())?.let { userEpisode ->
                            when (task.state) {
                                WorkInfo.State.BLOCKED -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.WAITING_FOR_WIFI)
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.UPLOADED)
                                    userEpisodeDao.updateUploadTaskId(userEpisode.uuid, null)
                                }
                                WorkInfo.State.FAILED -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.LOCAL)
                                    userEpisodeDao.updateUploadError(userEpisode.uuid, task.outputData.getString(UploadEpisodeTask.OUTPUT_ERROR_MESSAGE))
                                    userEpisodeDao.updateUploadTaskId(userEpisode.uuid, null)
                                }
                                WorkInfo.State.RUNNING -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.UPLOADING)
                                }
                                WorkInfo.State.CANCELLED -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.LOCAL)
                                    userEpisodeDao.updateUploadTaskId(userEpisode.uuid, null)
                                }
                                WorkInfo.State.ENQUEUED -> {
                                    userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.QUEUED)
                                }
                            }
                        }
                    }
                }
            }
    }

    override suspend fun add(episode: UserEpisode, playbackManager: PlaybackManager) {
        userEpisodeDao.insert(episode)

        if (settings.getCloudAddToUpNext()) {
            playbackManager.playLast(episode = episode, source = AnalyticsSource.FILES)
        }
    }

    override suspend fun addAll(episodes: List<UserEpisode>) {
        userEpisodeDao.insertAll(episodes)
    }

    override suspend fun update(episode: UserEpisode) {
        userEpisodeDao.update(episode)
    }

    override suspend fun delete(episode: UserEpisode, playbackManager: PlaybackManager) {
        deleteFilesForEpisode(episode)
        playbackManager.removeEpisode(episodeToRemove = episode, source = AnalyticsSource.FILES, userInitiated = false)
        cancelUpload(episode)
        userEpisodeDao.delete(episode)
    }

    private fun deleteFilesForEpisode(episode: UserEpisode) {
        FileUtil.deleteFileByPath(episode.downloadedFilePath)
        if (episode.artworkUrl?.startsWith("/") == true) {
            FileUtil.deleteFileByPath(episode.artworkUrl)
        }
    }

    override suspend fun deleteAll(episodes: List<UserEpisode>, playbackManager: PlaybackManager) {
        episodes.forEach {
            playbackManager.removeEpisode(episodeToRemove = it, source = AnalyticsSource.FILES, userInitiated = false)
        }
        userEpisodeDao.deleteAll(episodes)
    }

    override fun observeUserEpisodes(): Flowable<List<UserEpisode>> {
        return userEpisodeDao.observeUserEpisodesDesc()
    }

    override suspend fun findUserEpisodes(): List<UserEpisode> {
        return userEpisodeDao.findUserEpisodesDesc()
    }

    override fun observeUserEpisodesSorted(sortOrder: Settings.CloudSortOrder): Flowable<List<UserEpisode>> {
        return when (sortOrder) {
            Settings.CloudSortOrder.NEWEST_OLDEST -> userEpisodeDao.observeUserEpisodesDesc()
            Settings.CloudSortOrder.OLDEST_NEWEST -> userEpisodeDao.observeUserEpisodesAsc()
            Settings.CloudSortOrder.A_TO_Z -> userEpisodeDao.observeUserEpisodesTitleAsc()
            Settings.CloudSortOrder.Z_TO_A -> userEpisodeDao.observeUserEpisodesTitleDesc()
            Settings.CloudSortOrder.SHORT_LONG -> userEpisodeDao.observeUserEpisodesDurationAsc()
            Settings.CloudSortOrder.LONG_SHORT -> userEpisodeDao.observeUserEpisodesDurationDesc()
        }.map { it.filterNot { it.serverStatus == UserEpisodeServerStatus.MISSING } }
    }

    override fun observeDownloadUserEpisodes(): Flowable<List<UserEpisode>> {
        return userEpisodeDao.observeDownloadingUserEpisodes()
    }

    override fun observeEpisodeRx(uuid: String): Flowable<UserEpisode> {
        return userEpisodeDao.observeEpisodeRx(uuid)
    }

    override fun observeEpisode(uuid: String): Flow<UserEpisode> {
        return userEpisodeDao.observeEpisode(uuid)
    }

    override fun findEpisodeByUuidRx(uuid: String): Maybe<UserEpisode> {
        return userEpisodeDao.findEpisodeByUuidRx(uuid)
    }

    override suspend fun findEpisodeByUuid(uuid: String): UserEpisode? {
        return userEpisodeDao.findEpisodeByUuid(uuid)
    }

    override fun downloadMissingUserEpisode(uuid: String, placeholderTitle: String?, placeholderPublished: Date?): Maybe<UserEpisode> {
        val missingEpisode = UserEpisode(uuid = uuid, title = placeholderTitle ?: "Unable to find episode", publishedDate = placeholderPublished ?: Date(), serverStatus = UserEpisodeServerStatus.MISSING)
        val replaceEpisodeWithSubstitute = userEpisodeDao.insertRx(missingEpisode).andThen(userEpisodeDao.findEpisodeByUuidRx(uuid))

        val downloadMissingEpisode = syncManager.getUserEpisode(uuid)
            .flatMap {
                userEpisodeDao.insertRx(it.toUserEpisode()).andThen(userEpisodeDao.findEpisodeByUuidRx(uuid))
            }.switchIfEmpty(replaceEpisodeWithSubstitute)

        return userEpisodeDao.findEpisodeByUuidRx(uuid)
            .flatMap {
                if (it.serverStatus == UserEpisodeServerStatus.MISSING) {
                    Maybe.empty() // We want to attempt to redownload missing files so we mark as not found
                } else {
                    Maybe.just(it)
                }
            }
            .switchIfEmpty(downloadMissingEpisode)
    }

    override fun syncFilesInBackground(playbackManager: PlaybackManager) {
        launch {
            try {
                syncFiles(playbackManager)
            } catch (e: Exception) {
                Timber.e("Could not sync cloud files: ${e.message}")
            }
        }
    }

    override suspend fun updateFiles(files: List<UserEpisode>) = withContext(Dispatchers.IO) {
        val response = syncManager.postFiles(files.toServerPost()).blockingGet()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    override suspend fun syncFiles(playbackManager: PlaybackManager) {
        val episodesToSync = userEpisodeDao.findUserEpisodesToSync()
        if (episodesToSync.isNotEmpty()) {
            val response = withContext(Dispatchers.IO) {
                syncManager.postFiles(episodesToSync.toServerPost()).blockingGet()
            }
            if (response.isSuccessful) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Synced cloud files successfully")
                userEpisodeDao.markAllSynced()
            } else {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Couldn't sync cloud files")
                throw HttpException(response)
            }
        }

        val response = withContext(Dispatchers.IO) {
            syncManager.getFiles().blockingGet()
        }
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        if (response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            // Do nothing
            return
        }
        val responseBody = response.body() ?: return

        usageRelay.accept(Optional.of(responseBody.account))

        val playingEpisodeUUID = (playbackManager.getCurrentEpisode() as? UserEpisode)?.uuid
        val existingFiles = userEpisodeDao.findAllUuids().toMutableList()
        responseBody.files.forEach {
            val existingFile = findEpisodeByUuid(it.uuid)
            if (existingFile != null) {
                var didChange = false
                var playingEpisodePlayedUpToChanged = false

                existingFiles.remove(existingFile.uuid)

                if (existingFile.title != it.title) {
                    existingFile.title = it.title
                    didChange = true
                }

                if (existingFile.duration != it.duration.toDouble() && it.duration > 0) {
                    existingFile.duration = it.duration.toDouble()
                    didChange = true
                }

                if (existingFile.sizeInBytes != it.size) {
                    existingFile.sizeInBytes = it.size
                    didChange = true
                }

                if (existingFile.hasCustomImage != it.hasCustomImage) {
                    existingFile.hasCustomImage = it.hasCustomImage
                    didChange = true
                }

                if (existingFile.tintColorIndex != it.colour) {
                    existingFile.tintColorIndex = it.colour
                    didChange = true
                }

                if (existingFile.artworkUrl != it.imageUrl) {
                    existingFile.artworkUrl = it.imageUrl
                    didChange = true
                }

                if (existingFile.playedUpTo != it.playedUpTo.toDouble()) {
                    existingFile.playedUpTo = it.playedUpTo.toDouble()
                    existingFile.playedUpToModified = null
                    didChange = true

                    if (playingEpisodeUUID == existingFile.uuid) {
                        // This is the currently loaded episode, we need to seek the player
                        playingEpisodePlayedUpToChanged = true
                    }
                }

                if (it.playingStatus != existingFile.playingStatus) {
                    existingFile.playingStatus = it.playingStatus
                    existingFile.playingStatusModified = null
                    didChange = true
                }

                if (existingFile.serverStatus != UserEpisodeServerStatus.UPLOADED) {
                    existingFile.serverStatus = UserEpisodeServerStatus.UPLOADED
                    didChange = true
                }

                if (didChange) {
                    userEpisodeDao.update(existingFile)

                    if (playingEpisodePlayedUpToChanged && !playbackManager.isPlaying()) {
                        playbackManager.seekIfPlayingToTimeMs(existingFile.uuid, (it.playedUpTo * 1000))
                    }
                }
            } else {
                val newEpisode = it.toUserEpisode()
                add(newEpisode, playbackManager)

                if (settings.getCloudAutoDownload() && subscriptionManager.getCachedStatus() is SubscriptionStatus.Paid) {
                    userEpisodeDao.updateAutoDownloadStatus(PodcastEpisode.AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED, newEpisode.uuid)
                    newEpisode.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_AUTO_DOWNLOADED
                    downloadManager.addEpisodeToQueue(newEpisode, "cloud files sync", false)
                }
            }
        }

        // Clean up any files that have been deleted on the server
        existingFiles.forEach {
            val episode = findEpisodeByUuid(it)
            if (episode != null) {
                if (!episode.isDownloaded) {
                    // File deleted from server
                    playbackManager.removeEpisode(episode, source = AnalyticsSource.UNKNOWN, userInitiated = false)
                    userEpisodeDao.delete(episode)
                } else {
                    if (episode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                        userEpisodeDao.updateServerStatus(episode.uuid, UserEpisodeServerStatus.LOCAL)
                    }
                }
            }
        }
    }

    override fun uploadToServer(userEpisode: UserEpisode, waitForWifi: Boolean) {
        val networkType = if (waitForWifi) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val inputData = Data.Builder()
            .putString(UploadEpisodeTask.INPUT_EPISODE_UUID, userEpisode.uuid)
            .build()

        val task = OneTimeWorkRequestBuilder<UploadEpisodeTask>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(WORK_MANAGER_UPLOAD_TASK)
            .build()

        WorkManager.getInstance(context).enqueue(task)

        launch {
            if (waitForWifi) {
                userEpisodeDao.updateServerStatus(userEpisode.uuid, UserEpisodeServerStatus.WAITING_FOR_WIFI)
            }

            userEpisodeDao.updateUploadTaskId(userEpisode.uuid, task.id.toString())
        }
    }

    override fun performUploadToServer(userEpisode: UserEpisode, playbackManager: PlaybackManager): Completable {
        Timber.d("Starting upload of ${userEpisode.uuid}")
        val artworkUrl = userEpisode.artworkUrl
        val imageFile = if (artworkUrl != null && userEpisode.artworkUrl?.startsWith("/") == true) File(artworkUrl) else null
        val imageUploadTask: Completable
        if (imageFile != null) {
            imageUploadTask = uploadImageToServer(userEpisode, imageFile)
        } else {
            imageUploadTask = Completable.complete()
        }

        return userEpisodeDao.updateServerStatusRx(userEpisode.uuid, UserEpisodeServerStatus.UPLOADING)
            .andThen(userEpisodeDao.updateUploadErrorRx(userEpisode.uuid, null))
            .andThen(syncManager.uploadFileToServer(userEpisode))
            .andThen(
                userEpisodeDao.updateServerStatusRx(userEpisode.uuid, serverStatus = UserEpisodeServerStatus.UPLOADED)
            )
            .andThen(imageUploadTask)
            // let the file upload report to upload to the api server
            .delay(1, TimeUnit.SECONDS)
            // the api server will call S3 to check the file exists if it doesn't know
            .andThen(
                syncManager.getFileUploadStatus(userEpisode.uuid)
                    .onErrorReturn {
                        Timber.e(it)
                        false
                    }
                    .flatMapCompletable { success ->
                        if (success) {
                            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UPLOAD_FINISHED, uuid = userEpisode.uuid)
                            userEpisodeDao.updateServerStatusRx(userEpisode.uuid, serverStatus = UserEpisodeServerStatus.UPLOADED)
                        } else {
                            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UPLOAD_FAILED, uuid = userEpisode.uuid)
                            userEpisodeDao.updateUploadErrorRx(userEpisode.uuid, "Upload failed")
                        }
                    }
            )
            .andThen(
                rxCompletable { syncFiles(playbackManager) }
                    .doOnError { Timber.e(it) }
                    .onErrorComplete()
            )
    }

    override fun uploadImageToServer(userEpisode: UserEpisode, imageFile: File): Completable =
        syncManager.uploadImageToServer(userEpisode, imageFile)

    override fun cancelUpload(userEpisode: UserEpisode) {
        if (userEpisode.uploadTaskId == null) return
        WorkManager.getInstance(context).cancelWorkById(UUID.fromString(userEpisode.uploadTaskId))
        removeFromCloud(userEpisode) // Cancels upload request on server
    }

    override fun removeFromCloud(userEpisode: UserEpisode) {
        syncManager.deleteFromServer(userEpisode)
            .flatMapCompletable { userEpisodeDao.updateServerStatusRx(userEpisode.uuid, UserEpisodeServerStatus.LOCAL) }
            .andThen(syncManager.getFileUsage().doOnSuccess { usageRelay.accept(Optional.of(it)) }.ignoreElement())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = {
                    LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, it, "Could not upload file ${userEpisode.uuid} - ${userEpisode.title}")
                }
            )
    }

    override suspend fun deleteImageFromServer(userEpisode: UserEpisode) = withContext(Dispatchers.IO) {
        syncManager.deleteImageFromServer(userEpisode).await()
        userEpisode.hasCustomImage = false
        userEpisode.artworkUrl = null
        update(userEpisode)
    }

    override fun getPlaybackUrl(userEpisode: UserEpisode): Single<String> {
        return syncManager.getPlaybackUrl(userEpisode)
    }

    override suspend fun updateEpisodeStatus(episode: UserEpisode, status: EpisodeStatusEnum) {
        userEpisodeDao.updateEpisodeStatus(episode.uuid, status)
    }

    override suspend fun updateDownloadedFilePath(episode: UserEpisode, filePath: String) {
        userEpisodeDao.updateDownloadedFilePath(episode.uuid, filePath)
    }

    override suspend fun updateFileType(episode: UserEpisode, fileType: String) {
        userEpisodeDao.updateFileType(episode.uuid, fileType)
    }

    override suspend fun updateSizeInBytes(episode: UserEpisode, sizeInBytes: Long) {
        userEpisodeDao.updateSizeInBytes(episode.uuid, sizeInBytes)
    }

    override suspend fun updateLastDownloadDate(episode: UserEpisode, date: Date) {
        userEpisodeDao.updateDownloadedAttemptDate(episode.uuid, date)
    }

    override suspend fun updateDownloadErrorDetails(episode: UserEpisode, errorDetails: String?) {
        userEpisodeDao.updateDownloadError(episode.uuid, errorDetails)
    }

    override suspend fun updateDownloadTaskId(episode: UserEpisode, taskId: String?) {
        userEpisodeDao.updateDownloadTaskId(episode.uuid, taskId)
    }

    override fun observeAccountUsage(): Flowable<Optional<FileAccount>> {
        return usageRelay.toFlowable(BackpressureStrategy.LATEST)
    }

    override suspend fun updateServerStatus(userEpisode: UserEpisode, serverStatus: UserEpisodeServerStatus) {
        userEpisodeDao.updateServerStatusRx(userEpisode.uuid, serverStatus)
    }

    override suspend fun deletePlayedEpisodeIfReq(episode: UserEpisode, playbackManager: PlaybackManager) {
        if (settings.getDeleteLocalFileAfterPlaying()) {
            deleteFilesForEpisode(episode)
            userEpisodeDao.updateEpisodeStatus(episode.uuid, EpisodeStatusEnum.NOT_DOWNLOADED)

            if (episode.serverStatus == UserEpisodeServerStatus.LOCAL) {
                userEpisodeDao.delete(episode)
            }
        }

        if (settings.getDeleteCloudFileAfterPlaying() && episode.serverStatus == UserEpisodeServerStatus.UPLOADED) {
            removeFromCloud(episode)
            if (!episode.isDownloaded) {
                delete(episode, playbackManager)
            }
        }
    }

    override fun autoUploadToCloudIfReq(episode: UserEpisode) {
        if (settings.getCloudAutoUpload() && subscriptionManager.getCachedStatus() is SubscriptionStatus.Paid) {
            uploadToServer(episode, settings.getCloudOnlyWifi())
        }
    }

    override suspend fun removeCloudStatusFromFiles(playbackManager: PlaybackManager) = withContext(Dispatchers.IO) {
        userEpisodeDao.observeUserEpisodesDesc().firstElement().awaitSingleOrNull()?.forEach {
            if (!it.isDownloaded) { // Cloud only
                delete(it, playbackManager)
            } else if (it.isDownloaded && it.serverStatus == UserEpisodeServerStatus.UPLOADED) {
                userEpisodeDao.updateServerStatus(it.uuid, UserEpisodeServerStatus.LOCAL)
            }
        }

        // Cancel all uploads
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_MANAGER_UPLOAD_TASK)

        // Clear usage
        usageRelay.accept(Optional.empty())

        return@withContext // Need this to satisfy the type for implicit return (which is dumb)
    }

    override suspend fun markAllAsPlayed(episodes: List<UserEpisode>, playbackManager: PlaybackManager) {
        episodes.map { it.uuid }.chunked(500).forEach { userEpisodeDao.updateAllPlayingStatus(it, System.currentTimeMillis(), EpisodePlayingStatus.COMPLETED) }
        episodes.forEach { playbackManager.removeEpisode(it, source = AnalyticsSource.UNKNOWN, userInitiated = false) }
    }

    override suspend fun markAllAsUnplayed(episodes: List<UserEpisode>) {
        episodes.map { it.uuid }.chunked(500).forEach { userEpisodeDao.markAllUnplayed(it, System.currentTimeMillis()) }
    }
}

fun UserEpisode.toServerPostFile(): FilePost {
    return FilePost(
        uuid = this.uuid,
        title = this.title,
        colour = this.tintColorIndex,
        playedUpTo = this.playedUpTo.roundToInt(),
        playingStatus = this.playingStatus.ordinal + 1,
        duration = this.duration.roundToInt(),
        hasCustomImage = this.hasCustomImage
    )
}

private fun List<UserEpisode>.toServerPost(): List<FilePost> = this.map { it.toServerPostFile() }

fun UserEpisode.toUploadData(): FileUploadData {
    return FileUploadData(
        uuid = this.uuid,
        title = this.title,
        colour = this.tintColorIndex,
        duration = this.duration.roundToInt(),
        contentType = "${fileType ?: "audio/mp3"}"
    )
}

fun ServerFile.toUserEpisode(): UserEpisode {
    return UserEpisode(
        uuid = this.uuid,
        fileType = this.contentType,
        duration = this.duration.toDouble(),
        artworkUrl = this.imageUrl,
        playedUpTo = this.playedUpTo.toDouble(),
        playedUpToModified = this.playedUpToModified,
        playingStatus = this.playingStatus,
        playingStatusModified = this.playingStatusModified,
        publishedDate = this.publishedDate,
        sizeInBytes = this.size,
        title = this.title,
        serverStatus = UserEpisodeServerStatus.UPLOADED,
        hasCustomImage = this.hasCustomImage,
        tintColorIndex = this.colour,
        addedDate = this.publishedDate
    )
}
