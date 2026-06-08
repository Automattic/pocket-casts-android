package au.com.shiftyjelly.pocketcasts.onboarding

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInViewModel
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSignInViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val syncManager = mock<SyncManager>()

    @Test
    fun `successful device authorize transitions to Ready state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(any(), any())).thenReturn(createLoginSuccess())

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is TvSignInUiState.Ready || state is TvSignInUiState.Complete)
            if (state is TvSignInUiState.Ready) {
                assertEquals(listOf("A", "B", "C", "1", "2", "3"), state.userCode)
                assertEquals("https://pocketcasts.com/pair", state.verificationUri)
                assertEquals("https://pocketcasts.com/pair?code=ABC123", state.verificationUriComplete)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `device authorize failure transitions to Error state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenThrow(RuntimeException("Network error"))

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            assertEquals(TvSignInUiState.Error, awaitItem())
        }
    }

    @Test
    fun `successful poll transitions to Complete state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any())).thenReturn(createLoginSuccess())

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            // May see Ready briefly before Complete, or jump straight to Complete
            val states = mutableListOf(awaitItem())
            if (states.last() !is TvSignInUiState.Complete) {
                states.add(awaitItem())
            }
            assertEquals(TvSignInUiState.Complete, states.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authorization pending continues polling until success`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any()))
            .thenReturn(createAuthorizationPending())
            .thenReturn(createAuthorizationPending())
            .thenReturn(createLoginSuccess())

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            // Skip Ready, wait for Complete
            val states = mutableListOf(awaitItem())
            while (states.last() !is TvSignInUiState.Complete) {
                states.add(awaitItem())
            }
            assertTrue(states.any { it is TvSignInUiState.Ready })
            assertEquals(TvSignInUiState.Complete, states.last())
        }
    }

    @Test
    fun `non-pending error stops polling`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any()))
            .thenReturn(LoginResult.Failed(message = "Token expired", messageId = "expired_token"))

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            val states = mutableListOf(awaitItem())
            while (states.last() !is TvSignInUiState.Error) {
                states.add(awaitItem())
            }
            assertTrue(states.any { it is TvSignInUiState.Ready })
            assertEquals(TvSignInUiState.Error, states.last())
        }
    }

    @Test
    fun `retry restarts device authorize flow`() = runTest {
        whenever(syncManager.deviceAuthorize())
            .thenThrow(RuntimeException("Network error"))
            .thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(any(), any())).thenReturn(createLoginSuccess())

        val viewModel = TvSignInViewModel(syncManager)

        viewModel.uiState.test {
            assertEquals(TvSignInUiState.Error, awaitItem())

            viewModel.retry()

            // Loading may be skipped if authorize completes immediately
            val states = mutableListOf(awaitItem())
            while (states.last() !is TvSignInUiState.Complete) {
                states.add(awaitItem())
            }
            assertEquals(TvSignInUiState.Complete, states.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createDeviceAuthorizeResponse() = DeviceAuthorizeResponse(
        deviceCode = "device-code-123",
        userCode = "ABC123",
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?code=ABC123",
        expiresIn = 1800,
        interval = 1,
    )

    private fun createLoginSuccess() = LoginResult.Success(
        AuthResultModel(
            token = AccessToken("access-token"),
            uuid = "user-uuid",
            isNewAccount = false,
        ),
    )

    private fun createAuthorizationPending() = LoginResult.Failed(
        message = "Authorization pending",
        messageId = "authorization_pending",
    )
}
