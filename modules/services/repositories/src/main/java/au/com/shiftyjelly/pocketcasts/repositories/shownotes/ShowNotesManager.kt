package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.repositories.podcast.LoadTranscriptSource
import au.com.shiftyjelly.pocketcasts.servers.ShowNotesServiceManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ShowNotesManager @Inject constructor(
    private val showNotesServiceManager: ShowNotesServiceManager,
    private val showNotesProcessor: ShowNotesProcessor,
) {

    fun loadShowNotesFlow(podcastUuid: String, episodeUuid: String): Flow<ShowNotesState> =
        showNotesServiceManager.loadShowNotesFlow(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            processShowNotes = { showNotesProcessor.process(episodeUuid, it) },
        )

    suspend fun loadShowNotes(podcastUuid: String, episodeUuid: String): ShowNotesState =
        showNotesServiceManager.loadShowNotes(
            podcastUuid = podcastUuid,
            episodeUuid = episodeUuid,
            processShowNotes = { showNotesProcessor.process(episodeUuid, it) },
        )

    suspend fun downloadToCacheShowNotes(podcastUuid: String, episodeUuid: String) {
        showNotesServiceManager.downloadToCacheShowNotes(
            podcastUuid = podcastUuid,
            processShowNotes = { showNotesProcessor.process(episodeUuid, it, LoadTranscriptSource.DOWNLOAD_EPISODE) },
        )
    }
}
