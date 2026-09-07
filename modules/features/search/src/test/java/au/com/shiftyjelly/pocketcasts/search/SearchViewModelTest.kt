package au.com.shiftyjelly.pocketcasts.search

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SearchListShownEvent
import com.automattic.eventhorizon.SearchResultLegacyType
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class SearchViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var searchHandler: SearchHandler

    @Mock
    private lateinit var searchHistoryManager: SearchHistoryManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    private lateinit var viewModel: SearchViewModel

    private val eventSink = TestEventSink()
    private val improvedSearchResults = MutableSharedFlow<SearchUiState.SearchOperation<SearchResults.Results>>(replay = 1)

    @Before
    fun setUp() {
        whenever(searchHandler.searchSuggestions) doReturn emptyFlow()
        whenever(searchHandler.improvedSearchResults) doReturn improvedSearchResults
        viewModel = SearchViewModel(
            searchHandler = searchHandler,
            searchHistoryManager = searchHistoryManager,
            podcastManager = podcastManager,
            eventHorizon = EventHorizon(eventSink),
        )
    }

    @Test
    fun `given podcast is subscribed, when podcast subscribe plus icon clicked, then podcast is subscribed`() {
        val uuid = UUID.randomUUID().toString()
        viewModel.onSubscribeToPodcast(Podcast(uuid = uuid, isSubscribed = false))

        verify(podcastManager).subscribeToPodcast(podcastUuid = uuid, sync = true)
    }

    @Test
    fun `given podcast not subscribed, when podcast subscribe check icon clicked, then podcast remains subscribed`() {
        val uuid = UUID.randomUUID().toString()
        viewModel.onSubscribeToPodcast(Podcast(uuid = uuid, isSubscribed = true))

        verify(podcastManager, never()).subscribeToPodcast(podcastUuid = uuid, sync = true)
    }

    @Test
    fun `given results contain a network, when results are shown, then the networks filter is offered`() {
        emitResults(podcastItem, networkItem, episodeItem)

        assertEquals(
            listOf(ResultsFilters.TOP_RESULTS, ResultsFilters.PODCASTS, ResultsFilters.EPISODES, ResultsFilters.NETWORKS),
            resultsState().filterOptions,
        )
    }

    @Test
    fun `given results contain no networks, when results are shown, then the networks filter is not offered`() {
        emitResults(podcastItem, episodeItem)

        assertFalse(ResultsFilters.NETWORKS in resultsState().filterOptions)
    }

    @Test
    fun `given the networks filter is selected, when it is no longer offered, then the filter resets to top results`() {
        emitResults(podcastItem, networkItem)
        viewModel.selectFilter(ResultsFilters.NETWORKS)
        assertEquals(listOf(networkItem), filteredResults())

        emitResults(podcastItem, filter = ResultsFilters.NETWORKS)

        val state = resultsState()
        assertEquals(0, state.selectedFilterIndex)
        assertEquals(ResultsFilters.TOP_RESULTS, state.selectedFilter)
        assertEquals(listOf(podcastItem), filteredResults())
    }

    @Test
    fun `given the networks filter is no longer offered, when it is selected, then the selection is ignored`() {
        emitResults(podcastItem)

        viewModel.selectFilter(ResultsFilters.NETWORKS)

        val state = resultsState()
        assertEquals(0, state.selectedFilterIndex)
        assertEquals(ResultsFilters.TOP_RESULTS, state.selectedFilter)
        assertEquals(listOf(podcastItem), filteredResults())
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `when a filter is selected, then the selected index points at the offered filters`() {
        emitResults(podcastItem, networkItem, episodeItem)

        viewModel.selectFilter(ResultsFilters.NETWORKS)

        val state = resultsState()
        assertEquals(3, state.selectedFilterIndex)
        assertEquals(ResultsFilters.NETWORKS, state.selectedFilter)
    }

    @Test
    fun `given the networks filter is selected, when results are shown, then the shown list reports networks`() {
        emitResults(networkItem)
        viewModel.selectFilter(ResultsFilters.NETWORKS)
        eventSink.skipEvent(eventSink.size)

        viewModel.reportResultsShown()

        val event = eventSink.pollEvent() as SearchListShownEvent
        assertEquals(SearchResultLegacyType.Networks, event.displaying)
    }

    @Test
    fun `given no filter is selected, when results are shown, then the shown list reports no filter`() {
        emitResults(podcastItem, networkItem)

        viewModel.reportResultsShown()

        val event = eventSink.pollEvent() as SearchListShownEvent
        assertTrue(event.displaying == null)
    }

    private fun emitResults(vararg items: ImprovedSearchResultItem, filter: ResultsFilters = ResultsFilters.TOP_RESULTS) {
        improvedSearchResults.tryEmit(
            SearchUiState.SearchOperation.Success(
                searchTerm = "the times",
                results = SearchResults.Results(results = items.toList(), filter = filter),
            ),
        )
    }

    private fun resultsState() = viewModel.state.value as SearchUiState.Results

    private fun filteredResults() = (resultsState().operation as SearchUiState.SearchOperation.Success).results.filteredResults

    private val podcastItem = ImprovedSearchResultItem.PodcastItem(
        uuid = "podcast-uuid",
        title = "The Daily",
        author = "The New York Times",
        isFollowed = false,
    )

    private val networkItem = ImprovedSearchResultItem.NetworkItem(
        uuid = "network-uuid",
        title = "The New York Times",
        description = "The best audio journalism and storytelling in one place.",
    )

    private val episodeItem = ImprovedSearchResultItem.EpisodeItem(
        uuid = "episode-uuid",
        title = "Monday, March 3rd",
        podcastUuid = "podcast-uuid",
        podcastTitle = "The Daily",
        publishedDate = Date(0),
        duration = 25.minutes,
    )
}
