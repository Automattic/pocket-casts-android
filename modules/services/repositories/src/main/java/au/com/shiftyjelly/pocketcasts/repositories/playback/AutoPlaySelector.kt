package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.file.CloudFilesManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext

class AutoPlaySelector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val cloudFilesManager: CloudFilesManager,
) {
    suspend fun selectNextEpisode(currentEpisodeUuid: String?): Pair<BaseEpisode, AutoPlaySource>? {
        val source = settings.lastAutoPlaySource.value
        val episodes = when (source) {
            is AutoPlaySource.PodcastOrFilter -> {
                val podcast = podcastManager.findPodcastByUuid(uuid = source.uuid)
                if (podcast != null) {
                    findPodcastEpisodes(podcast, currentEpisodeUuid)
                } else {
                    findPlaylistEpisodes(source.uuid, currentEpisodeUuid)
                }
            }

            AutoPlaySource.Predefined.Downloads -> findDownloadedEpisodes()

            AutoPlaySource.Predefined.Files -> findUserEpisodes()

            AutoPlaySource.Predefined.Starred -> findStarredEpisodes()

            AutoPlaySource.Predefined.None -> emptyList()
        }
        val episode = if (currentEpisodeUuid == null) {
            episodes.firstOrNull()
        } else {
            val currentEpisodeIndex = episodes.indexOfFirst { it.uuid == currentEpisodeUuid }
            episodes.getOrNull(currentEpisodeIndex + 1) ?: episodes.firstOrNull()?.takeIf { it.uuid != currentEpisodeUuid }
        }

        if (episode != null) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Auto play selected episode=${episode.uuid} from source=$source")
        }

        return episode?.let { it to source }
    }

    private suspend fun findPodcastEpisodes(
        podcast: Podcast,
        currentEpisodeUuid: String?,
    ): List<PodcastEpisode> {
        val episodes = episodeManager
            .findEpisodesByPodcastOrderedSuspend(podcast)
            .filter { episode ->
                (!episode.isArchived && !episode.isFinished) ||
                    episode.uuid == currentEpisodeUuid
            }

        return withContext(Dispatchers.Default) {
            val modifiedEpisodes = when (podcast.grouping) {
                PodcastGrouping.None, PodcastGrouping.Starred, PodcastGrouping.Season -> episodes

                PodcastGrouping.Downloaded -> episodes.map { episode ->
                    if (episode.uuid == currentEpisodeUuid) {
                        episode.copy(downloadStatus = EpisodeDownloadStatus.Downloaded)
                    } else {
                        episode
                    }
                }

                PodcastGrouping.Unplayed -> episodes.map { episode ->
                    if (episode.uuid == currentEpisodeUuid) {
                        episode.copy(playingStatus = EpisodePlayingStatus.NOT_PLAYED)
                    } else {
                        episode
                    }
                }
            }

            podcast.grouping
                .formGroups(modifiedEpisodes, podcast, context.resources)
                .flatten()
        }
    }

    private suspend fun findPlaylistEpisodes(
        playlistUuid: String,
        currentEpisodeUuid: String?,
    ): List<PodcastEpisode> {
        return playlistManager
            .getAutoPlayEpisodes(playlistUuid, currentEpisodeUuid)
            .filter { episode ->
                (!episode.isArchived && !episode.isFinished) ||
                    episode.uuid == currentEpisodeUuid
            }
    }

    private suspend fun findDownloadedEpisodes(): List<PodcastEpisode> {
        return episodeManager.findDownloadedEpisodesRxFlowable().awaitFirst()
    }

    private suspend fun findUserEpisodes(): List<UserEpisode> {
        return cloudFilesManager.sortedCloudFiles.first()
    }

    private suspend fun findStarredEpisodes(): List<PodcastEpisode> {
        return episodeManager.findStarredEpisodes()
    }
}
