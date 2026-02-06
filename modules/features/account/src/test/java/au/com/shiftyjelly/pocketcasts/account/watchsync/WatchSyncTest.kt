package au.com.shiftyjelly.pocketcasts.account.watchsync

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.AuthResultModel
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.google.android.horologist.auth.data.phone.tokenshare.TokenBundleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class WatchSyncTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var syncManager: SyncManager

    @Mock
    private lateinit var tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>

    private lateinit var watchSync: WatchSync

    private val testRefreshToken = RefreshToken("test-refresh-token")
    private val testLoginIdentity = LoginIdentity.PocketCasts
    private val testAccessToken = AccessToken("test-access-token")

    @Before
    fun setup() {
        watchSync = WatchSync(syncManager, tokenBundleRepository)
    }

    @Test
    fun `processAuthDataChange calls syncManager loginWithToken when not logged in`() = runTest {
        // Given
        val authData = WatchSyncAuthData(testRefreshToken, testLoginIdentity)
        val expectedResult = LoginResult.Success(
            AuthResultModel(
                token = testAccessToken,
                uuid = "test-uuid",
                isNewAccount = false,
            ),
        )
        var callbackResult: LoginResult? = null

        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(
            syncManager.loginWithToken(
                token = testRefreshToken,
                loginIdentity = testLoginIdentity,
                signInSource = SignInSource.WatchPhoneSync,
            ),
        ).thenReturn(expectedResult)

        // When
        watchSync.processAuthDataChange(authData) { result ->
            callbackResult = result
        }

        // Then
        verify(syncManager).loginWithToken(
            token = testRefreshToken,
            loginIdentity = testLoginIdentity,
            signInSource = SignInSource.WatchPhoneSync,
        )
        assertEquals(expectedResult, callbackResult)
    }

    @Test
    fun `processAuthDataChange invokes callback with Success when already logged in`() = runTest {
        // Given
        val authData = WatchSyncAuthData(testRefreshToken, testLoginIdentity)
        var callbackResult: LoginResult? = null

        whenever(syncManager.isLoggedIn()).thenReturn(true)

        // When
        watchSync.processAuthDataChange(authData) { result ->
            callbackResult = result
        }

        // Then
        verify(syncManager, never()).loginWithToken(any(), any(), any())
        assertTrue(callbackResult is LoginResult.Success)
    }

    @Test
    fun `processAuthDataChange propagates login failure`() = runTest {
        // Given
        val authData = WatchSyncAuthData(testRefreshToken, testLoginIdentity)
        val expectedFailure = LoginResult.Failed(
            message = "Invalid credentials",
            messageId = "error_invalid_credentials",
        )
        var callbackResult: LoginResult? = null

        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(
            syncManager.loginWithToken(
                token = testRefreshToken,
                loginIdentity = testLoginIdentity,
                signInSource = SignInSource.WatchPhoneSync,
            ),
        ).thenReturn(expectedFailure)

        // When
        watchSync.processAuthDataChange(authData) { result ->
            callbackResult = result
        }

        // Then
        assertTrue(callbackResult is LoginResult.Failed)
        assertEquals("Invalid credentials", (callbackResult as LoginResult.Failed).message)
    }

    @Test
    fun `processAuthDataChange handles exceptions and returns Failed result`() = runTest {
        // Given
        val authData = WatchSyncAuthData(testRefreshToken, testLoginIdentity)
        val exception = RuntimeException("Network error")
        var callbackResult: LoginResult? = null

        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(
            syncManager.loginWithToken(
                token = testRefreshToken,
                loginIdentity = testLoginIdentity,
                signInSource = SignInSource.WatchPhoneSync,
            ),
        ).thenThrow(exception)

        // When
        watchSync.processAuthDataChange(authData) { result ->
            callbackResult = result
        }

        // Then
        assertTrue(callbackResult is LoginResult.Failed)
        assertEquals("Network error", (callbackResult as LoginResult.Failed).message)
    }

    @Test
    fun `processAuthDataChange does nothing when auth data is null`() = runTest {
        // Given
        var callbackInvoked = false

        // When
        watchSync.processAuthDataChange(null) {
            callbackInvoked = true
        }

        // Then
        verify(syncManager, never()).isLoggedIn()
        verify(syncManager, never()).loginWithToken(any(), any(), any())
        assertTrue("Callback should not be invoked for null data", !callbackInvoked)
    }

    @Test
    fun `sendAuthToDataLayer returns success when token saved successfully`() = runTest {
        // Given
        val authData = WatchSyncAuthData(testRefreshToken, testLoginIdentity)
        whenever(syncManager.getRefreshToken()).thenReturn(testRefreshToken)
        whenever(syncManager.getLoginIdentity()).thenReturn(testLoginIdentity)

        // When
        val result = watchSync.sendAuthToDataLayer()

        // Then
        assertTrue(result.isSuccess)
        verify(tokenBundleRepository).update(authData)
    }

    @Test
    fun `sendAuthToDataLayer clears data when no refresh token`() = runTest {
        // Given
        whenever(syncManager.getRefreshToken()).thenReturn(null)

        // When
        val result = watchSync.sendAuthToDataLayer()

        // Then
        assertTrue(result.isSuccess)
        verify(tokenBundleRepository).update(null)
    }

    @Test
    fun `sendAuthToDataLayer returns failure when update throws exception`() = runTest {
        // Given
        whenever(syncManager.getRefreshToken()).thenReturn(testRefreshToken)
        whenever(syncManager.getLoginIdentity()).thenReturn(testLoginIdentity)
        whenever(tokenBundleRepository.update(any())).thenThrow(RuntimeException("Data layer error"))

        // When
        val result = watchSync.sendAuthToDataLayer()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Data layer error") == true)
    }
}
