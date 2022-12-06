package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncChange
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncRequest
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.switchInvalidForNow
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import retrofit2.HttpException
import java.util.Date

private const val TAG = "SyncHistoryTask"

@HiltWorker
class SyncHistoryTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    var episodeManager: EpisodeManager,
    var serverManager: SyncServerManager,
    var podcastManager: PodcastManager,
    var settings: Settings,
    private val historyManager: HistoryManager,
) : CoroutineWorker(context, params) {

    companion object {
        fun scheduleToRun(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncHistoryTask>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, workRequest)
            LogBuffer.i(TAG, "Sync history task scheduled")
        }
    }

    override suspend fun doWork(): Result {
        LogBuffer.i(TAG, "Sync history running")

        val episodes = episodeManager.findEpisodesForHistorySync()

        val changes = episodes.mapNotNull { episode ->
            val lastPlaybackInteraction = episode.lastPlaybackInteraction ?: return@mapNotNull null
            HistorySyncChange(
                action = if (episode.lastPlaybackInteractionSyncStatus == 2L) 2 else 1,
                episode = episode.uuid,
                modifiedAt = lastPlaybackInteraction.toString(),
                podcast = episode.podcastUuid,
                published = episode.publishedDate.switchInvalidForNow().toIsoString(),
                title = episode.title,
                url = episode.downloadUrl ?: ""
            )
        }.toMutableList()

        val clearHistoryTime = settings.getClearHistoryTime()
        val wasHistoryCleared = clearHistoryTime > 0
        if (wasHistoryCleared) {
            val change = HistorySyncChange(
                action = 3,
                modifiedAt = clearHistoryTime.toString()
            )
            changes.add(change)
        }

        val request = HistorySyncRequest(
            changes = changes,
            deviceTime = System.currentTimeMillis(),
            serverModified = settings.getHistoryServerModified(),
            version = Settings.SYNC_HISTORY_VERSION
        )

        try {
            val response = serverManager
                .historySync(request)
                .toMaybe()
                .onErrorComplete { it is HttpException && it.code() == 304 }
                .blockingGet()

            if (response != null) {
                historyManager.processServerResponse(
                    response = response,
                    updateServerModified = true
                )

                // Clear history if they have cleared it on the server
                if (response.lastCleared > 0) {
                    val lastCleared = Date(response.lastCleared)
                    episodeManager.clearEpisodePlaybackInteractionDatesBefore(lastCleared)
                }

                episodeManager.markPlaybackHistorySynced()
                if (wasHistoryCleared) {
                    settings.setClearHistoryTime(0L)
                }
            }

            LogBuffer.i(TAG, "Sync history completed successfully")
        } catch (e: Exception) {
            LogBuffer.e(TAG, "Sync history failed: ${e.message}")
            return Result.failure()
        }

        return Result.success()
    }
}
