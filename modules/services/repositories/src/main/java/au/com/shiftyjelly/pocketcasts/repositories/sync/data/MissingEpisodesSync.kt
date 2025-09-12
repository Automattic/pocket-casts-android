package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.pocketcasts.service.api.EpisodeResponse
import com.pocketcasts.service.api.podcastsEpisodesRequest
import com.pocketcasts.service.api.publishedOrNull
import java.util.Date

internal class MissingEpisodesSync(
    private val syncManager: SyncManager,
    private val appDatabase: AppDatabase,
) {
    private val playlistDao = appDatabase.playlistDao()
    private val episodeDao = appDatabase.episodeDao()
    private val podcastDao = appDatabase.podcastDao()
    private val useManualPlaylists get() = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)

    suspend fun sync() {
        if (!useManualPlaylists) {
            return
        }
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
            localEpisodes.add(toPodcastEpisode(serverEpisode))
            localPodcasts.add(toPodcast(serverEpisode))
        }
        appDatabase.withTransaction {
            episodeDao.insertAllOrIgnore(localEpisodes)
            podcastDao.insertAllOrIgnore(localPodcasts.distinctBy(Podcast::uuid))
        }
    }
}

private fun toPodcastEpisode(serverEpisode: EpisodeResponse) = PodcastEpisode(
    uuid = serverEpisode.uuid,
    downloadUrl = serverEpisode.url,
    publishedDate = serverEpisode.publishedOrNull?.toDate() ?: Date(0),
    duration = serverEpisode.duration.toDouble(),
    fileType = serverEpisode.fileType,
    title = serverEpisode.title,
    sizeInBytes = serverEpisode.size,
    playingStatus = EpisodePlayingStatus.fromInt(serverEpisode.playingStatus),
    playedUpTo = serverEpisode.playedUpTo.toDouble(),
    isStarred = serverEpisode.starred,
    podcastUuid = serverEpisode.podcastUuid,
    type = serverEpisode.episodeType,
    season = serverEpisode.episodeSeason.toLong(),
    number = serverEpisode.episodeNumber.toLong(),
    isArchived = serverEpisode.isDeleted,
    slug = serverEpisode.slug,
)

private fun toPodcast(serverEpisode: EpisodeResponse) = Podcast(
    uuid = serverEpisode.podcastUuid,
    title = serverEpisode.podcastTitle,
    author = serverEpisode.author,
    slug = serverEpisode.podcastSlug,
)
