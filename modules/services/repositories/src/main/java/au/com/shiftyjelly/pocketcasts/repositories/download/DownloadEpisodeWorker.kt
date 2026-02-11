package au.com.shiftyjelly.pocketcasts.repositories.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.system.ErrnoException
import android.system.OsConstants
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadNotificationObserver.NotificationJob
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.utils.extensions.anyMessageContains
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloader.Result as DownloadResult

/**
 * **Note**: This `Worker` is intentionally implemented as a synchronous `Worker`
 * and must remain so.
 *
 * `WorkManager` invokes Worker.doWork() on a background thread provided by
 * its executor, and the method is expected to run synchronously until the
 * work completes. This worker relies on that contract to ensure that these requirements are met.
 *
 * Bounded concurrency: Episode downloads are limited to the fixed number
 * of `Worker` threads configured for `WorkManager` in the `Application` class.
 * By blocking within `doWork()`, we guarantee that no more than the
 * configured number of downloads run concurrently.
 *
 * Correct notification behavior: If this were implemented as a
 * `CoroutineWorker`, the suspending nature of `doWork()` could allow
 * additional workers to start while earlier work is still in flight.
 * This can lead to multiple episode notifications being triggered
 * concurrently and to perception of too many downloads at the same time.
 *
 * Lifecycle alignment: All download execution, including URL refresh,
 * file resolution, and EpisodeDownloader execution, completes within the
 * lifetime of `doWork()`. No coroutine jobs or asynchronous work may
 * outlive `doWork()`, ensuring that `Result.success()` or `Result.failure()`
 * accurately reflects the completion of the episode download and its
 * associated notification lifecycle.
 *
 * In a coroutine-heavy codebase, the use of `runBlocking` here is
 * deliberate: it confines execution to WorkManager’s executor and
 * preserves the application’s configured concurrency and notification
 * guarantees.
 */
@HiltWorker
class DownloadEpisodeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    @Downloads httpClient: Lazy<OkHttpClient>,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val progressCache: DownloadProgressCache,
    private val notificationObserver: DownloadNotificationObserver,
) : Worker(context, params) {
    private val args = inputData.toArgs()
    private val downloader = EpisodeDownloader(httpClient, progressCache)
    private var notificationJob: NotificationJob? = null

    override fun doWork(): Result {
        val result = try {
            dispatchWorkStarted()
            var episode = getEpisodeOrThrow()
            prepareForegroundNotification(episode)

            episode = refreshDownloadUrl(episode)
            val downloadFile = getDownloadFileOrThrow(episode)
            val tempFile = File(DownloadHelper.tempPathForEpisode(episode, fileStorage))

            val downloadResult = downloader.download(
                episode = episode,
                downloadFile = downloadFile,
                tempFile = tempFile,
            )
            downloadResult
        } catch (e: Throwable) {
            DownloadResult.ExceptionFailure(e)
        }
        notificationJob?.cancel()

        return when (result) {
            is DownloadResult.Success -> {
                val data = Data.Builder()
                    .putString(DOWNLOAD_FILE_PATH_KEY, result.file.path)
                    .build()
                Result.success(data)
            }

            is DownloadResult.Failure -> {
                val (message, shouldRetry) = processFailure(result)
                if (shouldRetry && runAttemptCount < MAX_RETRY_ATTEMPTS - 1) {
                    Result.retry()
                } else {
                    val data = Data.Builder()
                        .putString(ERROR_MESSAGE_KEY, message)
                        .build()
                    Result.failure(data)
                }
            }
        }
    }

    override fun onStopped() {
        notificationJob?.cancel()
    }

    private fun dispatchWorkStarted() {
        setProgressAsync(Data.Builder().putBoolean(IS_WORK_STARTED_KEY, true).build()).get()
    }

    private fun getEpisodeOrThrow() = runBlocking {
        requireNotNull(episodeManager.findEpisodeByUuid(args.episodeUuid)) {
            context.getString(LR.string.error_download_missing_episode)
        }
    }

    private fun refreshDownloadUrl(episode: BaseEpisode) = runBlocking<BaseEpisode> {
        when (episode) {
            is PodcastEpisode -> {
                val freshDownloadUrl = episodeManager.updateDownloadUrl(episode)
                episode.copy(downloadUrl = freshDownloadUrl)
            }

            is UserEpisode -> {
                episode
            }
        }
    }

    private fun getDownloadFileOrThrow(episode: BaseEpisode): File {
        val file = DownloadHelper.pathForEpisode(episode, fileStorage)?.let(::File)
        return requireNotNull(file) {
            context.getString(LR.string.error_download_no_episode_file)
        }
    }

    private fun processFailure(result: DownloadResult.Failure): Pair<String, Boolean> {
        return when (result) {
            is DownloadResult.InvalidDownloadUrl -> {
                context.getString(LR.string.error_download_invalid_url) to false
            }

            is DownloadResult.UnsuccessfulHttpCall -> {
                context.getString(LR.string.error_download_http_failure, result.code) to true
            }

            is DownloadResult.ExceptionFailure -> {
                val throwable = result.throwable
                // Order of checks here is important. Wrong order will result in mapping to wrong messages or states.
                // For example SocketTimeoutException inherits from InterruptedIOException. Checking for isCancelled
                // and consequently InterruptedIOException would result in mapping timeouts to cancellations.
                when {
                    throwable.isOutOfStorage() -> {
                        context.getString(LR.string.error_download_no_storage) to false
                    }

                    throwable.isChartableBlocked() -> {
                        context.getString(LR.string.error_download_chartable) to false
                    }

                    throwable.isAnyCause<UnknownHostException>() -> {
                        context.getString(LR.string.error_download_unknown_host) to true
                    }

                    throwable.isAnyCause<ConnectException>() -> {
                        context.getString(LR.string.error_download_connection_error) to true
                    }

                    throwable.isAnyCause<SocketTimeoutException>() -> {
                        context.getString(LR.string.error_download_socket_timeout) to true
                    }

                    throwable.isAnyCause<SSLException>() -> {
                        context.getString(LR.string.error_download_ssl_failure) to true
                    }

                    throwable.isCancelled() -> {
                        CANCELLED_MESSAGE to false
                    }

                    throwable is IOException -> {
                        context.getString(LR.string.error_download_io_failure) to true
                    }

                    else -> {
                        val message = context.getString(LR.string.error_download_generic_failure, throwable.message.orEmpty())
                        message.trim() to true
                    }
                }
            }
        }
    }

    private fun Throwable.isOutOfStorage(): Boolean {
        var throwable: Throwable? = this
        while (throwable != null) {
            when (throwable) {
                is ErrnoException -> {
                    if (throwable.errno == OsConstants.ENOSPC || throwable.errno == OsConstants.EDQUOT) {
                        return true
                    }
                }

                is IOException -> {
                    val message = throwable.message?.lowercase() ?: ""
                    if (OUT_OF_STORAGE_MESSAGES.any { message.contains(it) }) {
                        return true
                    }
                }
            }
            throwable = throwable.cause
        }
        return false
    }

    private fun Throwable.isChartableBlocked(): Boolean {
        return anyMessageContains("chtbl.com")
    }

    private fun Throwable.isCancelled(): Boolean {
        var throwable: Throwable? = this
        while (throwable != null) {
            when (throwable) {
                is InterruptedIOException, is InterruptedException, is CancellationException -> {
                    return true
                }

                is IOException -> {
                    if (throwable.message?.lowercase() == "cancelled") {
                        return true
                    }
                }
            }
            throwable = throwable.cause
        }
        return false
    }

    private inline fun <reified T : Throwable> Throwable.isAnyCause(): Boolean {
        var throwable: Throwable? = this
        while (throwable != null) {
            if (throwable is T) {
                return true
            }
            throwable = throwable.cause
        }
        return false
    }

    private fun prepareForegroundNotification(episode: BaseEpisode) {
        val notificationJob = notificationObserver.observeNotificationUpdates(episode)
        this.notificationJob = notificationJob
        val foregroundInfo = createForegroundInfo(notificationJob.id, notificationJob.notification)
        setForegroundAsync(foregroundInfo).get()
    }

    private fun createForegroundInfo(id: Int, notification: Notification): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    data class Args(
        val episodeUuid: String,
        val waitForWifi: Boolean,
        val waitForPower: Boolean,
    )

    companion object {
        const val WORKER_TAG = "EpisodeDownloadWorker"

        internal const val WORKER_EPISODE_TAG_PREFIX = "$WORKER_TAG:"

        fun episodeWorkerName(episodeUuid: String) = "$WORKER_EPISODE_TAG_PREFIX$episodeUuid"

        fun createWorkRequest(args: Args): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresCharging(args.waitForPower)
                .setRequiredNetworkType(if (args.waitForWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            return OneTimeWorkRequestBuilder<DownloadEpisodeWorker>()
                .setConstraints(constraints)
                .setInputData(args.toData())
                .addTag(WORKER_TAG)
                .addTag(episodeWorkerName(args.episodeUuid))
                .addTag(args.episodeUuid)
                .build()
        }

        fun mapToDownloadWorkInfo(info: WorkInfo): DownloadWorkInfo? {
            return info.tags
                .firstOrNull { tag -> tag.startsWith(WORKER_EPISODE_TAG_PREFIX) }
                ?.substringAfter(WORKER_EPISODE_TAG_PREFIX, missingDelimiterValue = "")
                ?.takeIf(String::isNotEmpty)
                ?.let { episodeUuid ->
                    val isWifiRequired = info.constraints.requiredNetworkType == NetworkType.UNMETERED
                    val isPowerRequired = info.constraints.requiresCharging()
                    val isStorageRequired = info.constraints.requiresStorageNotLow()
                    when (info.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                            DownloadWorkInfo.Pending(
                                episodeUuid = episodeUuid,
                                retryAttemptCount = info.runAttemptCount,
                                isWifiRequired = isWifiRequired,
                                isPowerRequired = isPowerRequired,
                                isStorageRequired = isStorageRequired,
                            )
                        }

                        WorkInfo.State.RUNNING -> {
                            // Running doesn't necessarily mean that Worker.doWork() was called.
                            // For this reason we use a separate flag.
                            if (info.progress.getBoolean(IS_WORK_STARTED_KEY, false)) {
                                DownloadWorkInfo.InProgress(episodeUuid)
                            } else {
                                DownloadWorkInfo.Pending(
                                    episodeUuid = episodeUuid,
                                    retryAttemptCount = info.runAttemptCount,
                                    isWifiRequired = isWifiRequired,
                                    isPowerRequired = isPowerRequired,
                                    isStorageRequired = isStorageRequired,
                                )
                            }
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            val filePath = requireNotNull(info.outputData.getString(DOWNLOAD_FILE_PATH_KEY)) {
                                "Output file path is missing for the episode $episodeUuid download"
                            }
                            DownloadWorkInfo.Success(episodeUuid, File(filePath))
                        }

                        WorkInfo.State.FAILED -> {
                            val errorMessage = info.outputData.getString(ERROR_MESSAGE_KEY)
                            if (errorMessage == CANCELLED_MESSAGE) {
                                DownloadWorkInfo.Cancelled(episodeUuid)
                            } else {
                                DownloadWorkInfo.Failure(episodeUuid, errorMessage)
                            }
                        }

                        WorkInfo.State.CANCELLED -> {
                            DownloadWorkInfo.Cancelled(episodeUuid)
                        }
                    }
                }
        }
    }
}

sealed interface DownloadWorkInfo {
    val episodeUuid: String

    data class Pending(
        override val episodeUuid: String,
        val retryAttemptCount: Int,
        val isWifiRequired: Boolean,
        val isPowerRequired: Boolean,
        val isStorageRequired: Boolean,
    ) : DownloadWorkInfo

    data class InProgress(
        override val episodeUuid: String,
    ) : DownloadWorkInfo

    data class Success(
        override val episodeUuid: String,
        val downloadFile: File,
    ) : DownloadWorkInfo

    data class Failure(
        override val episodeUuid: String,
        val errorMessage: String?,
    ) : DownloadWorkInfo

    data class Cancelled(
        override val episodeUuid: String,
    ) : DownloadWorkInfo
}

private fun Data.toArgs() = DownloadEpisodeWorker.Args(
    episodeUuid = requireNotNull(getString(EPISODE_UUID_KEY)),
    waitForWifi = getBoolean(WAIT_FOR_WIFI_KEY, true),
    waitForPower = getBoolean(WAIT_FOR_POWER_KEY, true),
)

private fun DownloadEpisodeWorker.Args.toData() = Data.Builder()
    .putString(EPISODE_UUID_KEY, episodeUuid)
    .putBoolean(WAIT_FOR_WIFI_KEY, waitForWifi)
    .putBoolean(WAIT_FOR_POWER_KEY, waitForPower)
    .build()

// Input keys
private const val EPISODE_UUID_KEY = "episode_uuid"
private const val WAIT_FOR_WIFI_KEY = "wait_for_wifi"
private const val WAIT_FOR_POWER_KEY = "wait_for_power"

// Progress keys
internal const val IS_WORK_STARTED_KEY = "is_work_started"

// Output keys
internal const val DOWNLOAD_FILE_PATH_KEY = "download_file_path"
internal const val ERROR_MESSAGE_KEY = "error_message"

private val OUT_OF_STORAGE_MESSAGES = setOf("no space", "not enough space", "disk full", "quota")
internal const val CANCELLED_MESSAGE = "___EPISODE_DOWNLOAD_CANCELLED___"

private const val MAX_RETRY_ATTEMPTS = 3
