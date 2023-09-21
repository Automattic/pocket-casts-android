package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * This task updates the download url of an episode.
 */
@HiltWorker
class UpdateEpisodeTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted val params: WorkerParameters,
    val podcastCacheServerManager: PodcastCacheServerManager,
    val appDatabase: AppDatabase
) : CoroutineWorker(context, params) {
    companion object {
        const val INPUT_PODCAST_UUID = "podcast_uuid"
        const val INPUT_EPISODE_UUID = "episode_uuid"

        fun buildInputData(episode: PodcastEpisode): Data {
            return Data.Builder()
                .putString(INPUT_EPISODE_UUID, episode.uuid)
                .putString(INPUT_PODCAST_UUID, episode.podcastUuid)
                .build()
        }
    }

    private val podcastUuid = inputData.getString(INPUT_PODCAST_UUID)
    private val episodeUuid = inputData.getString(INPUT_EPISODE_UUID)
    private val episodeDao = appDatabase.episodeDao()

    override suspend fun doWork(): Result {
        try {
            if (podcastUuid == null || episodeUuid == null) {
                return Result.success()
            }

            val serverPodcast = podcastCacheServerManager.getPodcastAndEpisode(podcastUuid, episodeUuid)

            val episode = episodeDao.findByUuid(episodeUuid)
            val serverEpisodeUrl = serverPodcast.episodes.firstOrNull()?.downloadUrl
            if (episode != null &&
                !serverEpisodeUrl.isNullOrBlank() &&
                episode.downloadUrl != serverEpisodeUrl
            ) {
                episode.downloadUrl = serverEpisodeUrl
                episodeDao.updateDownloadUrl(serverEpisodeUrl, episode.uuid)
            }

            return Result.success()
        } catch (e: Exception) {
            Timber.i(e, "Failed to update episode download url. Podcast: $podcastUuid Episode: $episodeUuid")
            return Result.success()
        }
    }
}
