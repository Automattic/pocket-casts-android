package au.com.shiftyjelly.pocketcasts.transcripts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SharingResponse
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SyncedTranscriptsAutoScrollResumedEvent
import com.automattic.eventhorizon.SyncedTranscriptsSeekFailedEvent
import com.automattic.eventhorizon.SyncedTranscriptsSeekUsedEvent
import com.automattic.eventhorizon.TranscriptSourceType
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val transcriptManager = TestTranscriptManager()
    private val signInStateFlow = MutableStateFlow<SignInState>(SignInState.SignedOut)
    private val playbackStateFlow = MutableStateFlow(PlaybackState(episodeUuid = ""))
    private val syncedStateFlow = MutableStateFlow<FingerprintTimingManager.State>(FingerprintTimingManager.State.Idle)
    private val eventSink = TestEventSink()
    private val fingerprintTimingManager = mock<FingerprintTimingManager> {
        on { state } doReturn FingerprintTimingManager.State.Idle
        on { stateFlow } doReturn syncedStateFlow
    }
    private val playbackManager = mock<PlaybackManager> {
        on { playbackStateFlow } doReturn playbackStateFlow
    }
    private val episodeManager = mock<EpisodeManager>()

    lateinit var viewModel: TranscriptViewModel

    @Before
    fun setUp() {
        viewModel = TranscriptViewModel(
            transcriptManager = transcriptManager,
            episodeManager = episodeManager,
            userManager = mock {
                on { getSignInState() } doReturn signInStateFlow.asFlowable()
            },
            paymentClient = PaymentClient.test(),
            eventHorizon = EventHorizon(eventSink),
            source = TranscriptViewModel.Source.Player,
            sharingClient = object : TranscriptSharingClient {
                override suspend fun shareTranscript(request: SharingRequest): SharingResponse {
                    Timber.i("Sharing transcript with request: $request")
                    return SharingResponse(isSuccessful = true, feedbackMessage = null, error = null)
                }
            },
            fingerprintTimingManager = fingerprintTimingManager,
            playbackManager = playbackManager,
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

    @Test
    fun `track synced transcript seek used event`() = runTest {
        setUpTapToSeek()
        whenever(fingerprintTimingManager.state).thenReturn(FingerprintTimingManager.State.Active(coverage = 1))
        whenever(fingerprintTimingManager.playbackTimeMs(any())).thenReturn(20_000)
        syncedStateFlow.value = FingerprintTimingManager.State.Active(coverage = 1)

        awaitTapToSeekAvailable()
        drainEvents()

        val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

        assertEquals(20_000, seekTarget)
        assertEquals(
            SyncedTranscriptsSeekUsedEvent(
                fromPositionSeconds = 10L,
                toPositionSeconds = 20L,
                source = TranscriptSourceType.Player,
                episodeUuid = "episode-uuid",
                podcastUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
            eventSink.pollEvent(),
        )
    }

    @Test
    fun `track synced transcript seek failed event when preparing`() = runTest {
        setUpTapToSeek()
        syncedStateFlow.value = FingerprintTimingManager.State.Preparing

        awaitTapToSeekAvailable()
        drainEvents()

        viewModel.messages.test {
            val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

            assertNull(seekTarget)
            assertEquals(
                SyncedTranscriptsSeekFailedEvent(
                    reason = "mapping_unavailable",
                    syncedState = "preparing",
                    source = TranscriptSourceType.Player,
                    episodeUuid = "episode-uuid",
                    podcastUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
                ),
                eventSink.pollEvent(),
            )
            assertEquals(TranscriptMessage.TapToSeekStreamingUnavailable, awaitItem())
            verify(playbackManager, never()).seekToTimeMs(any(), anyOrNull())
        }
    }

    @Test
    fun `track synced transcript seek failed event when failed`() = runTest {
        setUpTapToSeek()
        syncedStateFlow.value = FingerprintTimingManager.State.Failed(
            RuntimeException("no match"),
            episodeUuid = "episode-uuid",
        )

        awaitTapToSeekAvailable()
        drainEvents()

        viewModel.messages.test {
            val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

            assertNull(seekTarget)
            assertEquals(
                SyncedTranscriptsSeekFailedEvent(
                    reason = "mapping_unavailable",
                    syncedState = "failed",
                    source = TranscriptSourceType.Player,
                    episodeUuid = "episode-uuid",
                    podcastUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
                ),
                eventSink.pollEvent(),
            )
            assertEquals(TranscriptMessage.TapToSeekStreamingUnavailable, awaitItem())
        }
    }

    @Test
    fun `track synced transcript seek failed event without message when unavailable`() = runTest {
        setUpTapToSeek()
        syncedStateFlow.value = FingerprintTimingManager.State.Unavailable(episodeUuid = "episode-uuid")

        awaitTapToSeekAvailable()
        drainEvents()

        viewModel.messages.test {
            val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

            assertNull(seekTarget)
            assertEquals(
                SyncedTranscriptsSeekFailedEvent(
                    reason = "mapping_unavailable",
                    syncedState = "unavailable",
                    source = TranscriptSourceType.Player,
                    episodeUuid = "episode-uuid",
                    podcastUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
                ),
                eventSink.pollEvent(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `track synced transcript seek failed event without message when episode is downloaded`() = runTest {
        setUpTapToSeek()
        val episode = PodcastEpisode(
            uuid = "episode-uuid",
            publishedDate = Date(),
            podcastUuid = "podcast-uuid",
            downloadStatus = EpisodeDownloadStatus.Downloaded,
        )
        whenever(episodeManager.findByUuid("episode-uuid")).thenReturn(episode)
        syncedStateFlow.value = FingerprintTimingManager.State.Preparing

        awaitTapToSeekAvailable()
        drainEvents()

        viewModel.messages.test {
            val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

            assertNull(seekTarget)
            assertEquals(
                SyncedTranscriptsSeekFailedEvent(
                    reason = "mapping_unavailable",
                    syncedState = "preparing",
                    source = TranscriptSourceType.Player,
                    episodeUuid = "episode-uuid",
                    podcastUuid = "podcast-uuid",
                ),
                eventSink.pollEvent(),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `do not track seek events for non generated transcript`() = runTest {
        setUpTapToSeek()
        transcriptManager.avaiableTranscript = Transcript.TextPreview
        syncedStateFlow.value = FingerprintTimingManager.State.Active(coverage = 1)

        awaitSyncedActive()
        drainEvents()

        val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

        assertNull(seekTarget)
        assertTrue(eventSink.isEmpty())
        verify(playbackManager, never()).seekToTimeMs(any(), anyOrNull())
    }

    @Test
    fun `do not track seek events when transcript episode is not playing`() = runTest {
        setUpTapToSeek()
        playbackStateFlow.value = PlaybackState(episodeUuid = "other-uuid", positionMs = 10_000)
        syncedStateFlow.value = FingerprintTimingManager.State.Active(coverage = 1)

        awaitUiState { it.transcriptState is TranscriptState.Loaded && it.playingEpisodeUuid == "other-uuid" }
        drainEvents()

        val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

        assertNull(seekTarget)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `do not track seek events when paywall is visible`() = runTest {
        setUpTapToSeek()
        signInStateFlow.value = SignInState.SignedOut
        syncedStateFlow.value = FingerprintTimingManager.State.Active(coverage = 1)

        awaitSyncedActive()
        drainEvents()

        val seekTarget = viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = 30_000))

        assertNull(seekTarget)
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `do not track seek events for untimed entries`() = runTest {
        setUpTapToSeek()
        syncedStateFlow.value = FingerprintTimingManager.State.Preparing

        awaitTapToSeekAvailable()
        drainEvents()

        assertNull(viewModel.seekToTranscriptEntry(TranscriptEntry.Speaker("Speaker")))
        assertNull(viewModel.seekToTranscriptEntry(TranscriptEntry.Text("Line", startTimeMs = -1)))
        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `track synced transcript auto scroll resumed event`() = runTest {
        drainEvents()

        viewModel.trackAutoScrollResumed(manualScrollDurationMs = 1_500L)

        assertEquals(
            SyncedTranscriptsAutoScrollResumedEvent(
                manualScrollDurationMs = 1_500L,
                source = TranscriptSourceType.Player,
                episodeUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
                podcastUuid = AnalyticsTracker.INVALID_OR_NULL_VALUE,
            ),
            eventSink.pollEvent(),
        )
    }

    private fun setUpTapToSeek() {
        transcriptManager.avaiableTranscript = Transcript.TextPreview.copy(isGenerated = true)
        signInStateFlow.value = SignInState.SignedIn("email", Subscription.PlusPreview)
        playbackStateFlow.value = PlaybackState(episodeUuid = "episode-uuid", positionMs = 10_000)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(PodcastEpisode(uuid = "episode-uuid", publishedDate = Date()))
    }

    private suspend fun awaitSyncedActive() = awaitUiState { it.isSyncedActive }

    private suspend fun awaitTapToSeekAvailable() = awaitUiState { it.isTapToSeekAvailable && it.syncedState == syncedStateFlow.value }

    private suspend fun awaitUiState(predicate: (UiState) -> Boolean) {
        viewModel.uiState.test {
            viewModel.loadTranscript("episode-uuid")
            var state = awaitItem()
            while (!predicate(state)) {
                state = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun drainEvents() {
        while (!eventSink.isEmpty()) {
            eventSink.pollEvent()
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

    override suspend fun loadSummaryText(episodeUuid: String): String? = null
}
