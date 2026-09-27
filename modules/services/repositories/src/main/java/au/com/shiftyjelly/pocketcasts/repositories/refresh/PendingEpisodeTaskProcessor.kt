package au.com.shiftyjelly.pocketcasts.repositories.refresh

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.dao.PendingEpisodeTaskDao
import au.com.shiftyjelly.pocketcasts.models.entity.PendingEpisodeTask
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.AutoAddUpNext
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.AutoDownloadEpisodeProvider
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Clock
import java.time.Duration

/**
 * Runs the steps that a refresh records for its new episodes, so they still happen when the refresh that
 * recorded them was killed before it got to them.
 */
class PendingEpisodeTaskProcessor(
    private val pendingEpisodeTaskDao: PendingEpisodeTaskDao,
    private val episodeManager: EpisodeManager,
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val autoDownloadProvider: AutoDownloadEpisodeProvider,
    private val downloadQueue: DownloadQueue,
    private val clock: Clock,
) {
    suspend fun removeExpiredTasks() {
        pendingEpisodeTaskDao.deleteCreatedBefore(clock.instant() - EXPIRY)
    }

    suspend fun processUpNext(newEpisodeUuids: Collection<String>) {
        val episodeUuids = pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.UP_NEXT)
        if (episodeUuids.isEmpty()) {
            return
        }
        logRecovered(PendingEpisodeTask.Type.UP_NEXT, episodeUuids, newEpisodeUuids)

        val episodes = episodeManager.findByUuids(episodeUuids).filter { !it.isFinished && !it.isArchived }
        val modesByPodcast = episodes
            .map(PodcastEpisode::podcastUuid)
            .distinct()
            .associateWith { podcastUuid -> podcastManager.findPodcastByUuid(podcastUuid)?.autoAddToUpNext }
        val episodesToAdd = episodes.mapNotNull { episode ->
            val mode = modesByPodcast[episode.podcastUuid]
            if (mode == null || mode == AutoAddUpNext.OFF) null else mode to episode
        }

        playbackManager.addEpisodes(episodesToAdd)
        pendingEpisodeTaskDao.delete(PendingEpisodeTask.Type.UP_NEXT, episodeUuids)
    }

    suspend fun processAutoDownload(newEpisodeUuids: Collection<String>) {
        val episodeUuids = pendingEpisodeTaskDao.findEpisodeUuids(PendingEpisodeTask.Type.AUTO_DOWNLOAD)
        logRecovered(PendingEpisodeTask.Type.AUTO_DOWNLOAD, episodeUuids, newEpisodeUuids)

        val episodes = autoDownloadProvider.getAll(episodeUuids)
        val enqueueJob = downloadQueue.enqueueAll(episodes, DownloadType.Automatic(bypassAutoDownloadStatus = false), SourceView.AUTO_DOWNLOAD)
        enqueueJob.join()
        if (!enqueueJob.isCancelled) {
            pendingEpisodeTaskDao.delete(PendingEpisodeTask.Type.AUTO_DOWNLOAD, episodeUuids)
        }
    }

    private fun logRecovered(task: PendingEpisodeTask.Type, episodeUuids: List<String>, newEpisodeUuids: Collection<String>) {
        val recoveredCount = (episodeUuids - newEpisodeUuids.toSet()).size
        if (recoveredCount > 0) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - running $task for $recoveredCount episodes missed by an interrupted refresh")
        }
    }

    private companion object {
        val EXPIRY: Duration = Duration.ofDays(7)
    }
}
