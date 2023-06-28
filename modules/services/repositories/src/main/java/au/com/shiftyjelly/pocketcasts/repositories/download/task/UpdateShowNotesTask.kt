package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
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
        const val INPUT_PODCAST_UUID = "podcast_uuid"
        const val INPUT_EPISODE_UUID = "episode_uuid"
    }

    private val podcastUuid = inputData.getString(INPUT_PODCAST_UUID) ?: ""
    private val episodeUuid = inputData.getString(INPUT_EPISODE_UUID) ?: ""

    override suspend fun doWork(): Result {
        return try {
            showNotesManager.downloadShowNotes(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
            Result.success()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "Failed to update show notes")
            if (e !is HttpException) {
                SentryHelper.recordException(e)
            }
            // Don't keep retrying if the download fails. The user can download the show notes when viewing them.
            Result.success()
        }
    }
}
