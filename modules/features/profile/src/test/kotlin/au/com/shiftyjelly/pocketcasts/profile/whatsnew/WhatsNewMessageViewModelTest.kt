package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
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
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WhatsNewMessageViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val feedMessages = MutableStateFlow<List<WhatsNewMessage>>(emptyList())

    private val manager = mock<WhatsNewManager> {
        on { feedMessages } doReturn feedMessages
    }

    private val settings = mock<Settings> {
        on { bottomInset } doReturn MutableStateFlow(0)
    }

    private fun createViewModel(messageId: String) = WhatsNewMessageViewModel(messageId, manager, settings)

    @Test
    fun `shows the feed message with the requested id`() = runTest {
        val wanted = message("wanted")
        feedMessages.value = listOf(message("other"), wanted)

        createViewModel("wanted").uiState.test {
            assertEquals(UiState.Loaded(wanted), expectMostRecentItem())
        }
    }

    @Test
    fun `a message the feed does not list is missing`() = runTest {
        feedMessages.value = listOf(message("other"))

        createViewModel("hidden").uiState.test {
            assertEquals(UiState.Missing, expectMostRecentItem())
        }
    }

    @Test
    fun `a message that leaves the feed while open stays on screen`() = runTest {
        val message = message("expiring")
        feedMessages.value = listOf(message)

        createViewModel("expiring").uiState.test {
            assertEquals(UiState.Loaded(message), expectMostRecentItem())
            feedMessages.value = emptyList()
            expectNoEvents()
        }
    }

    @Test
    fun `waits for the catalog to load before deciding the message is missing`() = runTest {
        val refresh = CompletableDeferred<Unit>()
        whenever(manager.refreshIfNeeded()).doSuspendableAnswer { refresh.await() }
        val message = message("restored")

        createViewModel("restored").uiState.test {
            assertEquals(UiState.Loading, expectMostRecentItem())
            feedMessages.value = listOf(message)
            refresh.complete(Unit)
            assertEquals(UiState.Loaded(message), expectMostRecentItem())
        }
    }

    @Test
    fun `a message still absent once the catalog has loaded is missing`() = runTest {
        val refresh = CompletableDeferred<Unit>()
        whenever(manager.refreshIfNeeded()).doSuspendableAnswer { refresh.await() }

        createViewModel("gone").uiState.test {
            assertEquals(UiState.Loading, expectMostRecentItem())
            refresh.complete(Unit)
            assertEquals(UiState.Missing, expectMostRecentItem())
        }
    }

    private fun message(id: String) = WhatsNewMessage(
        id = id,
        type = WhatsNewMessageType.Tip,
        publishedAt = Instant.parse("2026-09-01T00:00:00Z"),
        expiresAt = null,
        targeting = WhatsNewTargeting(audiences = emptyList(), minimumAppVersion = null),
        title = "Title $id",
        content = WhatsNewContent.Pages(
            listOf(WhatsNewPage(image = null, heading = "Heading", description = "Description", action = null)),
        ),
    )
}
