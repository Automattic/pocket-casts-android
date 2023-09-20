package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException

/**
 * Try to cache the show notes so they can be viewed offline. This task happens when the user downloads an episode.
 */
@HiltWorker
class UpdateShowNotesTask @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val showNotesManager: ServerShowNotesManager
) : CoroutineWorker(context, params) {
    companion object {
        private const val TASK_NAME = "UpdateShowNotesTask"
        const val INPUT_PODCAST_UUID = "podcast_uuid"

        fun enqueue(episode: BaseEpisode, constraints: Constraints = Constraints.NONE, context: Context) {
            if (episode !is PodcastEpisode) {
                return
            }

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME - enqueued ${episode.uuid}")
            val cacheShowNotesData = Data.Builder()
                .putString(INPUT_PODCAST_UUID, episode.podcastUuid)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<UpdateShowNotesTask>()
                .setInputData(cacheShowNotesData)
                .addTag(episode.uuid)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).beginUniqueWork(TASK_NAME, ExistingWorkPolicy.APPEND, workRequest).enqueue()
        }
    }

    private val podcastUuid = inputData.getString(INPUT_PODCAST_UUID) ?: ""

    override suspend fun doWork(): Result {
        info("Worker started - podcast: $podcastUuid")
        val startTime = SystemClock.elapsedRealtime()
        return try {
            showNotesManager.downloadToCacheShowNotes(podcastUuid = podcastUuid)
            info("Worker completed - took ${SystemClock.elapsedRealtime() - startTime} ms")
            Result.success()
        } catch (e: Exception) {
            info("Worker failed - took ${SystemClock.elapsedRealtime() - startTime} ms")
            val logPriority = if (e is HttpException) Log.INFO else Log.ERROR
            LogBuffer.log(logPriority, LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to update show notes")
            // Don't keep retrying if the download fails. The user can download the show notes when viewing them.
            Result.success()
        }
    }

    private fun info(message: String) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "$TASK_NAME (Worker ID: $id) - $message")
    }
}
