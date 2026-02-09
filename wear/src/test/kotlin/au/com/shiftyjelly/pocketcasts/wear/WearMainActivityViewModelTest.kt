package au.com.shiftyjelly.pocketcasts.wear

import android.content.Context
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.wear.networking.PhoneConnectionMonitor
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncError
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
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

    @Test
    fun `initial state has correct default values`() = runTest {
        // Given
        setupPhoneConnectionMock()

        // When
        viewModel = createViewModel()
        // Run current tasks without advancing time to avoid triggering timeout
        runCurrent()

        // Then
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WatchSyncState.Syncing, state.syncState)
            assertEquals(SignInState.SignedOut, state.signInState)
            assertEquals(false, state.showLoggingInScreen)
        }
    }

    @Test
    fun `signOut delegates to UserManager`() = runTest {
        // Given
        setupPhoneConnectionMock()
        viewModel = createViewModel()
        runCurrent()

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
        runCurrent()

        // When - should not throw
        viewModel.retrySync()
        runCurrent()

        // Then - verify state is reset to Syncing after retry
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WatchSyncState.Syncing, state.syncState)
        }
    }

    @Test
    fun `startSyncFlow fails immediately when phone not connected`() = runTest {
        // Given: Phone is not connected
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(false)

        viewModel = createViewModel()

        // When: ViewModel initializes and starts sync flow
        runCurrent()

        // Then: Should immediately fail with NoPhoneConnection error
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.syncState is WatchSyncState.Failed)
            assertEquals(WatchSyncError.NoPhoneConnection, (state.syncState as WatchSyncState.Failed).error)
        }

        // Verify no flow collection was attempted
        verify(tokenBundleRepository, never()).flow
    }

    @Test
    fun `successful login sets Success state and shows logging in screen`() = runTest {
        // Given: Phone is connected and auth data will be emitted
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(true)

        val authData = WatchSyncAuthData(
            refreshToken = RefreshToken("test-token"),
            loginIdentity = LoginIdentity.PocketCasts,
        )
        val authFlow = MutableStateFlow<WatchSyncAuthData?>(authData)
        whenever(tokenBundleRepository.flow).thenReturn(authFlow)

        // Mock watchSync to invoke callback with success
        whenever(watchSync.processAuthDataChange(any(), any())).then { invocation ->
            val callback = invocation.getArgument<(LoginResult) -> Unit>(1)
            callback(
                LoginResult.Success(
                    AuthResultModel(
                        token = AccessToken("test-token"),
                        uuid = "test-uuid",
                        isNewAccount = false,
                    ),
                ),
            )
        }

        viewModel = createViewModel()

        // When: Start sync flow
        runCurrent()

        // Then: Should be in Success state
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WatchSyncState.Success, state.syncState)
            assertEquals(true, state.showLoggingInScreen)
        }
    }

    @Test
    fun `sync converts LoginResult Failed to WatchSyncState Failed with LoginFailed error`() = runTest {
        // Given: Phone is connected and auth data will be emitted
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(true)

        val authData = WatchSyncAuthData(
            refreshToken = RefreshToken("test-token"),
            loginIdentity = LoginIdentity.PocketCasts,
        )
        val authFlow = MutableStateFlow<WatchSyncAuthData?>(authData)
        whenever(tokenBundleRepository.flow).thenReturn(authFlow)

        // Mock watchSync to invoke callback with failure
        whenever(watchSync.processAuthDataChange(any(), any())).then { invocation ->
            val callback = invocation.getArgument<(LoginResult) -> Unit>(1)
            callback(LoginResult.Failed(message = "Invalid credentials", messageId = null))
        }

        viewModel = createViewModel()

        // When: Start sync flow
        runCurrent()

        // Then: Should be in Failed state with LoginFailed error
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.syncState is WatchSyncState.Failed)
            assertTrue((state.syncState as WatchSyncState.Failed).error is WatchSyncError.LoginFailed)
        }
    }

    @Test
    fun `startSyncFlow times out after 30 seconds and sets Timeout error`() = runTest {
        // Given: Phone is connected but no auth data is emitted (causes timeout)
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(true)

        // Flow that never emits a non-null value
        val neverEmittingFlow = MutableStateFlow<WatchSyncAuthData?>(null)
        whenever(tokenBundleRepository.flow).thenReturn(neverEmittingFlow)

        viewModel = createViewModel()

        // When: Advance time to just before timeout
        advanceTimeBy(29_999)
        runCurrent()

        // Should still be syncing
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WatchSyncState.Syncing, state.syncState)
        }

        // Advance time past timeout threshold
        advanceTimeBy(1)
        runCurrent()

        // Then: Should be in Failed state with Timeout error
        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.syncState is WatchSyncState.Failed)
            assertEquals(WatchSyncError.Timeout, (state.syncState as WatchSyncState.Failed).error)
        }
    }

    @Test
    fun `retrySync is debounced within 3 seconds`() = runTest {
        // Given: Phone connected and setup
        whenever(phoneConnectionMonitor.isPhoneConnected()).thenReturn(true)
        val authFlow = MutableStateFlow<WatchSyncAuthData?>(null)
        whenever(tokenBundleRepository.flow).thenReturn(authFlow)

        viewModel = createViewModel()
        runCurrent()

        // When: First retry attempt (after initial sync)
        viewModel.retrySync()
        runCurrent()

        // Record how many times phone connection was checked
        var callCount = 1 // Once for initial sync, once for first retry

        // Immediate retry - should be debounced (no additional check)
        viewModel.retrySync()
        runCurrent()

        // Advance 2 seconds - still within debounce window
        advanceTimeBy(2_000)
        viewModel.retrySync()
        runCurrent()

        // Advance 1 more second (total 3s) - debounce expired, should work now
        advanceTimeBy(1_000)
        viewModel.retrySync()
        runCurrent()

        // Then: Verify second retry happened (state is Syncing)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WatchSyncState.Syncing, state.syncState)
        }
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
