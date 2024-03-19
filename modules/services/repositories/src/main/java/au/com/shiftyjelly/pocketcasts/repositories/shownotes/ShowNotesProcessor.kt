package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ShowNotesProcessor @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val episodeManager: EpisodeManager,
) {
    fun process(showNotes: ShowNotesResponse) {
        scope.launch {
            updateImageUrls(showNotes)
        }
    }

    private suspend fun updateImageUrls(showNotes: ShowNotesResponse) {
        val imageUrlUpdates = showNotes.podcast?.episodes?.mapNotNull { episodeShowNotes ->
            episodeShowNotes.image?.let { image -> ImageUrlUpdate(episodeShowNotes.uuid, image) }
        }
        imageUrlUpdates?.let { episodeManager.updateImageUrls(it) }
    }
}
