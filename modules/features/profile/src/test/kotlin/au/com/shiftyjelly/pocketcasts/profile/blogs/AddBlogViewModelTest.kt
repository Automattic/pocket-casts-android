package au.com.shiftyjelly.pocketcasts.profile.blogs

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeedsService
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AddBlogViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val webFeedsService = mock<WebFeedsService>()

    private lateinit var viewModel: AddBlogViewModel

    @Before
    fun setUp() {
        viewModel = AddBlogViewModel(webFeedsService)
    }

    @Test
    fun `initial state is Start`() {
        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("", viewModel.url.value)
    }

    @Test
    fun `onUrlChange updates url`() {
        viewModel.onUrlChange("https://example.com")

        assertEquals("https://example.com", viewModel.url.value)
    }

    @Test
    fun `onFindFeeds with blank url is ignored`() = runTest {
        viewModel.onFindFeeds("   ")

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
    }

    @Test
    fun `onFindFeeds with single feed transitions Start to Loading to Found`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val gate = CompletableDeferred<List<WebFeed>>()
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { gate.await() }

        viewModel.uiState.test {
            assertEquals(AddBlogViewModel.UiState.Start, awaitItem())

            viewModel.onFindFeeds("https://example.com")
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.complete(listOf(feed))
            assertEquals(AddBlogViewModel.UiState.Found(feed), awaitItem())
        }
    }

    @Test
    fun `onFindFeeds with multiple feeds transitions Start to Loading to Pick`() = runTest {
        val feeds = listOf(
            webFeed("Feed 1", "https://example.com/feed1"),
            webFeed("Feed 2", "https://example.com/feed2"),
        )
        val gate = CompletableDeferred<List<WebFeed>>()
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { gate.await() }

        viewModel.uiState.test {
            assertEquals(AddBlogViewModel.UiState.Start, awaitItem())

            viewModel.onFindFeeds("https://example.com")
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.complete(feeds)
            assertEquals(AddBlogViewModel.UiState.Pick(feeds), awaitItem())
        }
    }

    @Test
    fun `onFindFeeds with no feeds transitions Start to Loading to Error NoFeedsFound`() = runTest {
        val gate = CompletableDeferred<List<WebFeed>>()
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { gate.await() }

        viewModel.uiState.test {
            assertEquals(AddBlogViewModel.UiState.Start, awaitItem())

            viewModel.onFindFeeds("https://example.com")
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.complete(emptyList())
            assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.NoFeedsFound), awaitItem())
        }
    }

    @Test
    fun `onFindFeeds with IOException maps to NoInternet error`() = runTest {
        val gate = CompletableDeferred<List<WebFeed>>()
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { gate.await() }

        viewModel.uiState.test {
            assertEquals(AddBlogViewModel.UiState.Start, awaitItem())

            viewModel.onFindFeeds("https://example.com")
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.completeExceptionally(IOException("offline"))
            assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.NoInternet), awaitItem())
        }
    }

    @Test
    fun `onFindFeeds with non-IO exception maps to Generic error`() = runTest {
        val gate = CompletableDeferred<List<WebFeed>>()
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { gate.await() }

        viewModel.uiState.test {
            assertEquals(AddBlogViewModel.UiState.Start, awaitItem())

            viewModel.onFindFeeds("https://example.com")
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.completeExceptionally(RuntimeException("boom"))
            assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.Generic), awaitItem())
        }
    }

    @Test
    fun `resetToStart clears state and url`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { listOf(feed) }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com")

        assertEquals(AddBlogViewModel.UiState.Found(feed), viewModel.uiState.value)

        viewModel.resetToStart()

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("", viewModel.url.value)
    }

    @Test
    fun `editUrl returns to Start but keeps the url`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { listOf(feed) }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com")

        assertEquals(AddBlogViewModel.UiState.Found(feed), viewModel.uiState.value)

        viewModel.editUrl()

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("https://example.com", viewModel.url.value)
    }

    @Test
    fun `onBackPressed returns false when at Start`() {
        assertFalse(viewModel.onBackPressed())
        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
    }

    @Test
    fun `onBackPressed resets and returns true when not at Start`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { listOf(feed) }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com")

        assertEquals(AddBlogViewModel.UiState.Found(feed), viewModel.uiState.value)

        assertTrue(viewModel.onBackPressed())
        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("", viewModel.url.value)
    }

    private fun webFeed(title: String, href: String) = WebFeed(title = title, href = href, type = "rss")
}
