package au.com.shiftyjelly.pocketcasts.reimagine.timestamp

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.reimagine.FakeTracker
import au.com.shiftyjelly.pocketcasts.reimagine.TrackEvent
import au.com.shiftyjelly.pocketcasts.reimagine.timestamp.ShareEpisodeTimestampViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ShareEpisodeTimestampViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val tracker = FakeTracker()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()

    private val episode = PodcastEpisode(uuid = "episode-id", podcastUuid = "podcast-id", publishedDate = Date())
    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")

    private lateinit var viewModel: ShareEpisodeTimestampViewModel

    @Before
    fun setUp() {
        whenever(episodeManager.observeByUuid("episode-id")).thenReturn(flowOf(episode))
        whenever(podcastManager.observePodcastByEpisodeUuid("episode-id")).thenReturn(flowOf(podcast))
        val artworkSetting = mock<UserSetting<ArtworkConfiguration>>()
        whenever(artworkSetting.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = true)))
        whenever(settings.artworkConfiguration).thenReturn(artworkSetting)

        viewModel = ShareEpisodeTimestampViewModel(
            podcast.uuid,
            episode.uuid,
            SourceView.PLAYER,
            episodeManager,
            podcastManager,
            settings,
            AnalyticsTracker.test(tracker, isEnabled = true),
        )
    }

    @Test
    fun `get UI state`() = runTest {
        viewModel.uiState.test {
            assertEquals(
                UiState(
                    podcast = podcast,
                    episode = episode,
                    useEpisodeArtwork = true,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `track screen show event`() = runTest {
        viewModel.onScreenShown()

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_SHOWN,
                mapOf(
                    "type" to "episode_timestamp",
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "source" to "player",
                ),
            ),
            event,
        )
    }
}
