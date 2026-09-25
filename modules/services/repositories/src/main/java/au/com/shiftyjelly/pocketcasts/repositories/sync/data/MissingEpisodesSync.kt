package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.pocketcasts.service.api.podcastsEpisodesRequest

internal class MissingEpisodesSync(
    private val syncManager: SyncManager,
    private val appDatabase: AppDatabase,
) {
    private val playlistDao = appDatabase.playlistDao()
    private val episodeDao = appDatabase.episodeDao()
    private val podcastDao = appDatabase.podcastDao()

    suspend fun sync() {
        val missingEpisodes = playlistDao.getAllMissingManualEpisodes()
        if (missingEpisodes.isEmpty()) {
            return
        }
        val request = podcastsEpisodesRequest {
            for (episode in missingEpisodes) {
                podcastUuids.add(episode.podcastUuid)
                episodeUuids.add(episode.episodeUuid)
            }
        }
        val response = syncManager.getEpisodesOrThrow(request)
        if (response.episodesCount == 0) {
            return
        }

        val localEpisodes = mutableListOf<PodcastEpisode>()
        val localPodcasts = mutableListOf<Podcast>()
        response.episodesList.forEach { serverEpisode ->
            localEpisodes.add(serverEpisode.toPodcastEpisode())
            localPodcasts.add(serverEpisode.toPodcast())
        }
        appDatabase.withTransaction {
            episodeDao.insertAllOrIgnore(localEpisodes)
            podcastDao.insertAllOrIgnore(localPodcasts.distinctBy(Podcast::uuid))
        }
    }
}
