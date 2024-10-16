package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextChangeDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.jobs.JobIds
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.extensions.switchInvalidForNow
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Completable
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitSingleOrNull
import retrofit2.HttpException

@HiltWorker
class UpNextSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appDatabase: AppDatabase,
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val settings: Settings,
    private val syncManager: SyncManager,
    private val upNextQueue: UpNextQueue,
    private val userEpisodeManager: UserEpisodeManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        @JvmStatic
        fun run(syncManager: SyncManager, context: Context) {
            // Don't run the job if Up Next syncing is turned off
            if (!syncManager.isLoggedIn()) {
                return
            }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncJob - scheduled")
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobId = JobIds.UP_NEXT_SYNC_JOB_ID
            scheduler.cancel(jobId)
            val builder = JobInfo.Builder(jobId, ComponentName(context, UpNextSyncWorker::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            scheduler.schedule(builder.build())
        }
    }

    override suspend fun doWork() = coroutineScope {
        try {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncWorker - onStartJob")
            performSync()
            Result.success()
        } catch (e: Exception) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "UpNextSyncWorker - failed")
            Result.failure()
        }
    }

    private suspend fun performSync() {
        val startTime = SystemClock.elapsedRealtime()
        val upNextChangeDao = appDatabase.upNextChangeDao()
        val changes = upNextChangeDao.findAll()
        val request = buildRequest(changes)
        try {
            val response = syncManager.upNextSync(request)
            readResponse(response)
            clearSyncedData(request, upNextChangeDao)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncWorker - finished - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
        } catch (e: HttpException) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, "UpNextSyncWorker - failed - ${String.format(Locale.ENGLISH, "%d ms", SystemClock.elapsedRealtime() - startTime)}")
            if (e.code() != 304) throw e
        }
    }

    private fun clearSyncedData(upNextSyncRequest: UpNextSyncRequest, upNextChangeDao: UpNextChangeDao): Completable {
        val latestChange: UpNextSyncRequest.Change? = upNextSyncRequest.upNext.changes.maxByOrNull { it.modified }
        return if (latestChange == null) {
            Completable.complete()
        } else {
            val latestActionTime = latestChange.modified
            upNextChangeDao.deleteChangesOlderOrEqualToRx(latestActionTime)
        }
    }

    private fun buildRequest(changes: List<UpNextChange>): UpNextSyncRequest {
        val requestChanges = mutableListOf<UpNextSyncRequest.Change>()
        for (change in changes) {
            requestChanges.add(buildChangeRequest(change))
        }

        val serverModified = settings.getUpNextServerModified()
        val upNext = UpNextSyncRequest.UpNext(serverModified, requestChanges)
        val deviceTime = System.currentTimeMillis()
        val version = Settings.SYNC_API_VERSION.toString()
        return UpNextSyncRequest(deviceTime, version, upNext)
    }

    private fun buildChangeRequest(change: UpNextChange): UpNextSyncRequest.Change {
        // replace action
        if (change.type == UpNextChange.ACTION_REPLACE) {
            val uuids = change.uuids?.splitIgnoreEmpty(",") ?: listOf()
            val episodes = uuids.map { uuid ->
                val episode = runBlocking { episodeManager.findEpisodeByUuid(uuid) }
                val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
                UpNextSyncRequest.ChangeEpisode(
                    uuid,
                    episode?.title,
                    episode?.downloadUrl,
                    podcastUuid,
                    episode?.publishedDate?.toIsoString(),
                )
            }
            return UpNextSyncRequest.Change(
                UpNextChange.ACTION_REPLACE,
                change.modified,
                episodes = episodes,
            )
        }
        // any other action
        else {
            val uuid = change.uuid
            val episode = if (uuid == null) null else runBlocking { episodeManager.findEpisodeByUuid(uuid) }
            val publishedDate = episode?.publishedDate?.switchInvalidForNow()?.toIsoString()
            val podcastUuid = if (episode is PodcastEpisode) episode.podcastUuid else Podcast.userPodcast.uuid
            return UpNextSyncRequest.Change(
                action = change.type,
                modified = change.modified,
                uuid = change.uuid,
                title = episode?.title,
                url = episode?.downloadUrl,
                published = publishedDate,
                podcast = podcastUuid,
            )
        }
    }

    private suspend fun readResponse(response: UpNextSyncResponse) {
        if (settings.getUpNextServerModified() == 0L && response.episodes.isNullOrEmpty() && playbackManager.getCurrentEpisode() != null) {
            // Server sent empty up next for first log in and we have an up next list already, we should keep the local copy
            upNextQueue.changeList(playbackManager.upNextQueue.queueEpisodes) // Change list will automatically include the current episode
            return
        }

        if (!response.hasChanged(settings.getUpNextServerModified())) {
            return
        }

        // import missing podcasts
        val podcastUuids: List<String> = response.episodes?.mapNotNull { it.podcast }?.filter { it != Podcast.userPodcast.uuid } ?: emptyList()
        podcastUuids.forEach { podcastUuid ->
            podcastManager.findOrDownloadPodcastRx(podcastUuid).await()
        }

        // import missing episodes
        val episodes = response.episodes?.mapNotNull { responseEpisode ->
            val episodeUuid = responseEpisode.uuid
            val podcastUuid = responseEpisode.podcast
            if (podcastUuid != null) {
                if (podcastUuid == Podcast.userPodcast.uuid) {
                    userEpisodeManager.downloadMissingUserEpisode(episodeUuid, placeholderTitle = responseEpisode.title, placeholderPublished = responseEpisode.published?.parseIsoDate())
                        .awaitSingleOrNull()
                } else {
                    val skeletonEpisode = responseEpisode.toSkeletonEpisode(podcastUuid)
                    episodeManager.downloadMissingEpisode(episodeUuid, podcastUuid, skeletonEpisode, podcastManager, false, source = SourceView.UP_NEXT)
                        .awaitSingleOrNull()
                }
            } else null
        } ?: emptyList()

        // import the server Up Next into the database
        upNextQueue.importServerChanges(episodes, playbackManager, downloadManager)
        // check the current episode it correct
        playbackManager.loadQueue()
        // save the server Up Next modified so we only apply changes
        settings.setUpNextServerModified(response.serverModified)
    }
}
