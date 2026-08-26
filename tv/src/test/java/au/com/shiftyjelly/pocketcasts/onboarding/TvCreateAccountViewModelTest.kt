package au.com.shiftyjelly.pocketcasts.onboarding

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.onboarding.createaccount.TvCreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.CreateAccountShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingFlowType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvCreateAccountViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val syncManager = mock<SyncManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `trackShown fires create account shown event`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenThrow(RuntimeException("qr disabled"))

        createViewModel().trackShown()

        verify(eventHorizon).track(CreateAccountShownEvent(flow = OnboardingFlowType.InitialOnboarding))
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
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `successful poll transitions to Complete state`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val states = mutableListOf(awaitItem())
            while (states.last() !is TvSignInUiState.Complete) {
                states.add(awaitItem())
            }
            assertEquals(TvSignInUiState.Complete, states.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `create account polls device auth as a new account`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(createDeviceAuthorizeResponse())
        whenever(syncManager.loginWithDeviceAuth(eq("device-code-123"), any(), any())).thenReturn(createLoginSuccess())

        val viewModel = createViewModel()

        viewModel.uiState.test {
            var state = awaitItem()
            while (state !is TvSignInUiState.Complete) {
                state = awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }

        verify(syncManager).loginWithDeviceAuth(eq("device-code-123"), any(), isNewAccount = eq(true))
    }

    private fun createViewModel() = TvCreateAccountViewModel(syncManager, eventHorizon)

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
            isNewAccount = true,
        ),
    )
}
