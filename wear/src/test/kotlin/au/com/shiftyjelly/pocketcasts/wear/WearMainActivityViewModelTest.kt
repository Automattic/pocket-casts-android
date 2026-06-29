package au.com.shiftyjelly.pocketcasts.wear

import android.content.Context
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSync
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.wear.networking.ConnectivityStateManager
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncError
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for WearMainActivityViewModel.
 *
 * The watch passively listens for the auth token the phone publishes to the data
 * layer: the screen shows the login instructions (Idle) until a token arrives, at
 * which point a login is attempted and the state moves to Success or Failed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WearMainActivityViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule(StandardTestDispatcher())

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
    private lateinit var connectivityStateManager: ConnectivityStateManager

    private lateinit var viewModel: WearMainActivityViewModel

    private val connectivityFlow = MutableStateFlow(true)

    private val authData = WatchSyncAuthData(
        refreshToken = RefreshToken("refresh-token"),
        loginIdentity = LoginIdentity.PocketCasts,
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(SignInState.SignedOut),
        )
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(null))
        whenever(connectivityStateManager.isConnected).thenReturn(connectivityFlow)
    }

    @After
    fun tearDown() = runTest {
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `initial state shows login instructions`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(WatchSyncState.Idle, state.syncState)
        assertEquals(SignInState.SignedOut, state.signInState)
        assertEquals(false, state.showLoggingInScreen)
    }

    @Test
    fun `successful phone login moves to Success and shows logging in screen`() = runTest {
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(authData))
        whenever(watchSync.processAuthDataChange(anyOrNull(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(LoginResult) -> Unit>(1)(LoginResult.Success(mock<AuthResultModel>()))
            Unit
        }

        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(WatchSyncState.Success, state.syncState)
        assertEquals(true, state.showLoggingInScreen)
    }

    @Test
    fun `failed phone login surfaces a login failed error`() = runTest {
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(authData))
        whenever(watchSync.processAuthDataChange(anyOrNull(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(LoginResult) -> Unit>(1)(LoginResult.Failed("Invalid token", null))
            Unit
        }

        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(
            WatchSyncState.Failed(WatchSyncError.LoginFailed("Invalid token")),
            viewModel.state.value.syncState,
        )
    }

    @Test
    fun `already logged in moves to Success`() = runTest {
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(authData))
        whenever(watchSync.processAuthDataChange(anyOrNull(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<() -> Unit>(2)()
            Unit
        }

        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(WatchSyncState.Success, viewModel.state.value.syncState)
    }

    @Test
    fun `restartSyncIfNeeded clears a failed state back to instructions`() = runTest {
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(authData))
        whenever(watchSync.processAuthDataChange(anyOrNull(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(LoginResult) -> Unit>(1)(LoginResult.Failed("Invalid token", null))
            Unit
        }
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()
        assertEquals(
            WatchSyncState.Failed(WatchSyncError.LoginFailed("Invalid token")),
            viewModel.state.value.syncState,
        )

        viewModel.restartSyncIfNeeded()

        assertEquals(WatchSyncState.Idle, viewModel.state.value.syncState)
    }

    @Test
    fun `retrySync resets to instructions`() = runTest {
        whenever(tokenBundleRepository.flow).thenReturn(flowOf(authData))
        whenever(watchSync.processAuthDataChange(anyOrNull(), any(), any())).thenAnswer { invocation ->
            invocation.getArgument<(LoginResult) -> Unit>(1)(LoginResult.Failed("Invalid token", null))
            Unit
        }
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.retrySync()

        assertEquals(WatchSyncState.Idle, viewModel.state.value.syncState)
    }

    @Test
    fun `onSignInConfirmationActionHandled sets showLoggingInScreen to false`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.onSignInConfirmationActionHandled()

        assertEquals(false, viewModel.state.value.showLoggingInScreen)
    }

    @Test
    fun `signOut delegates to UserManager`() = runTest {
        viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.signOut()

        verify(userManager).signOut(playbackManager, wasInitiatedByUser = false)
    }

    private fun createViewModel() = WearMainActivityViewModel(
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        userManager = userManager,
        settings = settings,
        context = context,
        tokenBundleRepository = tokenBundleRepository,
        watchSync = watchSync,
        connectivityStateManager = connectivityStateManager,
    )
}
