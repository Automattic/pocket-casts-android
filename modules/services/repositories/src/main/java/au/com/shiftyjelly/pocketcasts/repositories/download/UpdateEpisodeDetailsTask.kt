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
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@HiltWorker
class UpdateEpisodeDetailsTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val episodeManager: EpisodeManager,
    @Downloads private val httpClient: OkHttpClient,
) : CoroutineWorker(context, params) {
    companion object {
        private const val TASK_NAME = "UpdateEpisodeDetailsTask"
        const val INPUT_EPISODE_UUIDS = "episode_uuids"
        private const val MAX_RETRIES = 3

        fun enqueue(episodes: List<PodcastEpisode>, context: Context) {
            // As Wear OS or Automotive are both have limited resources and they won't play audio don't check the episode content type and file size
            if (episodes.isEmpty() || Util.isWearOs(context) || Util.isAutomotive(context)) {
                return
            }

            val episodeUuids = episodes.filterNot { ignoreEpisode(it) }.map { it.uuid }
            if (episodeUuids.isEmpty()) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME - no episodes found to check")
                return
            }
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

        private fun ignoreEpisode(episode: PodcastEpisode): Boolean {
            // Skip metadata check for episodes that are already downloaded, as the download task also checks the content type.
            return episode.isQueued || episode.isDownloaded || episode.isDownloading || episode.isArchived
        }
    }

    private val episodeUuids: List<String>? = inputData.getStringArray(INPUT_EPISODE_UUIDS)?.toList()

    override suspend fun doWork(): Result {
        if (episodeUuids == null) {
            return Result.success()
        }

        if (isStopped) {
            return retryWithLimit()
        }

        val startTime = SystemClock.elapsedRealtime()
        info("Worker started - episodes: ${episodeUuids.joinToString()}}")

        try {
            for (episodeUuid in episodeUuids) {
                val episode = episodeManager.findByUuid(episodeUuid) ?: continue
                if (ignoreEpisode(episode)) {
                    info("Ignoring episode ${episode.uuid}")
                    continue
                }
                val downloadUrl = episode.downloadUrl?.toHttpUrlOrNull() ?: continue
                val request = Request.Builder()
                    .url(downloadUrl)
                    .head()
                    .build()

                if (isStopped) {
                    return retryWithLimit()
                }

                try {
                    val response = httpClient.newCall(request).await()

                    val contentType = response.header("Content-Type")
                    if (!contentType.isNullOrBlank()) {
                        if ((episode.fileType.isNullOrBlank() && (contentType.startsWith("audio") || contentType.startsWith("video"))) || contentType.startsWith("video")) {
                            episodeManager.updateFileTypeBlocking(episode, contentType)
                            episode.fileType = contentType
                        }
                    }

                    val contentLengthHeader = response.header("Content-Length")
                    if (!contentLengthHeader.isNullOrBlank()) {
                        try {
                            val contentLength = java.lang.Long.parseLong(contentLengthHeader)
                            val sizeInBytes = episode.sizeInBytes
                            if ((sizeInBytes == 0L || sizeInBytes != contentLength) && contentLength > 153600) {
                                episodeManager.updateSizeInBytesBlocking(episode, contentLength)
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

    private fun retryWithLimit(): Result {
        val attempt = runAttemptCount + 1
        return if (attempt < MAX_RETRIES) {
            Result.retry()
        } else {
            info("Worker stopped after $attempt attempts.")
            // mark the worker as a success or it will won't process all the chain tasks
            Result.success()
        }
    }

    /**
     * Log a message to the log buffer for support and to the console for debugging.
     */
    private fun info(message: String) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME (Worker ID: $id) - $message")
    }
}
