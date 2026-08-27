package au.com.shiftyjelly.pocketcasts.onboarding

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInMode
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInViewModel
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SignInShownEvent
import com.automattic.eventhorizon.SignInType
import com.automattic.eventhorizon.SignInTypeTappedEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSignInViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val syncManager = mock<SyncManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `trackShown fires sign in shown event`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(any(), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()
        viewModel.trackShown()

        verify(eventHorizon).track(SignInShownEvent)
    }

    @Test
    fun `successful device authorize transitions to Ready state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(any(), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

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

        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvSignInUiState.Error, awaitItem())
        }
    }

    @Test
    fun `successful poll transitions to Complete state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

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
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any(), any()))
            .thenReturn(createAuthorizationPending())
            .thenReturn(createAuthorizationPending())
            .thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

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
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any(), any()))
            .thenReturn(LoginResult.Failed(message = "Token expired", messageId = "expired_token"))

        val viewModel = createViewModel()

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
        whenever(syncManager.loginWithDeviceAuth(any(), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

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

    @Test
    fun `switching to email mode tracks password sign in type`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.Email)

        verify(eventHorizon).track(SignInTypeTappedEvent(type = SignInType.Password))
    }

    @Test
    fun `switching back to qr mode tracks qr sign in type`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.selectMode(TvSignInMode.QrCode)

        verify(eventHorizon).track(SignInTypeTappedEvent(type = SignInType.Qr))
    }

    @Test
    fun `reselecting the current mode does not track`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.QrCode)

        verify(eventHorizon, never()).track(any())
    }

    @Test
    fun `successful email sign in transitions to Complete state`() = runTest {
        val viewModel = createViewModelWithQrDisabled()
        whenever(syncManager.loginWithEmailAndPassword(any(), any(), any())).thenReturn(createLoginSuccess())

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        assertEquals(TvSignInUiState.Complete, viewModel.uiState.value)
        verify(syncManager).loginWithEmailAndPassword(
            email = eq("listener@pocketcasts.com"),
            password = eq("hunter2"),
            signInSource = eq(SignInSource.UserInitiated.Onboarding),
        )
    }

    @Test
    fun `invalid email shows email error and does not attempt login`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("not-an-email")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        assertTrue(viewModel.emailState.value.showEmailError)
        verify(syncManager, never()).loginWithEmailAndPassword(any(), any(), any())
    }

    @Test
    fun `short password shows password error and does not attempt login`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("short")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        assertTrue(viewModel.emailState.value.showPasswordError)
        verify(syncManager, never()).loginWithEmailAndPassword(any(), any(), any())
    }

    @Test
    fun `failed email sign in shows server error and clears submitting`() = runTest {
        val viewModel = createViewModelWithQrDisabled()
        whenever(syncManager.loginWithEmailAndPassword(any(), any(), any()))
            .thenReturn(LoginResult.Failed(message = "Wrong password", messageId = "invalid_credentials"))

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        assertEquals("Wrong password", viewModel.emailState.value.serverError)
        assertFalse(viewModel.emailState.value.isSubmitting)
        assertFalse(viewModel.uiState.value is TvSignInUiState.Complete)
    }

    @Test
    fun `submitting again while signing in only attempts one login`() = runTest {
        val viewModel = createViewModelWithQrDisabled()
        whenever(syncManager.loginWithEmailAndPassword(any(), any(), any())).thenReturn(createLoginSuccess())

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        verify(syncManager, times(1)).loginWithEmailAndPassword(any(), any(), any())
    }

    @Test
    fun `updating email trims surrounding whitespace`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.updateEmail("  listener@pocketcasts.com  ")

        assertEquals("listener@pocketcasts.com", viewModel.emailState.value.email)
    }

    @Test
    fun `switching to email mode clears field errors`() = runTest {
        val viewModel = createViewModelWithQrDisabled()

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("not-an-email")
        viewModel.updatePassword("short")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()
        assertTrue(viewModel.emailState.value.showEmailError)

        viewModel.selectMode(TvSignInMode.QrCode)
        viewModel.selectMode(TvSignInMode.Email)

        assertFalse(viewModel.emailState.value.showEmailError)
        assertFalse(viewModel.emailState.value.showPasswordError)
    }

    @Test
    fun `updating email clears the previous server error`() = runTest {
        val viewModel = createViewModelWithQrDisabled()
        whenever(syncManager.loginWithEmailAndPassword(any(), any(), any()))
            .thenReturn(LoginResult.Failed(message = "Wrong password", messageId = "invalid_credentials"))

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()
        assertEquals("Wrong password", viewModel.emailState.value.serverError)

        viewModel.updateEmail("listener@pocketcasts.com")

        assertNull(viewModel.emailState.value.serverError)
    }

    @Test
    fun `successful email sign in clears the credentials`() = runTest {
        val viewModel = createViewModelWithQrDisabled()
        whenever(syncManager.loginWithEmailAndPassword(any(), any(), any())).thenReturn(createLoginSuccess())

        viewModel.selectMode(TvSignInMode.Email)
        viewModel.updateEmail("listener@pocketcasts.com")
        viewModel.updatePassword("hunter2")
        viewModel.submitEmailSignIn()
        advanceUntilIdle()

        assertEquals("", viewModel.emailState.value.email)
        assertEquals("", viewModel.emailState.value.password)
        assertFalse(viewModel.emailState.value.isSubmitting)
    }

    private fun createViewModel() = TvSignInViewModel(syncManager, eventHorizon)

    private suspend fun createViewModelWithQrDisabled(): TvSignInViewModel {
        whenever(syncManager.deviceAuthorize()).thenThrow(RuntimeException("qr disabled"))
        return createViewModel()
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
