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
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.DownloadStatusUpdate
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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloader.Result as DownloadResult

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
                Result.success()
            }

            is DownloadResult.Failure -> {
                Timber.d("Download failure: ${processFailure(result)}")
                Result.failure()
            }
        }
    }

    override fun onStopped() {
        notificationJob?.cancel()
    }

    private fun getEpisodeOrThrow() = runBlocking {
        requireNotNull(episodeManager.findEpisodeByUuid(args.episodeUuid)) {
            context.getString(LR.string.error_missing_episode)
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
            context.getString(LR.string.error_download_no_episode)
        }
    }

    private fun processFailure(result: DownloadResult.Failure): DownloadStatusUpdate {
        return when (result) {
            is DownloadResult.InvalidDownloadUrl -> {
                DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_invalid_url))
            }

            is DownloadResult.UnsuccessfulHttpCall -> {
                DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_http_failure, result.code))
            }

            is DownloadResult.ExceptionFailure -> {
                val throwable = result.throwable
                Timber.tag("LOG_TAG").i(throwable)
                // Order of checks here is important. Wrong order will result in mapping to wrong messages or states.
                // For example SocketTimeoutException inherits from InterruptedIOException. Checking for isCancelled
                // and consequently InterruptedIOException would result in mapping timeouts to cancellations.
                when {
                    throwable.isOutOfStorage() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_no_storage))
                    }

                    throwable.isChartableBlocked() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_chartable))
                    }

                    throwable.isAnyCause<UnknownHostException>() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_unknown_host))
                    }

                    throwable.isAnyCause<ConnectException>() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_socket_timeout))
                    }

                    throwable.isAnyCause<SocketTimeoutException>() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_socket_timeout))
                    }

                    throwable.isAnyCause<SSLException>() -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_ssl_failure))
                    }

                    throwable.isCancelled() -> {
                        DownloadStatusUpdate.Idle
                    }

                    throwable is IOException -> {
                        DownloadStatusUpdate.Failure(context.getString(LR.string.error_download_io_failure))
                    }

                    else -> {
                        val message = context.getString(LR.string.error_download_generic_failure, throwable.message.orEmpty())
                        DownloadStatusUpdate.Failure(message.trim())
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

    companion object Companion {
        private const val WORKER_TAG = "EpisodeDownloadWorker"

        fun episodeWorkerName(episodeUuid: String) = "$WORKER_TAG:$episodeUuid"

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
    }
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

private const val EPISODE_UUID_KEY = "episode_uuid"
private const val WAIT_FOR_WIFI_KEY = "wait_for_wifi"
private const val WAIT_FOR_POWER_KEY = "wait_for_power"

private val OUT_OF_STORAGE_MESSAGES = setOf("no space", "not enough space", "disk full", "quota")
