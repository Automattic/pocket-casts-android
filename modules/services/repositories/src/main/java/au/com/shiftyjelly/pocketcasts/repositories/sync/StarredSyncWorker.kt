package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.pocketcasts.service.api.StarredEpisode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx2.await

@HiltWorker
class StarredSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val syncManager: SyncManager,
    private val settings: Settings,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORKER_TAG = "pocket_casts_starred_sync_worker_tag"
        private val ONE_WEEK_MS = TimeUnit.DAYS.toMillis(7)

        fun enqueue(syncManager: SyncManager, context: Context): Operation? {
            if (!syncManager.isLoggedIn()) {
                return null
            }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - scheduled")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<StarredSyncWorker>()
                .addTag(WORKER_TAG)
                .setConstraints(constraints)
                .build()
            return WorkManager.getInstance(context).enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
        }
    }

    override suspend fun doWork() = coroutineScope {
        val startTime = SystemClock.elapsedRealtime()
        try {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - started")
            performSync()
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - finished - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
            Result.success()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "StarredSyncWorker - failed - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
            Result.failure()
        }
    }

    private suspend fun performSync() {
        val serverEpisodes = syncManager.getStarredEpisodesOrThrow().episodesList

        // ignore older episodes we have already processed or missing episode will cause lots of server calls and a slow sync. Include all episodes starred in the last week to handle slow syncing devices.
        val lastStarredModified = settings.getStarredServerModified()
        val oneWeekAgo = System.currentTimeMillis() - ONE_WEEK_MS
        val serverEpisodesFiltered = serverEpisodes.filter { episode ->
            episode.starredModified >= oneWeekAgo || episode.starredModified > lastStarredModified
        }

        var maxStarredModified = lastStarredModified
        serverEpisodesFiltered.forEach { serverEpisode ->
            processServerEpisode(serverEpisode)
            if (serverEpisode.starredModified > maxStarredModified) {
                maxStarredModified = serverEpisode.starredModified
            }
        }

        // update the last sync time
        if (maxStarredModified > lastStarredModified) {
            settings.setStarredServerModified(maxStarredModified)
        }
    }

    private suspend fun processServerEpisode(serverEpisode: StarredEpisode) {
        val podcastUuid = serverEpisode.podcastUuid
        val episodeUuid = serverEpisode.uuid

        // import missing podcast
        val podcast = podcastManager.findOrDownloadPodcastRxSingle(podcastUuid).await() ?: return

        // import missing episodes
        val episodeStartTime = SystemClock.elapsedRealtime()
        var localEpisode = episodeManager.findByUuid(episodeUuid)

        // podcast not followed aren't kept up to date, so we need to download the episode
        if (localEpisode == null && !podcast.isSubscribed) {
            localEpisode = episodeManager.downloadMissingPodcastEpisode(episodeUuid = episodeUuid, podcastUuid = podcastUuid)
        }

        if (localEpisode == null) {
            return
        }

        // sync starred state
        if (!localEpisode.isStarred || localEpisode.lastStarredDate != serverEpisode.starredModified) {
            val syncStartTime = SystemClock.elapsedRealtime()
            episodeManager.starEpisodeFromServer(
                episode = localEpisode,
                modified = serverEpisode.starredModified,
            )
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "StarredSyncWorker - synced starred state for $episodeUuid - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - syncStartTime)}")
        }
    }
}
