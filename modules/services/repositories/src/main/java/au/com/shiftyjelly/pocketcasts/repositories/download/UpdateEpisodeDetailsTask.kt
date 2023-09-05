package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.await
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

@HiltWorker
class UpdateEpisodeDetailsTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val params: WorkerParameters,
    var episodeManager: EpisodeManager
) : CoroutineWorker(context, params) {
    companion object {
        const val TASK_NAME = "UpdateEpisodeDetailsTask"
        const val INPUT_EPISODE_UUIDS = "episode_uuids"
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
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpdateEpisodeDetailsJob - onStartJob")

        try {
            Timber.i("Downloading Meta Data for ${episodeUuids.size} episodes")

            for (episodeUuid in episodeUuids) {
                val episode = runBlocking {
                    episodeManager.findByUuid(episodeUuid)
                } ?: continue
                val downloadUrl = episode.downloadUrl?.toHttpUrlOrNull() ?: continue

                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(downloadUrl)
                    .addHeader("User-Agent", "Pocket Casts")
                    .head()
                    .build()

                if (isStopped) {
                    return Result.retry()
                }

                val response = client.newCall(request).await()

                val contentType = response.header("Content-Type")
                if (contentType != null && contentType.isNotBlank()) {
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
                        Timber.e(nfe, "Unable to read content length.")
                    }
                }
            }
        } catch (t: Throwable) {
            SentryHelper.recordException("Failed to update episode details", t)
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "Unable to check episode file details with a head request.")
            return if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }

        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpdateEpisodeDetailsJob - onTaskCompleted - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        return Result.success()
    }
}
