package au.com.shiftyjelly.pocketcasts.podcasts.view

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListFragment.Mode
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListViewModel.State
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.images.R as IR

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEpisodeListViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val episodeManager: EpisodeManager = mock()
    private val playbackManager: PlaybackManager = mock()
    private val analyticsTracker: AnalyticsTracker = mock()

    private val downloadedEpisodesMock = listOf(mock<PodcastEpisode>())
    private val starredEpisodesMock = listOf(mock<PodcastEpisode>())
    private val listeningHistoryEpisodesMock = listOf(mock<PodcastEpisode>())

    private lateinit var viewModel: ProfileEpisodeListViewModel

    @Test
    fun `setup with Downloaded mode updates state with downloaded episodes`() = runTest {
        initViewModel()

        viewModel.setup(Mode.Downloaded)

        viewModel.state.test {
            assertEquals(downloadedEpisodesMock, (awaitItem() as State.Loaded).results)
        }
    }

    @Test
    fun `setup with Starred mode updates state with starred episodes`() = runTest {
        initViewModel()

        viewModel.setup(Mode.Starred)

        viewModel.state.test {
            assertEquals(starredEpisodesMock, (awaitItem() as State.Loaded).results)
        }
    }

    @Test
    fun `setup with History mode updates state with playback history episodes`() = runTest {
        initViewModel()

        viewModel.setup(Mode.History)

        viewModel.state.test {
            assertEquals(listeningHistoryEpisodesMock, (awaitItem() as State.Loaded).results)
        }
    }

    @Test
    fun `empty state is shown if no listening history available`() = runTest {
        initViewModel(listeningHistoryEpisodes = emptyList())

        viewModel.setup(Mode.History)

        viewModel.state.test {
            assertEquals(
                awaitItem(),
                State.Empty(
                    titleRes = R.string.profile_empty_history,
                    summaryRes = R.string.profile_empty_history_summary,
                    iconRes = IR.drawable.ic_listen_history,
                ),
            )
        }
    }

    @Test
    fun `search bar is shown for listening history`() = runTest {
        initViewModel()

        viewModel.setup(Mode.History)

        viewModel.state.test {
            assertEquals(true, (awaitItem() as State.Loaded).showSearchBar)
        }
    }

    @Test
    fun `search bar not shown for starred mode`() = runTest {
        initViewModel()

        viewModel.setup(Mode.Starred)

        viewModel.state.test {
            assertEquals(false, (awaitItem() as State.Loaded).showSearchBar)
        }
    }

    @Test
    fun `search bar not shown for download mode`() = runTest {
        initViewModel()

        viewModel.setup(Mode.Downloaded)

        viewModel.state.test {
            assertEquals(false, (awaitItem() as State.Loaded).showSearchBar)
        }
    }

    @Test
    fun `onSearchQueryChanged updates search query flow`() = runTest {
        initViewModel()

        val searchQuery = "test query"
        viewModel.onSearchQueryChanged(searchQuery)

        viewModel.searchQueryFlow.test {
            assertEquals(searchQuery, awaitItem())
        }
    }

    @Test
    fun `search returns filtered playback history episodes`() = runTest {
        initViewModel()
        val filteredEpisodes = listOf(mock<PodcastEpisode>())
        whenever(episodeManager.filteredPlaybackHistoryEpisodesFlow("query")).thenReturn(flowOf(filteredEpisodes))
        viewModel.setup(Mode.History)

        viewModel.onSearchQueryChanged("query")

        viewModel.state.test {
            assertEquals(filteredEpisodes, (awaitItem() as State.Loaded).results)
        }
    }

    @Test
    fun `playback history records are not filtered on setup when search query is empty`() = runTest {
        initViewModel()

        viewModel.setup(Mode.History)

        verify(episodeManager, never()).filteredPlaybackHistoryEpisodesFlow("")
    }

    @Test
    fun `empty state is shown if no search results available`() = runTest {
        initViewModel()
        val filteredEpisodes = emptyList<PodcastEpisode>()
        whenever(episodeManager.filteredPlaybackHistoryEpisodesFlow("query")).thenReturn(flowOf(filteredEpisodes))
        viewModel.setup(Mode.History)

        viewModel.onSearchQueryChanged("query")

        viewModel.state.test {
            assertEquals(
                awaitItem(),
                State.Empty(
                    titleRes = R.string.search_episodes_not_found_title,
                    summaryRes = R.string.search_episodes_not_found_summary,
                    showSearchBar = true,
                    iconRes = IR.drawable.ic_listen_history,
                ),
            )
        }
    }

    private fun initViewModel(
        downloadedEpisodes: List<PodcastEpisode> = downloadedEpisodesMock,
        starredEpisodes: List<PodcastEpisode> = starredEpisodesMock,
        listeningHistoryEpisodes: List<PodcastEpisode> = listeningHistoryEpisodesMock,
    ) {
        val settings = mock<Settings>()
        val userManager = mock<UserManager>()
        val bannerSetting = mock<UserSetting<Boolean>>()

        whenever(episodeManager.findDownloadEpisodesRxFlowable()).thenReturn(flowOf(downloadedEpisodes).asFlowable())
        whenever(episodeManager.findStarredEpisodesRxFlowable()).thenReturn(flowOf(starredEpisodes).asFlowable())
        whenever(episodeManager.findPlaybackHistoryEpisodesRxFlowable()).thenReturn(flowOf(listeningHistoryEpisodes).asFlowable())
        whenever(episodeManager.filteredPlaybackHistoryEpisodesFlow(anyOrNull())).thenReturn(flowOf(emptyList()))
        whenever(userManager.getSignInState()).thenReturn(Flowable.empty())
        whenever(settings.isFreeAccountHistoryBannerDismissed).thenReturn(bannerSetting)
        whenever(bannerSetting.flow).thenReturn(MutableStateFlow(false))
        doNothing().whenever(analyticsTracker).track(any(), any())

        viewModel = ProfileEpisodeListViewModel(
            episodeManager = episodeManager,
            playbackManager = playbackManager,
            analyticsTracker = analyticsTracker,
            settings = settings,
            userManager = userManager,
        )
    }
}
