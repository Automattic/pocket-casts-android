package au.com.shiftyjelly.pocketcasts.wear.ui

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class WatchListScreenViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()
    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    private lateinit var viewModel: WatchListScreenViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = WatchListScreenViewModel(analyticsTracker, episodeManager, playbackManager, podcastManager)
    }

    @Test
    fun `test onNowPlayingClicked tapped`() {
        viewModel.onNowPlayingClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_NOW_PLAYING_TAPPED)
    }

    @Test
    fun `test onPodcastsClicked tapped`() {
        viewModel.onPodcastsClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_PODCASTS_TAPPED)
    }

    @Test
    fun `test onDownloadsClicked tapped`() {
        viewModel.onDownloadsClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_DOWNLOADS_TAPPED)
    }

    @Test
    fun `test onFiltersClicked tapped`() {
        viewModel.onFiltersClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_FILTERS_TAPPED)
    }

    @Test
    fun `test onFilesClicked tapped`() {
        viewModel.onFilesClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_FILES_TAPPED)
    }

    @Test
    fun `test onSettingsClicked tapped`() {
        viewModel.onSettingsClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_SETTINGS_TAPPED)
    }
}
