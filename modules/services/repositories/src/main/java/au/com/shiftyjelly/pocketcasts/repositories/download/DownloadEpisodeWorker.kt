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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloader.Result as DownloadResult

@HiltWorker
class DownloadEpisodeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    @Downloads httpClient: Lazy<OkHttpClient>,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val progressCache: DownloadProgressCache,
    private val notificationObserver: DownloadNotificationObserver,
) : Worker(context, params) {
    private val args = inputData.toArgs()
    private val downloader = EpisodeDownloader(httpClient, progressCache)
    private var notificationJob: Job? = null

    override fun doWork(): Result {
        val episode = runBlocking { episodeManager.findEpisodeByUuid(args.episodeUuid) } ?: run {
            // TODO: Handle missing episode
            return Result.failure()
        }
        val downloadFile = DownloadHelper.pathForEpisode(episode, fileStorage)?.let(::File) ?: run {
            // TODO: Handle no download file
            return Result.failure()
        }
        val tempFile = File(DownloadHelper.tempPathForEpisode(episode, fileStorage))

        notificationJob = notificationObserver.observeNotificationUpdates(episode) { id, notification ->
            setForegroundAsync(createForegroundInfo(id, notification))
        }

        val result = downloader.download(
            episode = episode,
            downloadFile = downloadFile,
            tempFile = tempFile,
        )
        notificationJob?.cancel()

        return when (result) {
            is DownloadResult.Success -> {
                Result.success()
            }

            is DownloadResult.Failure -> {
                processFailure(result)
                Result.failure()
            }
        }
    }

    override fun onStopped() {
        notificationJob?.cancel()
    }

    private fun processFailure(result: DownloadResult.Failure) {
        val message = when (result) {
            is DownloadResult.InvalidDownloadUrl -> {
                "Invalid URL: ${result.url}"
            }

            is DownloadResult.UnsuccessfulHttpCall -> {
                "Failed HTTP call: ${result.code}"
            }

            is DownloadResult.ExceptionFailure -> {
                Timber.tag("Downloads").d(result.throwable)
                val throwable = result.throwable
                when {
                    throwable.isOutOfStorage() -> {
                        "Out of storage"
                    }

                    throwable.isChartableBlocked() -> {
                        "Blocked chartable"
                    }

                    throwable.isCancelled() -> {
                        "Download cancelled"
                    }

                    throwable is SocketTimeoutException -> {
                        "Socket timeout"
                    }

                    throwable is UnknownHostException -> {
                        "Unknown host"
                    }

                    throwable is SSLException -> {
                        "SSL problem"
                    }

                    throwable is IOException -> {
                        "IO problem"
                    }

                    else -> {
                        "Generic problem"
                    }
                }
            }
        }
        Timber.tag("Downloads").d("Download failed: $message")
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
                is InterruptedIOException -> {
                    return true
                }

                is IOException -> {
                    if (throwable.message?.lowercase() == "Cancelled") {
                        return true
                    }
                }
            }
            throwable = throwable.cause
        }
        return false
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
                .setRequiredNetworkType(if (args.waitForWifi) NetworkType.UNMETERED else NetworkType.NOT_REQUIRED)
                .setRequiresStorageNotLow(true)
                .build()
            return OneTimeWorkRequestBuilder<DownloadEpisodeWorker>()
                .setConstraints(constraints)
                .setInputData(args.toData())
                .addTag(WORKER_TAG)
                .addTag(episodeWorkerName(args.episodeUuid))
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
