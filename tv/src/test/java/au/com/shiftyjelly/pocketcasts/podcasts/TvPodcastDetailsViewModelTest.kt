package au.com.shiftyjelly.pocketcasts.podcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.TvPreferences
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvPodcastDetailsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcast = Podcast(uuid = "podcast-uuid", title = "Podcast")
    private val availableEpisode = episode(uuid = "episode-1", isArchived = false)
    private val archivedEpisode = episode(uuid = "episode-2", isArchived = true)

    private val episodes = MutableSharedFlow<List<PodcastEpisode>>(replay = 1)
    private val podcastManager = mock<PodcastManager> {
        on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
        on { podcastByUuidFlow(any()) } doReturn MutableStateFlow(podcast)
    }
    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodesByPodcastOrderedFlow(any()) } doReturn episodes
    }
    private val preferences = mock<TvPreferences>()
    private val loggedIn = BehaviorRelay.createDefault(false)
    private val syncManager = mock<SyncManager> {
        on { isLoggedInObservable } doReturn loggedIn
    }

    @Test
    fun `archived episodes are hidden by default`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(podcast, state.podcast)
            assertEquals(listOf(availableEpisode), state.episodes)
            assertEquals(1, state.archivedEpisodeCount)
            assertEquals(false, state.isShowingArchived)
        }
    }

    @Test
    fun `archived episodes are shown when the stored preference allows them`() = runTest {
        val preferences = mock<TvPreferences> {
            on { isPodcastShowingArchived("podcast-uuid") } doReturn true
        }
        val viewModel = createViewModel(preferences)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode, archivedEpisode), state.episodes)
            assertEquals(true, state.isShowingArchived)
        }
    }

    @Test
    fun `a podcast with only archived episodes reports the archived count`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(archivedEpisode))

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(emptyList<PodcastEpisode>(), state.episodes)
            assertEquals(1, state.archivedEpisodeCount)
        }
    }

    @Test
    fun `toggling the archive filter updates the list and persists the preference`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))
            assertEquals(listOf(availableEpisode), (awaitItem() as TvPodcastDetailsUiState.Loaded).episodes)

            viewModel.toggleArchiveFilter()

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode, archivedEpisode), state.episodes)
            assertEquals(true, state.isShowingArchived)
            verify(preferences).setPodcastShowingArchived(eq("podcast-uuid"), eq(true))
        }
    }

    @Test
    fun `toggling the archive filter off hides archived episodes again`() = runTest {
        val preferences = mock<TvPreferences> {
            on { isPodcastShowingArchived("podcast-uuid") } doReturn true
        }
        val viewModel = createViewModel(preferences)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())

            episodes.emit(listOf(availableEpisode, archivedEpisode))
            assertEquals(listOf(availableEpisode, archivedEpisode), (awaitItem() as TvPodcastDetailsUiState.Loaded).episodes)

            viewModel.toggleArchiveFilter()

            val state = awaitItem() as TvPodcastDetailsUiState.Loaded
            assertEquals(listOf(availableEpisode), state.episodes)
            assertEquals(false, state.isShowingArchived)
            verify(preferences).setPodcastShowingArchived(eq("podcast-uuid"), eq(false))
        }
    }

    @Test
    fun `a sort order change re-runs the episodes query`() = runTest {
        val podcastFlow = MutableStateFlow(podcast.copy(episodesSortType = EpisodesSortType.EPISODES_SORT_BY_DATE_DESC))
        val podcastManager = mock<PodcastManager> {
            on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
            on { podcastByUuidFlow(any()) } doReturn podcastFlow
        }
        val viewModel = createViewModel(podcastManager = podcastManager)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded

            podcastFlow.value = podcastFlow.value.copy(episodesSortType = EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)

            cancelAndConsumeRemainingEvents()
        }

        verify(episodeManager, times(2)).findEpisodesByPodcastOrderedFlow(any())
    }

    @Test
    fun `a non-sort podcast change does not re-run the episodes query`() = runTest {
        val podcastFlow = MutableStateFlow(podcast.copy(episodesSortType = EpisodesSortType.EPISODES_SORT_BY_DATE_DESC))
        val podcastManager = mock<PodcastManager> {
            on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(podcast)
            on { podcastByUuidFlow(any()) } doReturn podcastFlow
        }
        val viewModel = createViewModel(podcastManager = podcastManager)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded

            podcastFlow.value = podcastFlow.value.copy(title = "Renamed podcast")

            cancelAndConsumeRemainingEvents()
        }

        verify(episodeManager, times(1)).findEpisodesByPodcastOrderedFlow(any())
    }

    @Test
    fun `changing the sort type persists it to the podcast`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded
        }

        viewModel.changeSortType(EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)

        verify(podcastManager).updateEpisodesSortTypeBlocking(Podcast(uuid = "podcast-uuid"), EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)
    }

    @Test
    fun `a podcast that cannot be resolved maps to the not found state`() = runTest {
        val podcastManager = mock<PodcastManager> {
            on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.error(RuntimeException("boom"))
        }
        val viewModel = createViewModel(podcastManager = podcastManager)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.NotFound, expectMostRecentItem())
        }
    }

    @Test
    fun `the logged in state is reflected in the loaded state`() = runTest {
        loggedIn.accept(true)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            assertEquals(true, (awaitItem() as TvPodcastDetailsUiState.Loaded).isLoggedIn)
        }
    }

    @Test
    fun `toggling an unsubscribed podcast follows it`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded

            viewModel.toggleSubscribe()

            cancelAndConsumeRemainingEvents()
        }

        verify(podcastManager).subscribeToPodcast(podcastUuid = "podcast-uuid", sync = true)
    }

    @Test
    fun `toggling a subscribed podcast unfollows it`() = runTest {
        val subscribedPodcast = podcast.copy(isSubscribed = true)
        val podcastManager = mock<PodcastManager> {
            on { findOrDownloadPodcastRxSingle(any(), any()) } doReturn Single.just(subscribedPodcast)
            on { podcastByUuidFlow(any()) } doReturn MutableStateFlow(subscribedPodcast)
        }
        val viewModel = createViewModel(podcastManager = podcastManager)

        viewModel.uiState.test {
            assertEquals(TvPodcastDetailsUiState.Loading, awaitItem())
            episodes.emit(listOf(availableEpisode))
            awaitItem() as TvPodcastDetailsUiState.Loaded

            viewModel.toggleSubscribe()

            cancelAndConsumeRemainingEvents()
        }
        advanceUntilIdle()

        verifyBlocking(podcastManager) { unsubscribe("podcast-uuid", SourceView.PODCAST_SCREEN) }
    }

    @Test
    fun `starting account auth drives the account state to complete`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(deviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(any(), any())).thenReturn(loginSuccess())
        val viewModel = createViewModel()

        viewModel.accountAuthState.test {
            assertEquals(TvSignInUiState.Loading, awaitItem())

            viewModel.startAccountAuth()

            val states = mutableListOf<TvSignInUiState>()
            while (states.lastOrNull() !is TvSignInUiState.Complete) {
                states.add(awaitItem())
            }
            assertTrue(states.any { it is TvSignInUiState.Ready })
            assertEquals(TvSignInUiState.Complete, states.last())
        }
        advanceUntilIdle()

        verify(podcastManager).subscribeToPodcast(podcastUuid = "podcast-uuid", sync = true)
        verify(podcastManager).refreshPodcastsAfterSignIn()
    }

    private fun deviceAuthorizeResponse() = DeviceAuthorizeResponse(
        deviceCode = "device-code",
        userCode = "ABC123",
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?code=ABC123",
        expiresIn = 1800,
        interval = 1,
    )

    private fun loginSuccess() = LoginResult.Success(
        AuthResultModel(
            token = AccessToken("access-token"),
            uuid = "user-uuid",
            isNewAccount = false,
        ),
    )

    private fun createViewModel(
        prefs: TvPreferences = preferences,
        podcastManager: PodcastManager = this.podcastManager,
    ) = TvPodcastDetailsViewModel(
        podcastUuid = "podcast-uuid",
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        syncManager = syncManager,
        preferences = prefs,
        defaultDispatcher = coroutineRule.testDispatcher,
        ioDispatcher = coroutineRule.testDispatcher,
    )

    private fun episode(uuid: String, isArchived: Boolean) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(0),
        isArchived = isArchived,
    )
}
