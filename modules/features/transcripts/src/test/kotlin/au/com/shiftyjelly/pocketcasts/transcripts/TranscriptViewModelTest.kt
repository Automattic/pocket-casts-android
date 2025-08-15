package au.com.shiftyjelly.pocketcasts.transcripts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val transcriptManager = TestTranscriptManager()
    private val signInStateFlow = MutableStateFlow<SignInState>(SignInState.SignedOut)

    lateinit var viewModel: TranscriptViewModel

    @Before
    fun setUp() {
        viewModel = TranscriptViewModel(
            transcriptManager = transcriptManager,
            episodeManager = mock(),
            userManager = mock {
                on { getSignInState() } doReturn signInStateFlow.asFlowable()
            },
            paymentClient = PaymentClient.test(),
            analyticsTracker = AnalyticsTracker.test(),
            source = TranscriptViewModel.Source.Player,
            sharingClient = object : TranscriptSharingClient {
                override suspend fun shareTranscript(request: SharingRequest): SharingResponse {
                    Timber.i("Sharing transcript with request: $request")
                    return SharingResponse(isSuccessful = true, feedbackMessage = null, error = null)
                }
            },
        )
    }

    @Test
    fun `start with empty state`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()

            // Set free trial flag because it is avaialable by default in the test PaymentClient
            assertEquals(UiState.Empty.copy(isFreeTrialAvailable = true), state)
        }
    }

    @Test
    fun `update plus user status`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            signInStateFlow.emit(SignInState.SignedIn("email", Subscription.PlusPreview))
            assertTrue(awaitItem().isPlusUser)

            signInStateFlow.emit(SignInState.SignedIn("email", subscription = null))
            assertFalse(awaitItem().isPlusUser)

            signInStateFlow.emit(SignInState.SignedIn("email", Subscription.PatronPreview))
            assertTrue(awaitItem().isPlusUser)

            signInStateFlow.emit(SignInState.SignedOut)
            assertFalse(awaitItem().isPlusUser)
        }
    }

    @Test
    fun `load transcript`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.loadTranscript("episode-uuid")
            assertEquals(TranscriptState.Loaded(transcriptManager.avaiableTranscript), awaitItem().transcriptState)
        }
    }

    @Test
    fun `load transcript with no content`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            transcriptManager.avaiableTranscript = Transcript.TextPreview.copy(entries = emptyList())
            viewModel.loadTranscript("episode-uuid")
            assertEquals(TranscriptState.NoContent, awaitItem().transcriptState)
        }
    }

    @Test
    fun `fail to load transcript`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            transcriptManager.shouldLoadTranscripts = false
            viewModel.loadTranscript("episode-uuid")
            assertEquals(TranscriptState.Failure, awaitItem().transcriptState)
        }
    }

    @Test
    fun `reload transcript`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            transcriptManager.shouldLoadTranscripts = false
            viewModel.loadTranscript("episode-uuid")
            assertEquals(TranscriptState.Failure, awaitItem().transcriptState)

            viewModel.reloadTranscript()
            assertEquals(TranscriptState.Loading, awaitItem().transcriptState)
            assertEquals(TranscriptState.Loaded(transcriptManager.avaiableTranscript), awaitItem().transcriptState)
        }
    }

    @Test
    fun `toggle search`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.openSearch()
            assertTrue(awaitItem().searchState.isSearchOpen)

            viewModel.hideSearch()
            assertFalse(awaitItem().searchState.isSearchOpen)
        }
    }

    @Test
    fun `find searched text`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            transcriptManager.avaiableTranscript = Transcript.TextPreview.copy(
                entries = listOf(
                    TranscriptEntry.Text("Some text"),
                    TranscriptEntry.Text("And more"),
                    TranscriptEntry.Text("And even more text"),
                ),
            )
            viewModel.loadTranscript("episode-uuid")
            skipItems(1)

            viewModel.searchInTranscript("text")
            assertEquals(SearchState.Empty.copy(searchTerm = "text"), awaitItem().searchState)
            assertEquals(
                SearchState(
                    isSearchOpen = true,
                    searchTerm = "text",
                    matches = SearchMatches(
                        selectedCoordinate = SearchCoordinates(line = 0, match = 5),
                        matchingCoordinates = mapOf(
                            0 to listOf(5),
                            2 to listOf(14),
                        ),
                    ),
                ),
                awaitItem().searchState,
            )
        }
    }

    @Test
    fun `cycle search matches`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            transcriptManager.avaiableTranscript = Transcript.TextPreview.copy(
                entries = listOf(
                    TranscriptEntry.Text("Some text"),
                    TranscriptEntry.Text("And more text and text"),
                    TranscriptEntry.Text("And even more text"),
                ),
            )
            viewModel.loadTranscript("episode-uuid")
            skipItems(1)

            viewModel.searchInTranscript("text")
            skipItems(1)
            assertEquals(0, awaitItem().searchState.matches.selectedMatchIndex)

            viewModel.selectNextSearchMatch()
            assertEquals(1, awaitItem().searchState.matches.selectedMatchIndex)

            viewModel.selectPreviousSearchMatch()
            assertEquals(0, awaitItem().searchState.matches.selectedMatchIndex)

            viewModel.selectPreviousSearchMatch()
            assertEquals(3, awaitItem().searchState.matches.selectedMatchIndex)

            viewModel.selectNextSearchMatch()
            assertEquals(0, awaitItem().searchState.matches.selectedMatchIndex)
        }
    }

    @Test
    fun `cancel search when loading new transript`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.loadTranscript("episode-uuid-1")
            viewModel.openSearch()
            viewModel.searchInTranscript("lorem")
            skipItems(3)

            viewModel.loadTranscript("episode-uuid-2")
            val state = awaitItem()
            assertEquals(SearchState.Empty, state.searchState)
            assertFalse(state.searchState.isSearchOpen)

            assertEquals(TranscriptState.Loaded(transcriptManager.avaiableTranscript), awaitItem().transcriptState)
        }
    }

    @Test
    fun `debounce search`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.loadTranscript("episode-uuid")
            skipItems(1)

            viewModel.searchInTranscript("lorem")
            assertEquals("lorem", awaitItem().searchState.searchTerm)
            advanceTimeBy(200)
            expectNoEvents()

            viewModel.searchInTranscript("lore")
            assertEquals("lore", awaitItem().searchState.searchTerm)
            advanceTimeBy(299)
            expectNoEvents()

            viewModel.searchInTranscript("lor")
            assertEquals("lor", awaitItem().searchState.searchTerm)
            advanceTimeBy(300)
            assertTrue(awaitItem().searchState.matches.matchingCoordinates.isNotEmpty())
        }
    }

    @Test
    fun `clear search`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.loadTranscript("episode-uuid")
            skipItems(1)

            viewModel.searchInTranscript("lorem")
            skipItems(2)

            // Trigger search to make sure that clearing ignores debounce
            // and no search event is emitted afterwards
            viewModel.searchInTranscript("lor")
            skipItems(1)
            advanceTimeBy(200)

            viewModel.clearSearch()
            assertEquals(SearchState.Empty.copy(isSearchOpen = true), awaitItem().searchState)
        }
    }
}

private class TestTranscriptManager : TranscriptManager {
    var avaiableTranscript: Transcript = Transcript.TextPreview
    var shouldLoadTranscripts = true

    override fun observeIsTranscriptAvailable(episodeUuid: String) = emptyFlow<Boolean>()

    override suspend fun loadTranscript(episodeUuid: String): Transcript? {
        yield()
        return avaiableTranscript.takeIf { shouldLoadTranscripts }
    }

    override fun resetInvalidTranscripts(episodeUuid: String) {
        shouldLoadTranscripts = true
    }
}
