package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesPodcast
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class ShowNotesProcessTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val episodeManager = mock<EpisodeManager>()

    @Test
    fun `update episodes with image URLs`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager)
        val episodeWithImage1 = ShowNotesEpisode(
            uuid = "episode_uuid1",
            showNotes = "show_notes1",
            image = "image1",
        )
        val episodeWithImage2 = ShowNotesEpisode(
            uuid = "episode_uuid2",
            showNotes = "show_notes2",
            image = "image2",
        )
        val episodeWithoutImage = ShowNotesEpisode(
            uuid = "episode_uuid3",
            showNotes = "show_notes3",
            image = null,
        )

        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast_uuid1",
                episodes = listOf(
                    episodeWithImage1,
                    episodeWithImage2,
                    episodeWithoutImage,
                ),
            ),
        )

        processor.process(showNotesResponse)

        val imageUrlUpdateForEpisode = { episode: ShowNotesEpisode ->
            ImageUrlUpdate(
                episodeUuid = episode.uuid,
                imageUrl = episode.image!!,
            )
        }
        val expectedUpdates = listOf(
            imageUrlUpdateForEpisode(episodeWithImage1),
            imageUrlUpdateForEpisode(episodeWithImage2),
        )

        verify(episodeManager).updateImageUrls(expectedUpdates)
    }
}
