package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdateEpisodeDetailsTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val params: WorkerParameters,
    var episodeManager: EpisodeManager,
) : CoroutineWorker(context, params) {
    companion object {
        private const val TASK_NAME = "UpdateEpisodeDetailsTask"
        const val INPUT_EPISODE_UUIDS = "episode_uuids"
        private const val REQUEST_TIMEOUT_SECS = 20L

        fun enqueue(episodes: List<PodcastEpisode>, context: Context) {
            if (episodes.isEmpty()) {
                return
            }

            val episodeUuids = episodes.map { it.uuid }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME - enqueued ${episodeUuids.joinToString()}")
            val workData = Data.Builder()
                .putStringArray(INPUT_EPISODE_UUIDS, episodeUuids.toTypedArray())
                .build()
            val workRequest = OneTimeWorkRequestBuilder<UpdateEpisodeDetailsTask>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(workData)
                .build()
            WorkManager.getInstance(context).beginUniqueWork(TASK_NAME, ExistingWorkPolicy.APPEND, workRequest).enqueue()
        }
    }

    private val episodeUuids: List<String>? = inputData.getStringArray(INPUT_EPISODE_UUIDS)?.toList()

    override suspend fun doWork(): Result {
        if (episodeUuids == null) {
            return Result.success()
        }

        if (isStopped) {
            return Result.retry()
        }

        val startTime = SystemClock.elapsedRealtime()
        info("Worker started - episodes: ${episodeUuids.joinToString()}}")

        try {
            val client = OkHttpClient.Builder().callTimeout(REQUEST_TIMEOUT_SECS, TimeUnit.SECONDS).build()

            for (episodeUuid in episodeUuids) {
                val episode = episodeManager.findByUuid(episodeUuid) ?: continue
                val downloadUrl = episode.downloadUrl?.toHttpUrlOrNull() ?: continue
                val request = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("User-Agent", "Pocket Casts")
                    .head()
                    .build()

                if (isStopped) {
                    return Result.retry()
                }

                try {
                    val response = client.newCall(request).await()

                    val contentType = response.header("Content-Type")
                    if (!contentType.isNullOrBlank()) {
                        if ((episode.fileType.isNullOrBlank() && (contentType.startsWith("audio") || contentType.startsWith("video"))) || contentType.startsWith("video")) {
                            episodeManager.updateFileType(episode, contentType)
                            episode.fileType = contentType
                        }
                    }

                    val contentLengthHeader = response.header("Content-Length")
                    if (!contentLengthHeader.isNullOrBlank()) {
                        try {
                            val contentLength = java.lang.Long.parseLong(contentLengthHeader)
                            val sizeInBytes = episode.sizeInBytes
                            if ((sizeInBytes == 0L || sizeInBytes != contentLength) && contentLength > 153600) {
                                episodeManager.updateSizeInBytes(episode, contentLength)
                                episode.sizeInBytes = contentLength
                            }
                        } catch (nfe: NumberFormatException) {
                            Timber.i(nfe, "Unable to read content length.")
                        }
                    }
                } catch (ioException: IOException) {
                    // don't report IO issues such as timeouts to Sentry
                    info("Unable to check episode file details with a head request for $episodeUuid. Error: ${ioException.message}")
                }
            }

            info("Worker completed - took ${SystemClock.elapsedRealtime() - startTime} ms")
            return Result.success()
        } catch (t: Throwable) {
            info("Worker failed - took ${SystemClock.elapsedRealtime() - startTime} ms")
            // report the error to Sentry and to the log buffer for support
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "Unable to check episode file details with a head request.")
            // mark the worker as a success or it will won't process all the chain tasks
            return Result.success()
        }
    }

    /**
     * Log a message to the log buffer for support and to the console for debugging.
     */
    private fun info(message: String) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME (Worker ID: $id) - $message")
    }
}
