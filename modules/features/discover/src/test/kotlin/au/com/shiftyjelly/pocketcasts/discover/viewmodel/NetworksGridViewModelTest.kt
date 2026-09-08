package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class NetworksGridViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()

    @Test
    fun `a new view model starts in the loading state`() {
        assertTrue(NetworksGridViewModel(listRepository).state.value is NetworksGridViewModel.UiState.Loading)
    }

    @Test
    fun `a feed that loads exposes its networks`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed(RELAY, NYT) }
        val viewModel = NetworksGridViewModel(listRepository)

        viewModel.load(NETWORKS_URL)

        val state = viewModel.state.value as NetworksGridViewModel.UiState.Loaded
        assertEquals(listOf("Relay", "The New York Times"), state.networks.map { it.title })
    }

    @Test
    fun `entries that are not podcast lists are left out`() {
        val episodeList = RELAY.copy(uuid = "episodes", type = ListType.EpisodeList)
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed(RELAY, episodeList) }
        val viewModel = NetworksGridViewModel(listRepository)

        viewModel.load(NETWORKS_URL)

        val state = viewModel.state.value as NetworksGridViewModel.UiState.Loaded
        assertEquals(listOf("Relay"), state.networks.map { it.title })
    }

    @Test
    fun `a feed that fails moves to the error state`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn null }
        val viewModel = NetworksGridViewModel(listRepository)

        viewModel.load(NETWORKS_URL)

        assertTrue(viewModel.state.value is NetworksGridViewModel.UiState.Error)
    }

    @Test
    fun `a feed with no networks moves to the error state rather than showing a blank grid`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed() }
        val viewModel = NetworksGridViewModel(listRepository)

        viewModel.load(NETWORKS_URL)

        assertTrue(viewModel.state.value is NetworksGridViewModel.UiState.Error)
    }

    @Test
    fun `loading the same source again keeps what is already loaded`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed(RELAY) }
        val viewModel = NetworksGridViewModel(listRepository)
        viewModel.load(NETWORKS_URL)

        viewModel.load(NETWORKS_URL)

        val state = viewModel.state.value as NetworksGridViewModel.UiState.Loaded
        assertEquals(listOf("Relay"), state.networks.map { it.title })
        runBlocking { verify(listRepository, times(1)).getListFeed(NETWORKS_URL, false) }
    }

    @Test
    fun `retry reloads the source the failure came from`() {
        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn null }
        val viewModel = NetworksGridViewModel(listRepository)
        viewModel.load(NETWORKS_URL)

        listRepository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed(RELAY) }
        viewModel.retry()

        val state = viewModel.state.value as NetworksGridViewModel.UiState.Loaded
        assertEquals(listOf("Relay"), state.networks.map { it.title })
        runBlocking { verify(listRepository, times(2)).getListFeed(NETWORKS_URL, false) }
    }

    @Test
    fun `retry before a load does nothing`() {
        val viewModel = NetworksGridViewModel(listRepository)

        viewModel.retry()

        assertTrue(viewModel.state.value is NetworksGridViewModel.UiState.Loading)
    }

    private fun networksFeed(vararg lists: DiscoverListSummary) = ListFeed(
        title = "Networks",
        subtitle = null,
        description = null,
        shortDescription = null,
        date = null,
        podcasts = null,
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
        listId = "0b370140-ae34-4f10-9b6f-301820de0605",
        type = ListType.ListsList,
        summaryStyle = DisplayStyle.LargeList(),
        expandedStyle = ExpandedStyle.NetworkGrid(),
        lists = lists.toList(),
    )

    companion object {
        private const val NETWORKS_URL = "https://lists.pocketcasts.com/networks.json"

        private val RELAY = DiscoverListSummary(
            uuid = "cdb75bc0-9f5a-4217-b1ca-f573821a7913",
            title = "Relay",
            description = "The Relay network of podcasts.",
            type = ListType.PodcastList,
            summaryStyle = DisplayStyle.CollectionList(),
            expandedStyle = ExpandedStyle.NetworkGrid(),
            source = "https://lists.pocketcasts.com/cdb75bc0-9f5a-4217-b1ca-f573821a7913.json",
            collectionImage = "https://static.pocketcasts.com/relay-author.png",
            itemCount = 4,
            urlPath = "relay-network",
        )

        private val NYT = RELAY.copy(
            uuid = "e07f76d0-0064-48d9-81a3-895de009f5c7",
            title = "The New York Times",
        )
    }
}
