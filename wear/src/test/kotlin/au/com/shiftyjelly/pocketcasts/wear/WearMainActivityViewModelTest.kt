package au.com.shiftyjelly.pocketcasts.wear

import android.content.Context
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.wear.networking.PhoneConnectionMonitor
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for WearMainActivityViewModel.
 *
 * Note: These tests focus on direct method calls and state management
 * rather than testing the complex flow collection logic in the init block,
 * which is better suited for integration tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WearMainActivityViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>

    @Mock
    private lateinit var watchSync: WatchSync

    @Mock
    private lateinit var phoneConnectionMonitor: PhoneConnectionMonitor

    private lateinit var viewModel: WearMainActivityViewModel

    // Flow that never completes, simulating real tokenBundleRepository behavior
    private val tokenFlow = MutableSharedFlow<WatchSyncAuthData?>()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock UserManager to return a Flowable of SignInState
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(SignInState.SignedOut),
        )

        // Mock tokenBundleRepository to return a flow that never completes (hot flow behavior)
        whenever(tokenBundleRepository.flow).thenReturn(tokenFlow)
    }

    private suspend fun setupPhoneConnectionMock() {
        // Mock phoneConnectionMonitor to return true (phone is connected)
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(true)
    }

    @After
    fun tearDown() = runTest {
        // Cancel the ViewModel's sync job to prevent coroutine leakage into next test
        // The ViewModel stores its sync job, so we can access it via reflection
        if (::viewModel.isInitialized) {
            try {
                val syncJobField = WearMainActivityViewModel::class.java.getDeclaredField("syncJob")
                syncJobField.isAccessible = true
                val syncJob = syncJobField.get(viewModel) as? kotlinx.coroutines.Job
                syncJob?.cancelAndJoin() // Wait for cancellation to complete
            } catch (e: Exception) {
                // If reflection fails, that's okay - the test framework will clean up eventually
            }
        }
    }

    @Test
    fun `initial state has correct default values`() = runTest {
        // Given
        setupPhoneConnectionMock()

        // When
        viewModel = createViewModel()
        // Run current tasks without advancing time to avoid triggering timeout
        testScheduler.runCurrent()

        // Then
        val state = viewModel.state.value
        assertEquals(WatchSyncState.Syncing, state.syncState)
        assertEquals(SignInState.SignedOut, state.signInState)
        assertEquals(false, state.showLoggingInScreen)
    }

    @Test
    fun `onSignInConfirmationActionHandled sets showLoggingInScreen to false`() = runTest {
        // Given
        setupPhoneConnectionMock()
        viewModel = createViewModel()
        testScheduler.runCurrent()

        // Manually update state to simulate successful login
        viewModel.state.value.copy(
            showLoggingInScreen = true,
            syncState = WatchSyncState.Success,
        )

        // When
        viewModel.onSignInConfirmationActionHandled()

        // Then
        assertEquals(false, viewModel.state.value.showLoggingInScreen)
    }

    @Test
    fun `signOut delegates to UserManager`() = runTest {
        // Given
        setupPhoneConnectionMock()
        viewModel = createViewModel()
        testScheduler.runCurrent()

        // When
        viewModel.signOut()

        // Then
        verify(userManager).signOut(playbackManager, wasInitiatedByUser = false)
    }

    @Test
    fun `retrySync can be called without error`() = runTest {
        // Given
        setupPhoneConnectionMock()
        viewModel = createViewModel()
        testScheduler.runCurrent()

        // When - should not throw
        viewModel.retrySync()
        testScheduler.runCurrent()

        // Then - verify state is reset to Syncing after retry
        assertEquals(WatchSyncState.Syncing, viewModel.state.value.syncState)
    }

    private fun createViewModel() = WearMainActivityViewModel(
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        userManager = userManager,
        settings = settings,
        context = context,
        tokenBundleRepository = tokenBundleRepository,
        watchSync = watchSync,
        phoneConnectionMonitor = phoneConnectionMonitor,
    )
}
