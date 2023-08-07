package au.com.shiftyjelly.pocketcasts.profile.accountmanager

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.content.Context
import android.content.Intent
import androidx.core.os.BundleCompat
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.TokenErrorNotification
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class PocketCastsAccountAuthenticatorTest {

    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var account: Account
    private lateinit var authenticator: PocketCastsAccountAuthenticator

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder()
            .add(AccessToken::class.java, AccessToken.Adapter)
            .add(RefreshToken::class.java, RefreshToken.Adapter)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(OkHttpClient.Builder().build())
            .build()

        val okhttpCache = Cache(File(context.cacheDir.absolutePath, "HttpCache"), (10 * 1024 * 1024).toLong())

        account = Account("support@pocketcasts.com", AccountConstants.ACCOUNT_TYPE)
        val accountManager = mock<AccountManager> {
            on { getAccountsByType(AccountConstants.ACCOUNT_TYPE) } doReturn arrayOf(account)
            on { getPassword(account) } doReturn "refresh_token"
            on { getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY) } doReturn AccountConstants.SignInType.Tokens.value
        }
        val tokenErrorNotification = mock<TokenErrorNotification>()
        val syncAccountManager = SyncAccountManagerImpl(tokenErrorNotification, accountManager)
        val syncServerManager = SyncServerManager(retrofit, mock(), okhttpCache)

        val syncManager = SyncManagerImpl(
            analyticsTracker = mock(),
            context = context,
            settings = mock(),
            syncAccountManager = syncAccountManager,
            syncServerManager = syncServerManager,
        )
        // make sure the test device is signed out
        syncManager.signOut()

        authenticator = PocketCastsAccountAuthenticator(context, syncManager)
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
                """.trimIndent()
            )
        mockWebServer.enqueue(response)

        val bundle = authenticator.getAuthToken(
            response = null,
            account = account,
            authTokenType = null,
            options = null
        )
        assertNotNull(bundle)
        assertEquals(account.name, bundle.getString(AccountManager.KEY_ACCOUNT_NAME))
        assertEquals(account.type, bundle.getString(AccountManager.KEY_ACCOUNT_TYPE))
        assertEquals("new access token", bundle.getString(AccountManager.KEY_AUTHTOKEN))

        // check the token refresh endpoint was called
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/user/token", request?.path)
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
                """.trimIndent()
            )
        mockWebServer.enqueue(response)

        val bundle = authenticator.getAuthToken(
            response = null,
            account = account,
            authTokenType = null,
            options = null
        )
        assertNotNull(bundle)

        // if the refresh token has expired and they need to sign in again an intent is returned to the login page
        val intent = BundleCompat.getParcelable(bundle, AccountManager.KEY_INTENT, Intent::class.java)
        assertNotNull(intent)
        assertEquals(AccountActivity::class.java.name, intent?.component?.className)

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
                options = null
            )
        }

        // check the token refresh endpoint was called
        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("/user/token", request?.path)
    }
}
