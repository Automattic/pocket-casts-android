package au.com.shiftyjelly.pocketcasts.clip

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ShareClipViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val clipPlayer = FakeClipPlayer()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()

    private val episode = PodcastEpisode(uuid = "episode-id", publishedDate = Date())
    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")

    private val episodeFlow = MutableStateFlow(episode)
    private val podcastFlow = MutableStateFlow(podcast)

    private lateinit var viewModel: ShareClipViewModel

    @Before
    fun setUp() {
        whenever(episodeManager.observeByUuid("episode-id")).thenReturn(episodeFlow)
        whenever(podcastManager.observePodcastByEpisodeUuid("episode-id")).thenReturn(podcastFlow)
        val artworkSetting = mock<UserSetting<ArtworkConfiguration>>()
        whenever(artworkSetting.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = true)))
        whenever(settings.artworkConfiguration).thenReturn(artworkSetting)

        viewModel = ShareClipViewModel(
            "episode-id",
            clipPlayer,
            episodeManager,
            podcastManager,
            settings,
        )
    }

    @Test
    fun `play clip`() = runTest {
        viewModel.playClip()

        assertEquals(Clip(episode, Clip.Range(15.seconds, 30.seconds)), clipPlayer.clips.awaitItem())
    }

    @Test
    fun `update play state`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().isPlaying)

            viewModel.playClip()
            assertTrue(awaitItem().isPlaying)

            viewModel.stopClip()
            assertFalse(awaitItem().isPlaying)
        }
    }
}
