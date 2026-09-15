package au.com.shiftyjelly.pocketcasts.repositories.sync

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeviceAuthStateTest {

    private val syncManager = mock<SyncManager>()

    @Test
    fun `completes once the pending code is approved`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(authorizeResponse(deviceCode = "code-1", userCode = "ABC123"))
        whenever(syncManager.loginWithDeviceAuth(eq("code-1"), any(), any()))
            .thenReturn(pending())
            .thenReturn(success())

        createFlow().test {
            assertEquals(DeviceAuthState.Loading, awaitItem())
            assertEquals(readyState(userCode = "ABC123"), awaitItem())
            assertEquals(DeviceAuthState.Complete, awaitItem())
            awaitComplete()
        }
        verify(syncManager, times(2)).loginWithDeviceAuth(eq("code-1"), eq(SignInSource.UserInitiated.Watch), eq(false))
    }

    @Test
    fun `requests a new code when the current one expires`() = runTest {
        whenever(syncManager.deviceAuthorize())
            .thenReturn(authorizeResponse(deviceCode = "code-1", userCode = "ABC123"))
            .thenReturn(authorizeResponse(deviceCode = "code-2", userCode = "XYZ789"))
        whenever(syncManager.loginWithDeviceAuth(eq("code-1"), any(), any())).thenReturn(failed("expired_token"))
        whenever(syncManager.loginWithDeviceAuth(eq("code-2"), any(), any())).thenReturn(success())

        createFlow().test {
            assertEquals(DeviceAuthState.Loading, awaitItem())
            assertEquals(readyState(userCode = "ABC123"), awaitItem())
            assertEquals(readyState(userCode = "XYZ789"), awaitItem())
            assertEquals(DeviceAuthState.Complete, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits an error when a code can't be requested`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenThrow(RuntimeException("offline"))

        createFlow().test {
            assertEquals(DeviceAuthState.Loading, awaitItem())
            assertEquals(DeviceAuthState.Error, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits an error when the user denies the code`() = runTest {
        whenever(syncManager.deviceAuthorize()).thenReturn(authorizeResponse(deviceCode = "code-1", userCode = "ABC123"))
        whenever(syncManager.loginWithDeviceAuth(eq("code-1"), any(), any())).thenReturn(failed("access_denied"))

        createFlow().test {
            assertEquals(DeviceAuthState.Loading, awaitItem())
            assertEquals(readyState(userCode = "ABC123"), awaitItem())
            assertEquals(DeviceAuthState.Error, awaitItem())
            awaitComplete()
        }
    }

    private fun createFlow() = deviceAuthFlow(
        syncManager = syncManager,
        signInSource = SignInSource.UserInitiated.Watch,
        isNewAccount = false,
    )

    private fun authorizeResponse(deviceCode: String, userCode: String) = DeviceAuthorizeResponse(
        deviceCode = deviceCode,
        userCode = userCode,
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?user_code=$userCode",
        expiresIn = 1800,
        interval = 5,
    )

    private fun readyState(userCode: String) = DeviceAuthState.Ready(
        userCode = userCode,
        verificationUri = "https://pocketcasts.com/pair",
        verificationUriComplete = "https://pocketcasts.com/pair?user_code=$userCode",
    )

    private fun success() = LoginResult.Success(
        AuthResultModel(token = AccessToken("access-token"), uuid = "user-uuid", isNewAccount = false),
    )

    private fun pending() = failed("authorization_pending")

    private fun failed(messageId: String) = LoginResult.Failed(message = messageId, messageId = messageId)
}
