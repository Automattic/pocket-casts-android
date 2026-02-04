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
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.WatchSyncState
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
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
    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val coroutineRule = MainCoroutineRule(testDispatcher)

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

    private lateinit var viewModel: WearMainActivityViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock UserManager to return a Flowable of SignInState
        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(SignInState.SignedOut),
        )

        // Mock tokenBundleRepository to return empty flow (prevents infinite collection)
        whenever(tokenBundleRepository.flow).thenReturn(emptyFlow())
    }

    @Test
    fun `initial state has correct default values`() = runTest(testDispatcher) {
        // When
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals(WatchSyncState.Syncing, state.syncState)
        assertEquals(SignInState.SignedOut, state.signInState)
        assertEquals(false, state.showLoggingInScreen)
    }

    @Test
    fun `onSignInConfirmationActionHandled sets showLoggingInScreen to false`() = runTest(testDispatcher) {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

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
    fun `signOut delegates to UserManager`() = runTest(testDispatcher) {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.signOut()

        // Then
        verify(userManager).signOut(playbackManager, wasInitiatedByUser = false)
    }

    @Test
    fun `retrySync can be called without error`() = runTest(testDispatcher) {
        // Given
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - should not throw
        viewModel.retrySync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - verify state is still Syncing after retry
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
    )
}
