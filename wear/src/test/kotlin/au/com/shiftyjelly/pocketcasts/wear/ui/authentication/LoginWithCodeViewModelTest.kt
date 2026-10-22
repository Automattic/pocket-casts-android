package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.DeviceAuthState
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LoginWithCodeViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val syncManager = mock<SyncManager>()
    private val podcastManager = mock<PodcastManager>()

    private val readyResponse = DeviceAuthorizeResponse(
        deviceCode = "code-1",
        userCode = "ABC123",
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?user_code=ABC123",
        expiresIn = 1800,
        interval = 5,
    )

    private val readyState = DeviceAuthState.Ready(
        userCode = "ABC123",
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?user_code=ABC123",
    )

    @Test
    fun `retry requests a new code after an error`() = runTest {
        whenever(syncManager.deviceAuthorize())
            .thenThrow(RuntimeException("offline"))
            .thenReturn(readyResponse)

        val viewModel = LoginWithCodeViewModel(syncManager, podcastManager)

        viewModel.state.test {
            assertEquals(DeviceAuthState.Error, awaitItem())

            viewModel.retry()

            assertEquals(readyState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshes podcasts before completing sign in`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(readyResponse)
        whenever(syncManager.loginWithDeviceAuth(any(), any(), any())).thenReturn(
            LoginResult.Success(AuthResultModel(token = AccessToken("token"), uuid = "uuid", isNewAccount = false)),
        )

        val viewModel = LoginWithCodeViewModel(syncManager, podcastManager)

        viewModel.state.test {
            assertEquals(readyState, awaitItem())
            assertEquals(DeviceAuthState.Complete, awaitItem())
            inOrder(syncManager, podcastManager) {
                verify(syncManager).loginWithDeviceAuth(any(), any(), any())
                verify(podcastManager).refreshPodcastsAfterSignIn()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `stops polling once the screen stops collecting`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(readyResponse)
        whenever(syncManager.loginWithDeviceAuth(any(), any(), any()))
            .thenReturn(LoginResult.Failed(message = "authorization_pending", messageId = "authorization_pending"))

        val viewModel = LoginWithCodeViewModel(syncManager, podcastManager)

        viewModel.state.test {
            assertEquals(readyState, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(6.seconds)
        clearInvocations(syncManager)
        advanceTimeBy(1.minutes)

        verify(syncManager, never()).loginWithDeviceAuth(any(), any(), any())
    }
}
