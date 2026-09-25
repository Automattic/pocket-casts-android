package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.PollState
import au.com.shiftyjelly.pocketcasts.profile.whatsnew.WhatsNewMessageViewModel.UiState
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewReadState
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewAction
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewContent
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewImage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewPoll
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewResearch
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewTargeting
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.WhatsNewActionTappedEvent
import com.automattic.eventhorizon.WhatsNewActionType
import com.automattic.eventhorizon.WhatsNewMessageShownEvent
import com.automattic.eventhorizon.WhatsNewPollResponseSubmittedEvent
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import com.automattic.eventhorizon.WhatsNewMessageType as AnalyticsMessageType

class WhatsNewMessageViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val feedMessages = MutableStateFlow<List<WhatsNewMessage>>(emptyList())

    private val readState = MutableStateFlow(WhatsNewReadState())

    private val manager = mock<WhatsNewManager> {
        on { feedMessages } doReturn feedMessages
        on { readState } doReturn readState
    }

    private val eventHorizon = mock<EventHorizon>()

    private val settings = mock<Settings> {
        on { bottomInset } doReturn MutableStateFlow(0)
    }

    private fun createViewModel(messageId: String) = WhatsNewMessageViewModel(messageId, manager, eventHorizon, settings)

    @Test
    fun `shows the feed message with the requested id`() = runTest {
        val wanted = message("wanted")
        feedMessages.value = listOf(message("other"), wanted)

        createViewModel("wanted").uiState.test {
            assertEquals(wanted, (expectMostRecentItem() as UiState.Loaded).message)
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
            assertEquals(message, (expectMostRecentItem() as UiState.Loaded).message)
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
            assertEquals(message, (expectMostRecentItem() as UiState.Loaded).message)
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

    @Test
    fun `pages keep their order and content`() = runTest {
        val image = WhatsNewImage(url = "https://example.com/a.webp", width = 2, height = 1, alt = "Alt")
        feedMessages.value = listOf(
            message(
                "paged",
                WhatsNewPage(image = image, heading = "One", description = "First", action = null),
                WhatsNewPage(image = null, heading = "Two", description = "Second", action = null),
            ),
        )

        createViewModel("paged").uiState.test {
            val pages = (expectMostRecentItem() as UiState.Loaded).pages
            assertEquals(listOf("One", "Two"), pages.map { it.heading })
            assertEquals(listOf("First", "Second"), pages.map { it.description })
            assertEquals(listOf(image, null), pages.map { it.image })
        }
    }

    @Test
    fun `a supported action becomes the page's button`() = runTest {
        feedMessages.value = listOf(message("action", page(WhatsNewAction(event = "open_discover", label = "Open Discover"))))

        createViewModel("action").uiState.test {
            val action = (expectMostRecentItem() as UiState.Loaded).pages.single().action
            assertEquals(WhatsNewMessageViewModel.Action("Open Discover", WhatsNewActionEvent.OpenDiscover), action)
        }
    }

    @Test
    fun `an unsupported action drops only the button`() = runTest {
        feedMessages.value = listOf(
            message(
                "unsupported",
                page(WhatsNewAction(event = "open_time_machine", label = "Travel")),
                page(WhatsNewAction(event = "open_hyperspace", label = "Jump")),
            ),
        )

        createViewModel("unsupported").uiState.test {
            val pages = (expectMostRecentItem() as UiState.Loaded).pages
            assertEquals(2, pages.size)
            assertEquals(listOf(null, null), pages.map { it.action })
        }
    }

    @Test
    fun `a research message shows its poll instead of pages`() = runTest {
        feedMessages.value = listOf(research())

        createViewModel("research").uiState.test {
            val state = expectMostRecentItem() as UiState.Loaded
            assertEquals(emptyList<WhatsNewMessageViewModel.Page>(), state.pages)
            assertEquals(PollState(research().researchContent, selectedOptionId = null, hasResponded = false), state.poll)
        }
    }

    @Test
    fun `a standard message has no poll`() = runTest {
        feedMessages.value = listOf(message("standard"))

        createViewModel("standard").uiState.test {
            assertNull((expectMostRecentItem() as UiState.Loaded).poll)
        }
    }

    @Test
    fun `picking an option selects it without submitting`() = runTest {
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onOptionClick("b")
            val poll = (expectMostRecentItem() as UiState.Loaded).poll!!
            assertEquals("b", poll.selectedOptionId)
            assertTrue(poll.canSubmit)
        }
        verify(manager, never()).markAsResponded(any())
        verify(eventHorizon, never()).track(any<WhatsNewPollResponseSubmittedEvent>())
    }

    @Test
    fun `submitting records the answer and reports it once`() = runTest {
        whenever(manager.markAsResponded(any())).then { readState.value = readState.value.copy(respondedPollIds = setOf("poll")) }
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onOptionClick("b")
            viewModel.onSubmitClick()
            viewModel.onSubmitClick()
            val poll = (expectMostRecentItem() as UiState.Loaded).poll!!
            assertTrue(poll.hasResponded)
            assertEquals("b", poll.selectedOptionId)
            assertFalse(poll.canSubmit)
        }
        verify(manager, times(1)).markAsResponded("poll")
        verify(eventHorizon, times(1)).track(
            WhatsNewPollResponseSubmittedEvent(
                messageUuid = "research",
                messageType = AnalyticsMessageType.Research,
                pollUuid = "poll",
                pollKey = "poll_key",
                optionUuid = "b",
                pollOptionKey = "option_b",
            ),
        )
    }

    @Test
    fun `picking another option replaces the selection`() = runTest {
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onOptionClick("a")
            viewModel.onOptionClick("b")
            assertEquals("b", (expectMostRecentItem() as UiState.Loaded).poll!!.selectedOptionId)
        }
    }

    @Test
    fun `a selection the poll no longer offers cannot be submitted`() = runTest {
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onOptionClick("b")
            val withoutB = research().copy(
                content = WhatsNewContent.Research(
                    research().researchContent.copy(
                        poll = research().researchContent.poll.copy(
                            options = research().researchContent.poll.options.filter { it.id != "b" },
                        ),
                    ),
                ),
            )
            feedMessages.value = listOf(withoutB)
            assertFalse((expectMostRecentItem() as UiState.Loaded).poll!!.canSubmit)
            viewModel.onSubmitClick()
        }
        verify(manager, never()).markAsResponded(any())
        verify(eventHorizon, never()).track(any<WhatsNewPollResponseSubmittedEvent>())
    }

    @Test
    fun `nothing is submitted before an option is picked`() = runTest {
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSubmitClick()
        }
        verify(manager, never()).markAsResponded(any())
        verify(eventHorizon, never()).track(any<WhatsNewPollResponseSubmittedEvent>())
    }

    @Test
    fun `a poll answered before opens closed with no option selected`() = runTest {
        readState.value = WhatsNewReadState(respondedPollIds = setOf("poll"))
        feedMessages.value = listOf(research())
        val viewModel = createViewModel("research")

        viewModel.uiState.test {
            val poll = (expectMostRecentItem() as UiState.Loaded).poll!!
            assertTrue(poll.hasResponded)
            assertNull(poll.selectedOptionId)
            viewModel.onOptionClick("a")
            viewModel.onSubmitClick()
            expectNoEvents()
        }
        verify(manager, never()).markAsResponded(any())
        verify(eventHorizon, never()).track(any<WhatsNewPollResponseSubmittedEvent>())
    }

    private fun research() = message("research").copy(
        type = WhatsNewMessageType.Research,
        content = WhatsNewContent.Research(
            WhatsNewResearch(
                description = "One question",
                poll = WhatsNewPoll(
                    pollId = "poll",
                    pollKey = "poll_key",
                    question = "Question",
                    options = listOf(
                        WhatsNewPoll.Option(id = "a", pollOptionKey = "option_a", label = "A"),
                        WhatsNewPoll.Option(id = "b", pollOptionKey = "option_b", label = "B"),
                    ),
                ),
            ),
        ),
    )

    private val WhatsNewMessage.researchContent get() = (content as WhatsNewContent.Research).research

    @Test
    fun `showing a message reports it once however often the feed emits`() = runTest {
        val message = message("shown")
        feedMessages.value = listOf(message)

        createViewModel("shown").uiState.test {
            expectMostRecentItem()
            feedMessages.value = listOf(message, message("other"))
            feedMessages.value = listOf(message)
            cancelAndIgnoreRemainingEvents()
        }
        verify(eventHorizon, times(1)).track(
            WhatsNewMessageShownEvent(messageUuid = "shown", messageType = AnalyticsMessageType.Tip),
        )
    }

    @Test
    fun `a message the feed does not list is not reported as shown`() = runTest {
        feedMessages.value = listOf(message("other"))

        createViewModel("hidden").uiState.test {
            expectMostRecentItem()
        }
        verifyNoInteractions(eventHorizon)
    }

    @Test
    fun `tapping an action reports it with its message`() = runTest {
        feedMessages.value = listOf(message("action", page(WhatsNewAction(event = "open_upsell", label = "Upgrade"))))
        val viewModel = createViewModel("action")

        viewModel.uiState.test {
            val action = (expectMostRecentItem() as UiState.Loaded).pages.single().action!!
            viewModel.onActionClick(action.event)
        }
        verify(eventHorizon).track(
            WhatsNewActionTappedEvent(
                messageUuid = "action",
                messageType = AnalyticsMessageType.Tip,
                action = WhatsNewActionType.OpenUpsell,
            ),
        )
    }

    private fun page(action: WhatsNewAction?) = WhatsNewPage(image = null, heading = "Heading", description = "Description", action = action)

    private fun message(id: String, vararg pages: WhatsNewPage) = message(id).copy(content = WhatsNewContent.Pages(pages.toList()))

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
