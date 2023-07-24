package au.com.shiftyjelly.pocketcasts.search

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.utils.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

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

    @Mock
    private lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        whenever(searchHandler.searchResults).thenReturn(mock())
        whenever(searchHandler.loading).thenReturn(mock())
        viewModel =
            SearchViewModel(searchHandler, searchHistoryManager, podcastManager, analyticsTracker)
    }

    @Test
    fun `given podcast is subscribed, when podcast subscribe plus icon clicked, then podcast is subscribed`() =
        runTest {
            val uuid = UUID.randomUUID().toString()
            viewModel.onSubscribeToPodcast(Podcast(uuid = uuid, isSubscribed = false))

            verify(podcastManager).subscribeToPodcast(podcastUuid = uuid, sync = true)
        }

    @Test
    fun `given podcast not subscribed, when podcast subscribe check icon clicked, then podcast remains subscribed`() =
        runTest {
            val uuid = UUID.randomUUID().toString()
            viewModel.onSubscribeToPodcast(Podcast(uuid = uuid, isSubscribed = true))

            verify(podcastManager, never()).subscribeToPodcast(podcastUuid = uuid, sync = true)
        }
}
