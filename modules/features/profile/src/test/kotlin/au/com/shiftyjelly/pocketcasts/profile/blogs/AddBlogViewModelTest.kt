package au.com.shiftyjelly.pocketcasts.profile.blogs

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeed
import au.com.shiftyjelly.pocketcasts.servers.webfeeds.WebFeedsService
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.pocketcasts.service.api.WebFeedCreateResponse
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AddBlogViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val webFeedsService = mock<WebFeedsService>()
    private val syncManager = mock<SyncManager>()
    private val podcastManager = mock<PodcastManager>()

    private lateinit var viewModel: AddBlogViewModel

    @Before
    fun setUp() {
        viewModel = AddBlogViewModel(webFeedsService, syncManager, podcastManager)
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
        viewModel.onFindFeeds("   ") { }

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
    }

    @Test
    fun `onFindFeeds with single feed creates the feed and navigates to the podcast`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val podcastUuid = "uuid-123"
        val podcastResponse = mock<com.pocketcasts.service.api.ApiPodcastResponse> {
            on { uuid } doReturn podcastUuid
        }
        val response = mock<WebFeedCreateResponse> {
            on { hasPodcast() } doReturn true
            on { podcast } doReturn podcastResponse
        }
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { listOf(feed) }
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).thenReturn(response)

        var navigatedUuid: String? = null
        viewModel.onFindFeeds("https://example.com") { navigatedUuid = it }

        verify(syncManager).createWebFeedPodcastOrThrow(feed.href)
        assertEquals(podcastUuid, navigatedUuid)
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

            viewModel.onFindFeeds("https://example.com") { }
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

            viewModel.onFindFeeds("https://example.com") { }
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

            viewModel.onFindFeeds("https://example.com") { }
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

            viewModel.onFindFeeds("https://example.com") { }
            assertEquals(AddBlogViewModel.UiState.Loading, awaitItem())

            gate.completeExceptionally(RuntimeException("boom"))
            assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.Generic), awaitItem())
        }
    }

    @Test
    fun `resetToStart clears state and url`() = runTest {
        val feeds = listOf(
            webFeed("Feed 1", "https://example.com/feed1"),
            webFeed("Feed 2", "https://example.com/feed2"),
        )
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { feeds }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com") { }

        assertEquals(AddBlogViewModel.UiState.Pick(feeds), viewModel.uiState.value)

        viewModel.resetToStart()

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("", viewModel.url.value)
    }

    @Test
    fun `editUrl returns to Start but keeps the url`() = runTest {
        val feeds = listOf(
            webFeed("Feed 1", "https://example.com/feed1"),
            webFeed("Feed 2", "https://example.com/feed2"),
        )
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { feeds }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com") { }

        assertEquals(AddBlogViewModel.UiState.Pick(feeds), viewModel.uiState.value)

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
        val feeds = listOf(
            webFeed("Feed 1", "https://example.com/feed1"),
            webFeed("Feed 2", "https://example.com/feed2"),
        )
        whenever(webFeedsService.getFeeds("https://example.com")).doSuspendableAnswer { feeds }

        viewModel.onUrlChange("https://example.com")
        viewModel.onFindFeeds("https://example.com") { }

        assertEquals(AddBlogViewModel.UiState.Pick(feeds), viewModel.uiState.value)

        assertTrue(viewModel.onBackPressed())
        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("", viewModel.url.value)
    }

    @Test
    fun `createFeed with success transitions Loading to success and calls onNavigateToPodcast`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val podcastUuid = "uuid-123"
        val podcastResponse = mock<com.pocketcasts.service.api.ApiPodcastResponse> {
            on { uuid } doReturn podcastUuid
        }
        val response = mock<WebFeedCreateResponse> {
            on { hasPodcast() } doReturn true
            on { podcast } doReturn podcastResponse
        }
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).thenReturn(response)

        var navigatedUuid: String? = null
        viewModel.createFeed(feed) { navigatedUuid = it }

        assertEquals(AddBlogViewModel.UiState.Loading, viewModel.uiState.value)
        verify(syncManager).createWebFeedPodcastOrThrow(feed.href)
        verify(podcastManager).subscribeToPodcast(podcastUuid = eq(podcastUuid), sync = eq(true), shouldAutoDownload = eq(true))
        assertEquals(podcastUuid, navigatedUuid)
    }

    @Test
    fun `createFeed with timeout transitions to Generic error`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val response = mock<WebFeedCreateResponse> {
            on { hasPodcast() } doReturn false
        }
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).thenReturn(response)

        viewModel.createFeed(feed) { }

        assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.Generic), viewModel.uiState.value)
    }

    @Test
    fun `createFeed with exception transitions to Generic error`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).thenThrow(RuntimeException("boom"))

        viewModel.createFeed(feed) { }

        assertEquals(AddBlogViewModel.UiState.Error(AddBlogViewModel.ErrorReason.Generic), viewModel.uiState.value)
    }

    @Test
    fun `resetToStart cancels in-flight createFeed and skips navigation`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val gate = CompletableDeferred<WebFeedCreateResponse>()
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).doSuspendableAnswer { gate.await() }

        var navigatedUuid: String? = null
        viewModel.createFeed(feed) { navigatedUuid = it }
        assertEquals(AddBlogViewModel.UiState.Loading, viewModel.uiState.value)

        viewModel.resetToStart()

        val podcastResponse = mock<com.pocketcasts.service.api.ApiPodcastResponse> {
            on { uuid } doReturn "uuid-123"
        }
        gate.complete(
            mock<WebFeedCreateResponse> {
                on { hasPodcast() } doReturn true
                on { podcast } doReturn podcastResponse
            },
        )

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertNull(navigatedUuid)
    }

    @Test
    fun `editUrl cancels in-flight createFeed and skips navigation`() = runTest {
        val feed = webFeed("Example", "https://example.com/feed")
        val gate = CompletableDeferred<WebFeedCreateResponse>()
        whenever(syncManager.createWebFeedPodcastOrThrow(feed.href)).doSuspendableAnswer { gate.await() }

        viewModel.onUrlChange("https://example.com")
        var navigatedUuid: String? = null
        viewModel.createFeed(feed) { navigatedUuid = it }

        viewModel.editUrl()

        val podcastResponse = mock<com.pocketcasts.service.api.ApiPodcastResponse> {
            on { uuid } doReturn "uuid-123"
        }
        gate.complete(
            mock<WebFeedCreateResponse> {
                on { hasPodcast() } doReturn true
                on { podcast } doReturn podcastResponse
            },
        )

        assertEquals(AddBlogViewModel.UiState.Start, viewModel.uiState.value)
        assertEquals("https://example.com", viewModel.url.value)
        assertNull(navigatedUuid)
    }

    private fun webFeed(title: String, href: String) = WebFeed(title = title, href = href, type = "rss")
}
