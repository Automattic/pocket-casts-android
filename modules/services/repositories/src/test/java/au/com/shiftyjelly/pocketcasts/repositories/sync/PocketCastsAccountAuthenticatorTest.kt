package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import com.automattic.eventhorizon.EventHorizon
import dagger.Lazy
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.create

/** Stands in for the sign in screen each app supplies, so the test does not depend on a real activity. */
private class SignInActivity : Activity()

@RunWith(RobolectricTestRunner::class)
class PocketCastsAccountAuthenticatorTest {

    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var account: Account
    private lateinit var accountManager: AccountManager
    private val cachedAccessToken = AtomicReference<String?>(null)
    private lateinit var authenticator: PocketCastsAccountAuthenticator

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val module = NetworkModule()
        val moshi = module.provideMoshi()

        val retrofit = module.provideRetrofitBuilder(moshi, Dispatcher())
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient())
            .build()

        val okhttpCache = Cache(File(context.cacheDir.absolutePath, "HttpCache"), (10 * 1024 * 1024).toLong())

        account = Account("support@pocketcasts.com", AccountConstants.ACCOUNT_TYPE)
        accountManager = mock<AccountManager> {
            on { getAccountsByType(AccountConstants.ACCOUNT_TYPE) } doReturn arrayOf(account)
            // An account signed in before the refresh token moved to user data
            on { getPassword(account) } doReturn "refresh_token"
            on { getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY) } doReturn AccountConstants.SignInType.Tokens.value
            // Behave like the platform auth token cache so a refreshed token is visible to the next caller
            on { peekAuthToken(account, AccountConstants.TOKEN_TYPE) } doAnswer { cachedAccessToken.get() }
            on { setAuthToken(eq(account), eq(AccountConstants.TOKEN_TYPE), anyOrNull()) } doAnswer {
                cachedAccessToken.set(it.arguments[2] as String?)
                Unit
            }
        }
        val tokenErrorNotification = mock<TokenErrorNotification>()
        val syncAccountManager = SyncAccountManagerImpl(tokenErrorNotification, accountManager)
        val syncServiceManager = SyncServiceManager(retrofit.create(), mock(), Lazy { okhttpCache }, AppPlatform.Phone)

        val syncManager = SyncManagerImpl(
            eventHorizon = EventHorizon(TestEventSink()),
            context = context,
            settings = mock(),
            syncAccountManager = syncAccountManager,
            syncServiceManager = syncServiceManager,
            moshi = moshi,
            notificationManager = mock(),
        )
        // make sure the test account is signed out
        runBlocking { syncManager.signOut() }

        authenticator = PocketCastsAccountAuthenticator(context, syncManager, SignInActivity::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    /**
     * Test a successful token refresh.
     */
    @Test
    fun getAuthTokenSuccess() {
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(
                """
                {
                    "email": "support@pocketcasts.com",
                    "uuid": "79424ba0-f09b-013b-249c-566ad7a4dc9d",
                    "isNew": false,
                    "accessToken": "new access token",
                    "tokenType": "Bearer",
                    "expiresIn": 1800,
                    "refreshToken": "refresh token"
                }
                """.trimIndent(),
            )
        mockWebServer.enqueue(response)

        val bundle = authenticator.getAuthToken(
            response = null,
            account = account,
            authTokenType = null,
            options = null,
        )
        assertNotNull(bundle)
        assertEquals(account.name, bundle.getString(AccountManager.KEY_ACCOUNT_NAME))
        assertEquals(account.type, bundle.getString(AccountManager.KEY_ACCOUNT_TYPE))
        assertEquals("new access token", bundle.getString(AccountManager.KEY_AUTHTOKEN))

        // check the token refresh endpoint was called
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/user/token", request?.path)
        // no scope is sent so the server keeps the scope the refresh token was issued with
        assertEquals(
            """{"grant_type":"refresh_token","refresh_token":"refresh_token"}""",
            request?.body?.readUtf8(),
        )

        // the refreshed token goes to user data, because setPassword broadcasts LOGIN_ACCOUNTS_CHANGED to every app
        verify(accountManager).setUserData(account, AccountConstants.REFRESH_TOKEN_KEY, "refresh token")
        verify(accountManager, never()).setPassword(any(), anyOrNull())
    }

    /**
     * Concurrent callers without a cached access token should share a single refresh.
     */
    @Test
    fun getAuthTokenConcurrentCallersRefreshOnce() {
        repeat(2) {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(HttpURLConnection.HTTP_OK)
                    .setBodyDelay(500, TimeUnit.MILLISECONDS)
                    .setBody(
                        """
                        {
                            "email": "support@pocketcasts.com",
                            "uuid": "79424ba0-f09b-013b-249c-566ad7a4dc9d",
                            "isNew": false,
                            "accessToken": "new access token $it",
                            "tokenType": "Bearer",
                            "expiresIn": 1800,
                            "refreshToken": "refresh token $it"
                        }
                        """.trimIndent(),
                    ),
            )
        }

        val callers = 4
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(callers)
        try {
            val results = (1..callers).map {
                executor.submit<String?> {
                    start.await()
                    authenticator.getAuthToken(null, account, null, null).getString(AccountManager.KEY_AUTHTOKEN)
                }
            }
            start.countDown()
            val tokens = results.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(List(callers) { "new access token 0" }, tokens)
            assertEquals(1, mockWebServer.requestCount)
            verify(accountManager).setUserData(account, AccountConstants.REFRESH_TOKEN_KEY, "refresh token 0")
            verify(accountManager, never()).setPassword(any(), anyOrNull())
        } finally {
            executor.shutdownNow()
        }
    }

    /**
     * When the user's refresh token has expired test the sign in intent is returned.
     */
    @Test
    fun getAuthTokenExpiredRefreshToken() {
        // Return the same response the server would with an expired refresh token
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST) // 400 - Bad request
            .setBody(
                """
                {
                    "error": "invalid_grant",
                    "error_description": "The provided grant is invalid, expired or has been revoked.",
                    "error_uri": ""
                }
                """.trimIndent(),
            )
        mockWebServer.enqueue(response)

        val bundle = authenticator.getAuthToken(
            response = null,
            account = account,
            authTokenType = null,
            options = null,
        )
        assertNotNull(bundle)

        // if the refresh token has expired and they need to sign in again an intent is returned to the login page
        val intent = BundleCompat.getParcelable(bundle, AccountManager.KEY_INTENT, Intent::class.java)
        assertNotNull(intent)
        assertEquals(SignInActivity::class.java.name, intent?.component?.className)

        // check the token refresh endpoint was called
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/user/token", request?.path)
    }

    /**
     * When the server returns an error an exception should be thrown.
     */
    @Test
    fun getAuthTokenServerError() {
        // Return a HTTP status code 500 which means there was an internal server error
        val response = MockResponse().setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
        mockWebServer.enqueue(response)

        assertThrows("A server error should cause a network exception", NetworkErrorException::class.java) {
            authenticator.getAuthToken(
                response = null,
                account = account,
                authTokenType = null,
                options = null,
            )
        }

        // check the token refresh endpoint was called
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/user/token", request?.path)
    }

    /**
     * The account advertises no features, so only an empty request can be satisfied.
     */
    @Test
    fun hasFeatures() {
        val noFeatures = authenticator.hasFeatures(null, account, emptyArray())
        assertTrue(noFeatures.getBoolean(AccountManager.KEY_BOOLEAN_RESULT))

        val unknownFeature = authenticator.hasFeatures(null, account, arrayOf("unsupported_feature"))
        assertEquals(false, unknownFeature.getBoolean(AccountManager.KEY_BOOLEAN_RESULT))
    }
}
