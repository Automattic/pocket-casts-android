package au.com.shiftyjelly.pocketcasts.podcasts.helper.search

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.podcasts.helper.search.SearchHandler.SearchResult
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeSearchHandlerTest {
    private val cacheServiceManager = mock<PodcastCacheServiceManagerImpl>()
    private val settings = mock<Settings> {
        on { getEpisodeSearchDebounceMs() } doReturn DEBOUNCE_MS
    }
    private val testDispatcher = StandardTestDispatcher()
    private val handler = EpisodeSearchHandler(settings, cacheServiceManager, EventHorizon(TestEventSink()), testDispatcher)

    private val noSearchResult = SearchResult("", null)

    @Test
    fun `empty query emits no search result without debounce`() = runTest(testDispatcher) {
        handler.getSearchResultsFlow(PODCAST_UUID).test {
            assertEquals(noSearchResult, awaitItem())
            assertEquals(0L, testScheduler.currentTime)
        }
    }

    @Test
    fun `query is searched only after the debounce window`() = runTest(testDispatcher) {
        whenever(cacheServiceManager.searchEpisodes(PODCAST_UUID, "abc")).thenReturn(Single.just(listOf("uuid")))

        handler.getSearchResultsFlow(PODCAST_UUID).test {
            assertEquals(noSearchResult, awaitItem())

            handler.searchQueryUpdated("ab")
            testScheduler.advanceTimeBy(DEBOUNCE_MS - 1)
            handler.searchQueryUpdated("abc")
            testScheduler.advanceTimeBy(DEBOUNCE_MS - 1)
            testScheduler.runCurrent()
            expectNoEvents()

            testScheduler.advanceTimeBy(2)
            assertEquals(SearchResult("abc", listOf("uuid")), awaitItem())
            verify(cacheServiceManager, never()).searchEpisodes(PODCAST_UUID, "ab")
        }
    }

    @Test
    fun `repeating the same query does not search again`() = runTest(testDispatcher) {
        whenever(cacheServiceManager.searchEpisodes(PODCAST_UUID, "abc")).thenReturn(Single.just(listOf("uuid")))

        handler.getSearchResultsFlow(PODCAST_UUID).test {
            skipItems(1)
            handler.searchQueryUpdated("abc")
            assertEquals(SearchResult("abc", listOf("uuid")), awaitItem())

            handler.searchQueryUpdated("abc")
            testScheduler.advanceTimeBy(DEBOUNCE_MS * 2)
            testScheduler.runCurrent()
            expectNoEvents()
            verify(cacheServiceManager, times(1)).searchEpisodes(PODCAST_UUID, "abc")
        }
    }

    @Test
    fun `clearing the query emits no search result without debounce`() = runTest(testDispatcher) {
        whenever(cacheServiceManager.searchEpisodes(any(), any())).thenReturn(Single.just(listOf("uuid")))

        handler.getSearchResultsFlow(PODCAST_UUID).test {
            skipItems(1)
            handler.searchQueryUpdated("abc")
            assertEquals(SearchResult("abc", listOf("uuid")), awaitItem())

            val clearedAt = testScheduler.currentTime
            handler.searchQueryUpdated("")
            assertEquals(noSearchResult, awaitItem())
            assertEquals(clearedAt, testScheduler.currentTime)
        }
    }

    @Test
    fun `search error falls back to no search result`() = runTest(testDispatcher) {
        whenever(cacheServiceManager.searchEpisodes(PODCAST_UUID, "xyz")).thenReturn(Single.just(listOf("uuid")))
        whenever(cacheServiceManager.searchEpisodes(PODCAST_UUID, "abc")).thenReturn(Single.error(RuntimeException()))

        handler.getSearchResultsFlow(PODCAST_UUID).test {
            skipItems(1)
            handler.searchQueryUpdated("xyz")
            assertEquals(SearchResult("xyz", listOf("uuid")), awaitItem())

            handler.searchQueryUpdated("abc")
            assertEquals(noSearchResult, awaitItem())
        }
    }

    private companion object {
        const val PODCAST_UUID = "podcast-uuid"
        const val DEBOUNCE_MS = 1_000L
    }
}
