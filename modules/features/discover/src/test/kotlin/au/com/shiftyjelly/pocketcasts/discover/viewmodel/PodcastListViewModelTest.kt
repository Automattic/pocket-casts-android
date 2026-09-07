package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import au.com.shiftyjelly.pocketcasts.discover.view.PodcastGridListFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class PodcastListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val listRepository = mock<ListRepository>()
    private val colorManager = mock<ColorManager>()
    private val podcastManager = mock<PodcastManager>()
    private val userManager = mock<UserManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()

    @Before
    fun setUp() {
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
        // the init handler keeps AndroidSchedulers from reaching for a main Looper that does not exist here
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { Schedulers.trampoline() }

        whenever(podcastManager.getSubscribedPodcastUuidsRxSingle()).thenReturn(Single.just(emptyList()))
        whenever(podcastManager.podcastSubscriptionsRxFlowable()).thenReturn(Flowable.empty())
        whenever(playbackManager.playbackStateRelay).thenReturn(BehaviorRelay.createDefault(PlaybackState()).toSerialized())
    }

    @After
    fun tearDown() {
        RxJavaPlugins.reset()
        RxAndroidPlugins.reset()
    }

    @Test
    fun `a new view model starts in the loading state`() {
        assertTrue(createViewModel().state.value is PodcastListViewState.Loading)
    }

    @Test
    fun `a feed that loads moves to the loaded state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        val state = viewModel.awaitState<PodcastListViewState.ListLoaded>()
        assertEquals("Relay", state.feed.title)
        assertEquals(listOf(RELAY_PODCAST.uuid), state.feed.podcasts?.map { it.uuid })
    }

    @Test
    fun `a feed that fails to load moves to the error state`() {
        // the repository swallows the exception and returns null, which empties the stream
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn null }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        viewModel.awaitState<PodcastListViewState.Error>()
    }

    @Test
    fun `loading without a source url moves to the error state`() {
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = null, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        assertTrue(viewModel.state.value is PodcastListViewState.Error)
    }

    @Test
    fun `retrying after an error reloads the last requested feed`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn null }
        val viewModel = createViewModel()
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)
        viewModel.awaitState<PodcastListViewState.Error>()

        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        viewModel.retry()

        assertEquals("Relay", viewModel.awaitState<PodcastListViewState.ListLoaded>().feed.title)
    }

    @Test
    fun `reloading a loaded page does not drop back to the loading state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        val viewModel = createViewModel()
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)
        viewModel.awaitState<PodcastListViewState.ListLoaded>()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        assertTrue(viewModel.state.value is PodcastListViewState.ListLoaded)
    }

    @Test
    fun `retrying an unusable source url does not re-run the failed load`() {
        val viewModel = createViewModel()
        viewModel.load(sourceUrl = null, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        viewModel.retry()

        assertTrue(viewModel.state.value is PodcastListViewState.Error)
        verifyNoInteractions(listRepository)
    }

    @Test
    fun `retrying before anything has been loaded does nothing`() {
        val viewModel = createViewModel()

        viewModel.retry()

        assertTrue(viewModel.state.value is PodcastListViewState.Loading)
    }

    @Test
    fun `a list id becomes a list feed url`() {
        assertEquals("${Settings.SERVER_LIST_URL}/$RELAY_LIST_ID.json", PodcastGridListFragment.listUrl(RELAY_LIST_ID))
    }

    private fun createViewModel() = PodcastListViewModel(
        listRepository = listRepository,
        colorManager = colorManager,
        podcastManager = podcastManager,
        userManager = userManager,
        episodeManager = episodeManager,
        playbackManager = playbackManager,
    )

    /** The feed is fetched on a coroutine dispatcher the test does not control, so states are awaited rather than read. */
    private inline fun <reified T : PodcastListViewState> PodcastListViewModel.awaitState(): T {
        val latch = CountDownLatch(1)
        var matched: T? = null
        val observer = Observer<PodcastListViewState> { state ->
            if (state is T && matched == null) {
                matched = state
                latch.countDown()
            }
        }
        state.observeForever(observer)
        try {
            assertTrue("Timed out waiting for ${T::class.simpleName}", latch.await(5, TimeUnit.SECONDS))
        } finally {
            state.removeObserver(observer)
        }
        return checkNotNull(matched)
    }

    private fun listFeed(vararg podcasts: DiscoverPodcast) = ListFeed(
        title = "Relay",
        subtitle = null,
        description = "The Relay network of podcasts.",
        shortDescription = null,
        date = null,
        podcasts = podcasts.toList(),
        episodes = null,
        podroll = null,
        collectionImageUrl = null,
        collectionRectangleImageUrl = null,
        featureImage = null,
        headerImageUrl = null,
        tintColors = null,
        collageImages = null,
        webLinkUrl = null,
        webLinkTitle = null,
        promotion = null,
        listId = RELAY_LIST_ID,
        expandedStyle = ExpandedStyle.NetworkGrid(),
    )

    companion object {
        private const val RELAY_LIST_ID = "cdb75bc0-9f5a-4217-b1ca-f573821a7913"
        private val RELAY_URL = "https://lists.pocketcasts.net/$RELAY_LIST_ID.json"
        private val RELAY_PODCAST = DiscoverPodcast(
            uuid = "d041df50-4850-0132-cb49-5f4c86fd3263",
            title = "Analog(ue)",
            url = null,
            author = "Relay",
            category = null,
            description = null,
            language = null,
            mediaType = null,
        )
    }
}
