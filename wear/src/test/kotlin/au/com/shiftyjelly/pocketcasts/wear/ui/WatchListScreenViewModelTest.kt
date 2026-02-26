package au.com.shiftyjelly.pocketcasts.wear.ui

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WatchListScreenViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var upNextQueue: UpNextQueue

    private val refreshStateFlow = MutableStateFlow<RefreshState>(RefreshState.Never)

    private lateinit var viewModel: WatchListScreenViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(settings.refreshStateFlow).thenReturn(refreshStateFlow)
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager))
            .thenReturn(Observable.never())
        viewModel = WatchListScreenViewModel(analyticsTracker, settings, episodeManager, playbackManager, podcastManager)
    }

    @After
    fun tearDown() = runTest {
        testScheduler.advanceUntilIdle()
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
    fun `test onPlaylistsClicked tapped`() {
        viewModel.onPlaylistsClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_FILTERS_TAPPED)
    }

    @Test
    fun `test onFilesClicked tapped`() {
        viewModel.onFilesClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_FILES_TAPPED)
    }

    @Test
    fun `test onStarredClicked tapped`() {
        viewModel.onStarredClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_STARRED_TAPPED)
    }

    @Test
    fun `test onSettingsClicked tapped`() {
        viewModel.onSettingsClicked()
        verify(analyticsTracker).track(AnalyticsEvent.WEAR_MAIN_LIST_SETTINGS_TAPPED)
    }

    @Test
    fun `refreshPodcasts calls podcastManager refreshPodcasts`() {
        viewModel.refreshPodcasts()
        verify(podcastManager).refreshPodcasts("watch - list screen")
    }

    @Test
    fun `refreshPodcasts does not call podcastManager when already refreshing`() = runTest {
        refreshStateFlow.value = RefreshState.Refreshing
        testScheduler.advanceUntilIdle()

        viewModel.refreshPodcasts()
        verify(podcastManager, never()).refreshPodcasts("watch - list screen")
    }

    @Test
    fun `state reflects refreshState from settings flow`() = runTest {
        refreshStateFlow.value = RefreshState.Refreshing
        testScheduler.advanceUntilIdle()

        assertEquals(RefreshState.Refreshing, viewModel.state.value.refreshState)
    }
}
