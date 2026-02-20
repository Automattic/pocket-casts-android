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
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeDownloadError
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadEpisodeWorker.Companion.WORKER_TAG
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadNotificationObserver.NotificationJob
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageException
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.extensions.anyMessageContains
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.util.concurrent.ListenableFuture
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.net.ssl.SSLException
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.await
import okhttp3.Call
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
    @Downloads httpClient: Lazy<Call.Factory>,
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val fileStorage: FileStorage,
    private val progressCache: DownloadProgressCache,
    private val notificationObserver: DownloadNotificationObserver,
) : Worker(context, params) {
    private val args = inputData.toArgs()

    private val downloadError = EpisodeDownloadError().apply {
        episodeUuid = args.episodeUuid
        podcastUuid = args.podcastUuid
    }

    private val downloader = EpisodeDownloader(
        httpClient = httpClient,
        progressCache = progressCache,
        onCall = { call ->
            downloadCall = call
        },
        onResponse = { response ->
            downloadError.httpStatusCode = response.code
            downloadError.contentType = response.header("Content-Type") ?: "missing"
            downloadError.tlsCipherSuite = response.handshake?.cipherSuite?.javaName?.uppercase() ?: "missing"
            downloadError.isCellular = Network.isCellularConnection(context)
            downloadError.isProxy = Network.isVpnConnection(context)
        },
        onComplete = { downloadProgress, fileSize ->
            downloadError.expectedContentLength = downloadProgress?.contentLength
            downloadError.responseBodyBytesReceived = downloadProgress?.downloadedByteCount
            downloadError.fileSize = fileSize
        },
    )

    private var notificationJob: NotificationJob? = null
    private var downloadCall: Call? = null

    override fun doWork(): Result {
        if (!isStopped) {
            LogBuffer.i(LogBuffer.TAG_DOWNLOAD, "Download started. Episode: ${args.episodeUuid}")
        }
        val timedValue = measureTimedValue {
            try {
                // Block the work until the state is dispatched to keep it consistent
                dispatchProgressData(isWorkExecuting = true).get()

                var episode = getEpisodeOrThrow()
                prepareForegroundNotification(episode)

                episode = refreshDownloadUrlOrThrow(episode)
                val downloadFile = getDownloadFileOrThrow(episode)
                val tempFile = fileStorage.getOrCreatePodcastEpisodeTempFile(episode)

                val downloadResult = downloader.download(
                    episode = episode,
                    downloadFile = downloadFile,
                    tempFile = tempFile,
                )
                downloadResult
            } catch (e: Throwable) {
                DownloadResult.ExceptionFailure(e)
            }
        }
        downloadError.taskDuration = timedValue.duration.inWholeMilliseconds

        notificationJob?.cancel()
        dispatchProgressData(isWorkExecuting = false)

        return when (val result = timedValue.value) {
            is DownloadResult.Success -> {
                val data = Data.Builder()
                    .putString(DOWNLOAD_FILE_PATH_KEY, result.file.path)
                    .build()
                Result.success(data)
            }

            is DownloadResult.Failure -> {
                val (errorMessage, shouldRetry) = processFailure(result)
                if (shouldRetry && runAttemptCount < MAX_DOWNLOAD_ATTEMPT_COUNT) {
                    Result.retry()
                } else {
                    val data = Data.Builder()
                        .putString(ERROR_MESSAGE_KEY, errorMessage)
                        .putAll(downloadError.toProperties())
                        .build()
                    Result.failure(data)
                }
            }
        }
    }

    override fun onStopped() {
        downloadCall?.cancel()
        notificationJob?.cancel()
        dispatchProgressData(isWorkExecuting = false)
    }

    private fun dispatchProgressData(isWorkExecuting: Boolean): ListenableFuture<Void> {
        val data = Data.Builder()
            .putBoolean(IS_WORK_EXECUTING_KEY, isWorkExecuting)
            .build()
        return setProgressAsync(data)
    }

    private fun getEpisodeOrThrow() = runBlocking {
        requireNotNull(episodeManager.findEpisodeByUuid(args.episodeUuid)) {
            context.getString(LR.string.error_download_missing_episode)
        }
    }

    private fun refreshDownloadUrlOrThrow(episode: BaseEpisode) = runBlocking<BaseEpisode> {
        when (episode) {
            is PodcastEpisode -> {
                val freshDownloadUrl = episodeManager.updateDownloadUrl(episode)
                episode.copy(downloadUrl = freshDownloadUrl)
            }

            is UserEpisode -> {
                val freshDownloadUrl = userEpisodeManager.getPlaybackUrlRxSingle(episode).await()
                episode.copy(downloadUrl = freshDownloadUrl)
            }
        }
    }

    private fun getDownloadFileOrThrow(episode: BaseEpisode): File {
        return requireNotNull(fileStorage.getOrCreatePodcastEpisodeFile(episode)) {
            context.getString(LR.string.error_download_no_episode_file)
        }
    }

    private fun processFailure(result: DownloadResult.Failure): Pair<String, Boolean> {
        return when (result) {
            is DownloadResult.InvalidDownloadUrl -> {
                downloadError.reason = EpisodeDownloadError.Reason.MalformedHost
                context.getString(LR.string.error_download_invalid_url) to false
            }

            is DownloadResult.UnsuccessfulHttpCall -> {
                downloadError.reason = EpisodeDownloadError.Reason.StatusCode
                context.getString(LR.string.error_download_http_failure, result.code.toHumanReadable()) to true
            }

            is DownloadResult.InvalidContentType -> {
                downloadError.reason = EpisodeDownloadError.Reason.ContentType
                context.getString(LR.string.error_download_invalid_content_type, result.contentType) to true
            }

            is DownloadResult.SuspiciousFileSize -> {
                downloadError.reason = EpisodeDownloadError.Reason.SuspiciousContent
                context.getString(LR.string.error_download_suspicious_content_size) to true
            }

            is DownloadResult.ExceptionFailure -> {
                val throwable = result.throwable
                if (!isStopped) {
                    LogBuffer.i(LogBuffer.TAG_DOWNLOAD, throwable, "Download failed: ${args.episodeUuid}")
                }
                // Order of checks here is important. Wrong order will result in mapping to wrong messages or states.
                // For example SocketTimeoutException inherits from InterruptedIOException. Checking for isCancelled
                // and consequently InterruptedIOException would result in mapping timeouts to cancellations.
                when {
                    throwable.isOutOfStorage() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.NotEnoughStorage
                        context.getString(LR.string.error_download_no_storage) to false
                    }

                    throwable.isChartableBlocked() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.ChartableBlocked
                        context.getString(LR.string.error_download_chartable) to false
                    }

                    throwable.isAnyCause<UnknownHostException>() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.UnknownHost
                        context.getString(LR.string.error_download_unknown_host) to true
                    }

                    throwable.isAnyCause<SocketException>() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.SocketIssue
                        context.getString(LR.string.error_download_connection_error) to true
                    }

                    throwable.isAnyCause<SocketTimeoutException>() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.ConnectionTimeout
                        context.getString(LR.string.error_download_socket_timeout) to true
                    }

                    throwable.isAnyCause<SSLException>() -> {
                        downloadError.reason = EpisodeDownloadError.Reason.NoSSl
                        context.getString(LR.string.error_download_ssl_failure) to true
                    }

                    throwable is IOException -> {
                        context.getString(LR.string.error_download_io_failure) to true
                    }

                    throwable is StorageException -> {
                        downloadError.reason = EpisodeDownloadError.Reason.StorageIssue
                        context.getString(LR.string.error_download_generic_failure, throwable.message) to false
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

    private fun Int.toHumanReadable() = buildString {
        append(this@toHumanReadable)
        val text = when (this@toHumanReadable) {
            400 -> "Bad Request"
            401 -> "Unauthorized"
            402 -> "Payment Required"
            403 -> "Forbidden"
            404 -> "Not Found"
            406 -> "Not Acceptable"
            408 -> "Request Timeout"
            410 -> "Gone"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            501 -> "Not Implemented"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> null
        }
        if (text != null) {
            append(" (")
            append(text)
            append(')')
        }
    }

    data class Args(
        val episodeUuid: String,
        val podcastUuid: String,
        val waitForWifi: Boolean,
        val waitForPower: Boolean,
        val sourceView: SourceView,
    )

    companion object {
        const val WORKER_TAG = "EpisodeDownloadWorker"

        internal const val WORKER_EPISODE_TAG_PREFIX = "$WORKER_TAG:Episode:"

        internal const val WORKER_PODCAST_TAG_PREFIX = "$WORKER_TAG:Podcast:"

        internal const val WORKER_SOURCE_VIEW_TAG_PREFIX = "$WORKER_TAG:SourceView:"

        fun episodeTag(episodeUuid: String) = "$WORKER_EPISODE_TAG_PREFIX$episodeUuid"

        fun podcastTag(podcastUuid: String) = "$WORKER_PODCAST_TAG_PREFIX$podcastUuid"

        fun sourceViewTag(sourceView: SourceView) = "$WORKER_SOURCE_VIEW_TAG_PREFIX${sourceView.analyticsValue}"

        fun createWorkRequest(args: Args): Pair<OneTimeWorkRequest, Constraints> {
            val constraints = Constraints.Builder()
                .setRequiresCharging(args.waitForPower)
                .setRequiredNetworkType(if (args.waitForWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()
            val request = OneTimeWorkRequestBuilder<DownloadEpisodeWorker>()
                .setConstraints(constraints)
                .setInputData(args.toData())
                .addTag(WORKER_TAG)
                .addTag(episodeTag(args.episodeUuid))
                .addTag(podcastTag(args.podcastUuid))
                .addTag(sourceViewTag(args.sourceView))
                .build()
            return request to constraints
        }

        fun mapToDownloadWorkInfo(info: WorkInfo): DownloadWorkInfo? {
            val episodeUuid = info.findTag(WORKER_EPISODE_TAG_PREFIX) ?: return null
            val podcastUuid = info.findTag(WORKER_PODCAST_TAG_PREFIX) ?: return null
            val sourceView = info.findTag(WORKER_SOURCE_VIEW_TAG_PREFIX)?.let(SourceView::fromString) ?: SourceView.UNKNOWN

            val isWifiRequired = info.constraints.requiredNetworkType == NetworkType.UNMETERED
            val isPowerRequired = info.constraints.requiresCharging()
            val isStorageRequired = info.constraints.requiresStorageNotLow()

            return when (info.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                    DownloadWorkInfo.Pending(
                        id = info.id,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        runAttemptCount = info.runAttemptCount,
                        sourceView = sourceView,
                        isWifiRequired = isWifiRequired,
                        isPowerRequired = isPowerRequired,
                        isStorageRequired = isStorageRequired,
                    )
                }

                WorkInfo.State.RUNNING -> {
                    // Running doesn't necessarily mean that Worker.doWork() was called.
                    // For this reason we use a separate flag.
                    if (info.progress.getBoolean(IS_WORK_EXECUTING_KEY, false)) {
                        DownloadWorkInfo.InProgress(
                            id = info.id,
                            episodeUuid = episodeUuid,
                            podcastUuid = podcastUuid,
                            runAttemptCount = info.runAttemptCount,
                            sourceView = sourceView,
                        )
                    } else {
                        DownloadWorkInfo.Pending(
                            id = info.id,
                            episodeUuid = episodeUuid,
                            podcastUuid = podcastUuid,
                            runAttemptCount = info.runAttemptCount,
                            sourceView = sourceView,
                            isWifiRequired = isWifiRequired,
                            isPowerRequired = isPowerRequired,
                            isStorageRequired = isStorageRequired,
                        )
                    }
                }

                WorkInfo.State.SUCCEEDED -> {
                    val filePath = info.outputData.getString(DOWNLOAD_FILE_PATH_KEY)
                    if (filePath != null) {
                        DownloadWorkInfo.Success(
                            id = info.id,
                            episodeUuid = episodeUuid,
                            podcastUuid = podcastUuid,
                            runAttemptCount = info.runAttemptCount,
                            sourceView = sourceView,
                            downloadFile = File(filePath),
                        )
                    } else {
                        DownloadWorkInfo.Failure(
                            id = info.id,
                            episodeUuid = episodeUuid,
                            podcastUuid = podcastUuid,
                            runAttemptCount = info.runAttemptCount,
                            sourceView = sourceView,
                            error = EpisodeDownloadError(),
                            errorMessage = MISSING_DOWNLOADED_FILE_PATH_ERROR,
                        )
                    }
                }

                WorkInfo.State.FAILED -> {
                    val error = EpisodeDownloadError.fromProperties(info.outputData.keyValueMap)
                    val errorMessage = info.outputData.getString(ERROR_MESSAGE_KEY)
                    DownloadWorkInfo.Failure(
                        id = info.id,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        runAttemptCount = info.runAttemptCount,
                        sourceView = sourceView,
                        error = error,
                        errorMessage = errorMessage,
                    )
                }

                WorkInfo.State.CANCELLED -> {
                    DownloadWorkInfo.Cancelled(
                        id = info.id,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        runAttemptCount = info.runAttemptCount,
                        sourceView = sourceView,
                    )
                }
            }
        }

        private fun WorkInfo.findTag(prefix: String): String? = tags
            .firstOrNull { it.startsWith(prefix) }
            ?.substringAfter(prefix, missingDelimiterValue = "")
            ?.takeIf(String::isNotEmpty)
    }
}

sealed interface DownloadWorkInfo {
    val id: UUID
    val episodeUuid: String
    val podcastUuid: String
    val runAttemptCount: Int
    val sourceView: SourceView

    val isCancellable: Boolean
    val isTooManyAttempts get() = runAttemptCount >= MAX_DOWNLOAD_ATTEMPT_COUNT

    data class Pending(
        override val id: UUID,
        override val episodeUuid: String,
        override val podcastUuid: String,
        override val runAttemptCount: Int,
        override val sourceView: SourceView,
        val isWifiRequired: Boolean,
        val isPowerRequired: Boolean,
        val isStorageRequired: Boolean,
    ) : DownloadWorkInfo {
        override val isCancellable get() = true
    }

    data class InProgress(
        override val id: UUID,
        override val episodeUuid: String,
        override val podcastUuid: String,
        override val runAttemptCount: Int,
        override val sourceView: SourceView,
    ) : DownloadWorkInfo {
        override val isCancellable get() = true
    }

    data class Success(
        override val id: UUID,
        override val episodeUuid: String,
        override val podcastUuid: String,
        override val runAttemptCount: Int,
        override val sourceView: SourceView,
        val downloadFile: File,
    ) : DownloadWorkInfo {
        override val isCancellable get() = false
    }

    data class Failure(
        override val id: UUID,
        override val episodeUuid: String,
        override val podcastUuid: String,
        override val runAttemptCount: Int,
        override val sourceView: SourceView,
        val error: EpisodeDownloadError,
        val errorMessage: String?,
    ) : DownloadWorkInfo {
        override val isCancellable get() = false
    }

    data class Cancelled(
        override val id: UUID,
        override val episodeUuid: String,
        override val podcastUuid: String,
        override val runAttemptCount: Int,
        override val sourceView: SourceView,
    ) : DownloadWorkInfo {
        override val isCancellable get() = false
    }
}

private fun Data.toArgs() = DownloadEpisodeWorker.Args(
    episodeUuid = getString(EPISODE_UUID_KEY).orEmpty(),
    podcastUuid = getString(PODCAST_UUID_KEY).orEmpty(),
    waitForWifi = getBoolean(WAIT_FOR_WIFI_KEY, true),
    waitForPower = getBoolean(WAIT_FOR_POWER_KEY, true),
    sourceView = SourceView.fromString(getString(SOURCE_VIEW_KEY)),
)

private fun DownloadEpisodeWorker.Args.toData() = Data.Builder()
    .putString(EPISODE_UUID_KEY, episodeUuid)
    .putString(PODCAST_UUID_KEY, podcastUuid)
    .putBoolean(WAIT_FOR_WIFI_KEY, waitForWifi)
    .putBoolean(WAIT_FOR_POWER_KEY, waitForPower)
    .putString(SOURCE_VIEW_KEY, sourceView.analyticsValue)
    .build()

// Input keys
private const val EPISODE_UUID_KEY = "${WORKER_TAG}episode_uuid"
private const val PODCAST_UUID_KEY = "${WORKER_TAG}podcast_uuid"
private const val WAIT_FOR_WIFI_KEY = "${WORKER_TAG}wait_for_wifi"
private const val WAIT_FOR_POWER_KEY = "${WORKER_TAG}wait_for_power"
internal const val SOURCE_VIEW_KEY = "${WORKER_TAG}source_view"

// Progress keys
internal const val IS_WORK_EXECUTING_KEY = "${WORKER_TAG}is_work_executing"

// Output keys
internal const val DOWNLOAD_FILE_PATH_KEY = "${WORKER_TAG}download_file_path"
internal const val ERROR_MESSAGE_KEY = "${WORKER_TAG}error_message"

private val OUT_OF_STORAGE_MESSAGES = setOf("no space", "not enough space", "disk full", "quota")
private const val MAX_DOWNLOAD_ATTEMPT_COUNT = 3
internal const val MISSING_DOWNLOADED_FILE_PATH_ERROR = "Unable to find the downloaded file path. This should never happen. Please contact support."
