package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import kotlinx.coroutines.flow.Flow
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject

class ShowNotesManager @Inject constructor(
    private val serverShowNotesManager: ServerShowNotesManager,
    private val episodeManager: EpisodeManager,
) {

    fun loadShowNotesFlow(podcastUuid: String, episodeUuid: String): Flow<ShowNotesState> =
        serverShowNotesManager.loadShowNotesFlow(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            persistImageUrls = ::updateEpisodesWithImageUrls
        )

    suspend fun loadShowNotes(podcastUuid: String, episodeUuid: String): ShowNotesState =
        serverShowNotesManager.loadShowNotes(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            persistImageUrls = ::updateEpisodesWithImageUrls
        )

    suspend fun downloadToCacheShowNotes(podcastUuid: String) {
        serverShowNotesManager.downloadToCacheShowNotes(
            podcastUuid = podcastUuid,
            persistImageUrls = ::updateEpisodesWithImageUrls,
        )
    }

    @VisibleForTesting
    internal suspend fun updateEpisodesWithImageUrls(showNotesResponse: ShowNotesResponse) {
        showNotesResponse.podcast?.episodes?.mapNotNull { showNotesEpisode ->
            showNotesEpisode.image?.let { image ->
                ImageUrlUpdate(showNotesEpisode.uuid, image)
            }
        }?.let { episodeManager.updateImageUrls(it) }
    }
}
