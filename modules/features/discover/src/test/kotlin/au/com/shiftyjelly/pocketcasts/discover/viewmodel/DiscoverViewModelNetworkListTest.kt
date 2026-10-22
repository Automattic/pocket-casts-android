package au.com.shiftyjelly.pocketcasts.discover.viewmodel

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverListSummary
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class DiscoverViewModelNetworkListTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val repository = mock<ListRepository>()
    private val settings = mock<Settings>()

    @Before
    fun setUp() {
        val countryCode = mock<UserSetting<String>>()
        whenever(countryCode.value).thenReturn("us")
        whenever(settings.discoverCountryCode).thenReturn(countryCode)
    }

    @Test
    fun `the networks row is drawn from the lists entries alone`() {
        repository.stub { on { getListFeed(any(), anyOrNull()) } doReturn networksFeed() }

        val networkList = createViewModel().loadNetworkList(NETWORKS_URL, authenticated = false).blockingFirst()

        assertEquals("0b370140-ae34-4f10-9b6f-301820de0605", networkList.listId)
        assertEquals("2026-09-03T05:27:38Z", networkList.date)
        assertEquals(listOf("Relay"), networkList.networks.map { it.title })
        // no network's own feed is fetched to render the row
        runBlocking {
            verify(repository).getListFeed(NETWORKS_URL, false)
            verifyNoMoreInteractions(repository)
        }
    }

    private fun createViewModel() = DiscoverViewModel(
        repository = repository,
        settings = settings,
        podcastManager = mock(),
        episodeManager = mock(),
        playbackManager = mock(),
        categoriesManager = mock(),
        eventHorizon = mock(),
        crashLogging = mock(),
        syncManager = mock(),
    )

    private fun networksFeed() = ListFeed(
        title = "Networks",
        subtitle = null,
        description = null,
        shortDescription = null,
        date = "2026-09-03T05:27:38Z",
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
        lists = listOf(
            DiscoverListSummary(
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
            ),
        ),
    )

    companion object {
        private const val NETWORKS_URL = "https://lists.pocketcasts.com/networks.json"
    }
}
