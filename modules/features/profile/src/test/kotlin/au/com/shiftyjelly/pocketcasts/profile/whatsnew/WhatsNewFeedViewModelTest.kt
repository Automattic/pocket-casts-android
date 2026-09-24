package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewFeedViewModel.LoadState
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewReadState
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WhatsNewFeedViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val manager = FakeWhatsNewManager()

    private val settings = mock<Settings> {
        on { bottomInset } doReturn MutableStateFlow(0)
    }

    private fun createViewModel() = WhatsNewFeedViewModel(manager, settings)

    @Test
    fun `shows the messages the manager lists once the catalog loads`() = runTest {
        val messages = listOf(message("new"), message("old"))
        manager.onRefresh = { manager.publish(messages) }

        createViewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(LoadState.Loaded, state.loadState)
            assertEquals(listOf("new", "old"), state.items.map { it.id })
        }
    }

    @Test
    fun `stays loading while the first load is in flight`() = runTest {
        val refresh = CompletableDeferred<Unit>()
        manager.onRefresh = { refresh.await() }

        createViewModel().uiState.test {
            assertEquals(LoadState.Loading, expectMostRecentItem().loadState)
            refresh.complete(Unit)
            manager.publish(listOf(message("a")))
            assertEquals(LoadState.Loaded, expectMostRecentItem().loadState)
        }
    }

    @Test
    fun `fails when loading leaves no catalog`() = runTest {
        createViewModel().uiState.test {
            assertEquals(LoadState.Failed, expectMostRecentItem().loadState)
        }
    }

    @Test
    fun `a cached catalog shows as loaded when the fetch brings nothing new`() = runTest {
        manager.publish(listOf(message("cached")))
        manager.onRefresh = {}

        createViewModel().uiState.test {
            val state = expectMostRecentItem()
            assertEquals(LoadState.Loaded, state.loadState)
            assertEquals(listOf("cached"), state.items.map { it.id })
        }
    }

    @Test
    fun `retrying after a failure loads the catalog`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(LoadState.Failed, expectMostRecentItem().loadState)
            manager.onRefresh = { manager.publish(listOf(message("a"))) }
            viewModel.retry()
            assertEquals(LoadState.Loaded, expectMostRecentItem().loadState)
        }
    }

    @Test
    fun `unread follows the read state`() = runTest {
        manager.publish(listOf(message("read"), message("unread")))
        manager.readState.value = WhatsNewReadState(readMessageIds = setOf("read"))

        createViewModel().uiState.test {
            val items = expectMostRecentItem().items.associate { it.id to it.isUnread }
            assertEquals(mapOf("read" to false, "unread" to true), items)
        }
    }

    @Test
    fun `showing messages marks them listed but not read`() = runTest {
        manager.publish(listOf(message("a"), message("b")))

        createViewModel().onMessagesShown(listOf("a", "b"))

        assertEquals(setOf("a", "b"), manager.readState.value.listedMessageIds)
        assertTrue(manager.readState.value.readMessageIds.isEmpty())
    }

    @Test
    fun `collecting the feed does not mark anything listed on its own`() = runTest {
        manager.publish(listOf(message("a")))

        createViewModel().uiState.test {
            expectMostRecentItem()
            manager.publish(listOf(message("a"), message("b")))
            expectMostRecentItem()
        }

        assertTrue(manager.readState.value.listedMessageIds.isEmpty())
    }

    @Test
    fun `a message the catalog repeats is listed once`() = runTest {
        manager.publish(listOf(message("a"), message("a"), message("b")))

        createViewModel().uiState.test {
            assertEquals(listOf("a", "b"), expectMostRecentItem().items.map { it.id })
        }
    }

    @Test
    fun `retrying again while a retry is in flight does not fetch twice`() = runTest {
        var fetchCount = 0
        val viewModel = createViewModel()
        val refresh = CompletableDeferred<Unit>()
        manager.onRefresh = {
            fetchCount++
            refresh.await()
        }

        viewModel.retry()
        viewModel.retry()
        refresh.complete(Unit)

        assertEquals(1, fetchCount)
    }

    @Test
    fun `clicking a message marks only that message read`() = runTest {
        manager.publish(listOf(message("a"), message("b")))
        val viewModel = createViewModel()

        viewModel.onMessageClick("a")

        assertEquals(setOf("a"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `read all marks exactly the listed messages read`() = runTest {
        manager.publish(listOf(message("a"), message("b")))
        manager.catalog.value = manager.catalog.value?.copy(messages = listOf(message("a"), message("b"), message("hidden")))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem().hasUnread)
            viewModel.onReadAllClick()
            assertFalse(expectMostRecentItem().hasUnread)
        }
        assertEquals(setOf("a", "b"), manager.readState.value.readMessageIds)
    }

    @Test
    fun `nothing is unread once every listed message is read`() = runTest {
        manager.publish(listOf(message("a")))
        manager.readState.value = WhatsNewReadState(readMessageIds = setOf("a"))

        createViewModel().uiState.test {
            assertFalse(expectMostRecentItem().hasUnread)
        }
    }

    @Test
    fun `pull to refresh forces a fetch and shows progress until it finishes`() = runTest {
        manager.publish(listOf(message("a")))
        val viewModel = createViewModel()
        val refresh = CompletableDeferred<Unit>()
        manager.onRefresh = { refresh.await() }

        viewModel.uiState.test {
            assertFalse(expectMostRecentItem().isRefreshing)
            viewModel.refresh()
            assertTrue(expectMostRecentItem().isRefreshing)
            refresh.complete(Unit)
            assertFalse(expectMostRecentItem().isRefreshing)
        }
        assertEquals(1, manager.forcedRefreshCount)
    }

    @Test
    fun `pulling again while a refresh is in flight does not fetch twice`() = runTest {
        manager.publish(listOf(message("a")))
        val viewModel = createViewModel()
        val refresh = CompletableDeferred<Unit>()
        manager.onRefresh = { refresh.await() }

        viewModel.refresh()
        viewModel.refresh()
        refresh.complete(Unit)

        assertEquals(1, manager.forcedRefreshCount)
    }

    @Test
    fun `refreshing without any catalog fails`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(LoadState.Failed, expectMostRecentItem().loadState)
            viewModel.refresh()
            assertEquals(LoadState.Failed, expectMostRecentItem().loadState)
        }
    }

    private fun message(id: String) = WhatsNewMessage(
        id = id,
        type = WhatsNewMessageType.NewFeature,
        publishedAt = Instant.parse("2026-09-01T00:00:00Z"),
        expiresAt = null,
        targeting = WhatsNewTargeting(audiences = emptyList(), minimumAppVersion = null),
        title = "Title $id",
        content = WhatsNewContent.Pages(
            listOf(WhatsNewPage(image = null, heading = "Heading", description = "Description", action = null)),
        ),
    )

    private class FakeWhatsNewManager : WhatsNewManager {
        override val catalog = MutableStateFlow<WhatsNewCatalog?>(null)
        override val readState = MutableStateFlow(WhatsNewReadState())
        override val feedMessages = MutableStateFlow<List<WhatsNewMessage>>(emptyList())
        override val hasUnlistedMessages = MutableStateFlow(false)
        override val hasUnseenMessages = MutableStateFlow(false)

        var onRefresh: suspend () -> Unit = {}
        var forcedRefreshCount = 0

        fun publish(messages: List<WhatsNewMessage>) {
            catalog.value = WhatsNewCatalog(1, null, "android", "en", messages)
            feedMessages.value = messages
        }

        override suspend fun refreshIfNeeded() = onRefresh()

        override suspend fun refresh() {
            forcedRefreshCount++
            onRefresh()
        }

        override fun markAsRead(messageIds: Collection<String>) {
            readState.value = readState.value.copy(readMessageIds = readState.value.readMessageIds + messageIds)
        }

        override fun markAsSeen(messageIds: Collection<String>) {
            readState.value = readState.value.copy(seenMessageIds = readState.value.seenMessageIds + messageIds)
        }

        override fun markAsListed(messageIds: Collection<String>) {
            readState.value = readState.value.copy(listedMessageIds = readState.value.listedMessageIds + messageIds)
        }

        override fun markAsResponded(pollId: String) {
            readState.value = readState.value.copy(respondedPollIds = readState.value.respondedPollIds + pollId)
        }

        override fun resetReadState() {
            readState.value = WhatsNewReadState()
        }
    }
}
