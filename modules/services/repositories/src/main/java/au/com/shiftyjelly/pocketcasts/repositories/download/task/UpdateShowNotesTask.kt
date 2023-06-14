package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

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

    private val podcastUuid = inputData.getString(INPUT_EPISODE_UUID)!!
    private val episodeUuid = inputData.getString(INPUT_EPISODE_UUID)!!

    override suspend fun doWork(): Result {
        return try {
            showNotesManager.downloadShowNotes(podcastUuid = podcastUuid, episodeUuid = episodeUuid)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
