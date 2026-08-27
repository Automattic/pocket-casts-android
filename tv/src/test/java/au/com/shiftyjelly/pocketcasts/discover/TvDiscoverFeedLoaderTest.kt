package au.com.shiftyjelly.pocketcasts.discover

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvDiscoverFeedLoaderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()
    private val settings = mock<Settings>()
    private val context = mock<Context>()

    private fun TestScope.loader() = TvDiscoverFeedLoader(
        listRepository = listRepository,
        settings = settings,
        applicationScope = backgroundScope,
        context = context,
    )

    @Test
    fun `builds first and last cover urls and caches them after one fetch`() = runTest {
        val feed = feedOf("a", "b", "c")
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(feed)
        val loader = loader()

        val first = loader.loadCategoryCoverUrls(SOURCE)
        val second = loader.loadCategoryCoverUrls(SOURCE)

        assertEquals(listOf(artwork("a"), artwork("c")), first)
        assertEquals(first, second)
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `does not cache a failed feed load so it can be retried`() = runTest {
        val feed = feedOf("a", "b")
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doReturn(null, feed)
        val loader = loader()

        val first = loader.loadCategoryCoverUrls(SOURCE)
        val second = loader.loadCategoryCoverUrls(SOURCE)

        assertEquals(emptyList<String>(), first)
        assertEquals(listOf(artwork("a"), artwork("b")), second)
        verify(listRepository, times(2)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `a cancelled caller does not stop the covers from loading`() = runTest {
        val gate = CompletableDeferred<ListFeed>()
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doSuspendableAnswer { gate.await() }
        val loader = loader()

        val cancelled = launch { loader.loadCategoryCoverUrls(SOURCE) }
        runCurrent()
        cancelled.cancel()
        gate.complete(feedOf("a", "b"))
        advanceUntilIdle()

        assertEquals(listOf(artwork("a"), artwork("b")), loader.loadCategoryCoverUrls(SOURCE))
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    @Test
    fun `concurrent callers for the same source share one fetch`() = runTest {
        val gate = CompletableDeferred<ListFeed>()
        whenever(listRepository.getListFeed(eq(SOURCE), any())).doSuspendableAnswer { gate.await() }
        val loader = loader()

        val first = async { loader.loadCategoryCoverUrls(SOURCE) }
        val second = async { loader.loadCategoryCoverUrls(SOURCE) }
        runCurrent()
        gate.complete(feedOf("a", "b"))

        assertEquals(listOf(artwork("a"), artwork("b")), first.await())
        assertEquals(first.await(), second.await())
        verify(listRepository, times(1)).getListFeed(eq(SOURCE), any())
    }

    private fun feedOf(vararg uuids: String) = mock<ListFeed> {
        on { podcasts } doReturn uuids.map { uuid ->
            DiscoverPodcast(
                uuid = uuid,
                title = null,
                url = null,
                author = null,
                category = null,
                description = null,
                language = null,
                mediaType = null,
            )
        }
    }

    private fun artwork(uuid: String) = PodcastImage.getArtworkUrl(size = 200, uuid = uuid, isWearOS = false)

    private companion object {
        const val SOURCE = "https://category/us.json"
    }
}
