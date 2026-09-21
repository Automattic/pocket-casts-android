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
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPromotion
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.io.IOException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class PodcastListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val colorManager = mock<ColorManager>()
    private val podcastManager = mock<PodcastManager>()
    private val userManager = mock<UserManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()

    private val subscribedUuids = MutableStateFlow(emptyList<String>())
    private val playbackState = MutableStateFlow(PlaybackState())

    @Before
    fun setUp() {
        whenever(podcastManager.podcastSubscriptionsFlow()).thenReturn(subscribedUuids)
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackState)
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

        val state = viewModel.currentState<PodcastListViewState.ListLoaded>()
        assertEquals("Relay", state.feed.title)
        assertEquals(listOf(RELAY_PODCAST.uuid), state.feed.podcasts?.map { it.uuid })
    }

    @Test
    fun `a feed that fails to load moves to the error state`() {
        // the repository swallows the exception and returns null
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn null }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        viewModel.currentState<PodcastListViewState.Error>()
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
        viewModel.currentState<PodcastListViewState.Error>()

        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        viewModel.retry()

        assertEquals("Relay", viewModel.currentState<PodcastListViewState.ListLoaded>().feed.title)
    }

    @Test
    fun `reloading a loaded page does not drop back to the loading state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        val viewModel = createViewModel()
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)
        viewModel.currentState<PodcastListViewState.ListLoaded>()
        val states = viewModel.recordStates()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        assertTrue(states.all { it is PodcastListViewState.ListLoaded })
    }

    @Test
    fun `a reload that cancels a pending request does not show the error state`() {
        // like the real repository, a cancelled request comes back as null rather than throwing
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doSuspendableAnswer { runCatching { awaitCancellation() }.getOrNull() } }
        val viewModel = createViewModel()
        val states = viewModel.recordStates()
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        assertTrue(states.none { it is PodcastListViewState.Error })
        viewModel.currentState<PodcastListViewState.ListLoaded>()
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
    fun `a ranked list colours its first podcast with the downloaded artwork colour`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST.copy()) }
        colorManager.stub { on { downloadColors(RELAY_PODCAST.uuid) } doReturn ArtworkColors(background = ARTWORK_BACKGROUND) }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.RankedList(), authenticated = false)

        val state = viewModel.currentState<PodcastListViewState.ListLoaded>()
        assertEquals(ARTWORK_BACKGROUND, state.feed.podcasts?.first()?.color)
    }

    @Test
    fun `a ranked list without artwork colours loads with the default colour`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST.copy()) }
        colorManager.stub { on { downloadColors(RELAY_PODCAST.uuid) } doReturn null }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.RankedList(), authenticated = false)

        val state = viewModel.currentState<PodcastListViewState.ListLoaded>()
        assertEquals(0, state.feed.podcasts?.first()?.color)
        verifyBlocking(colorManager) { downloadColors(RELAY_PODCAST.uuid) }
    }

    @Test
    fun `a ranked list whose artwork colours fail to download moves to the error state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST.copy()) }
        colorManager.stub { on { downloadColors(RELAY_PODCAST.uuid) } doSuspendableAnswer { throw IOException("offline") } }
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.RankedList(), authenticated = false)

        viewModel.currentState<PodcastListViewState.Error>()
    }

    @Test
    fun `subscribed podcasts and promotions follow the subscription list`() {
        val promotion = DiscoverPromotion(promotionUuid = "promotion", podcastUuid = RELAY_PODCAST.uuid, title = "Promo", description = "")
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST).copy(promotion = promotion) }
        subscribedUuids.value = listOf(RELAY_PODCAST.uuid)
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        val subscribedFeed = viewModel.currentState<PodcastListViewState.ListLoaded>().feed
        assertTrue(subscribedFeed.podcasts?.single()?.isSubscribed == true)
        assertTrue(subscribedFeed.promotion?.isSubscribed == true)

        subscribedUuids.value = emptyList()

        val unsubscribedFeed = viewModel.currentState<PodcastListViewState.ListLoaded>().feed
        assertFalse(unsubscribedFeed.podcasts?.single()?.isSubscribed == true)
        assertFalse(unsubscribedFeed.promotion?.isSubscribed == true)
    }

    @Test
    fun `the playing episode is marked as playing until it pauses`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed().copy(episodes = listOf(RELAY_EPISODE)) }
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = RELAY_EPISODE.uuid)
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        assertTrue(viewModel.currentState<PodcastListViewState.ListLoaded>().feed.episodes?.single()?.isPlaying == true)

        playbackState.value = playbackState.value.copy(state = PlaybackState.State.PAUSED)

        assertFalse(viewModel.currentState<PodcastListViewState.ListLoaded>().feed.episodes?.single()?.isPlaying == true)
    }

    @Test
    fun `playback progress does not redraw the list`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed().copy(episodes = listOf(RELAY_EPISODE)) }
        playbackState.value = PlaybackState(state = PlaybackState.State.PLAYING, episodeUuid = RELAY_EPISODE.uuid)
        val viewModel = createViewModel()
        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)
        val states = viewModel.recordStates()

        playbackState.value = playbackState.value.copy(positionMs = 30_000)

        // observing replays the current state once, so only that one is expected
        assertEquals(1, states.size)
    }

    @Test
    fun `a subscription list that fails moves to the error state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn listFeed(RELAY_PODCAST) }
        whenever(podcastManager.podcastSubscriptionsFlow()).thenReturn(flow { throw IOException("database closed") })
        val viewModel = createViewModel()

        viewModel.load(sourceUrl = RELAY_URL, listStyle = ExpandedStyle.NetworkGrid(), authenticated = false)

        viewModel.currentState<PodcastListViewState.Error>()
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
        ioDispatcher = coroutineRule.testDispatcher,
    )

    private inline fun <reified T : PodcastListViewState> PodcastListViewModel.currentState(): T {
        val current = state.value
        assertTrue("Expected ${T::class.simpleName} but was $current", current is T)
        return current as T
    }

    private fun PodcastListViewModel.recordStates(): List<PodcastListViewState> {
        val states = mutableListOf<PodcastListViewState>()
        state.observeForever(Observer { states += it })
        return states
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
        private const val ARTWORK_BACKGROUND = 0xFF123456.toInt()
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
        private val RELAY_EPISODE = DiscoverEpisode(
            uuid = "8e2e2f50-3d9b-4f0f-9d6a-1c6f3b2d7a10",
            title = "Episode 1",
            url = null,
            published = null,
            duration = null,
            fileType = null,
            size = null,
            podcast_uuid = "d041df50-4850-0132-cb49-5f4c86fd3263",
            podcast_title = "Analog(ue)",
            type = null,
            season = null,
            number = null,
        )
    }
}
