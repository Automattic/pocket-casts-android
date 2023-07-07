package au.com.shiftyjelly.pocketcasts.servers.account

import android.accounts.AccountManager
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import retrofit2.Retrofit
import java.net.HttpURLConnection

internal class SyncAccountTest {

    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var retrofit: Retrofit
    private lateinit var okhttpCache: Cache
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = ServersModule.provideMoshiBuilder().build()
        val okHttpClient = OkHttpClient.Builder().build()
        retrofit = ServersModule.provideRetrofit(baseUrl = mockWebServer.url("/").toString(), okHttpClient = okHttpClient, moshi = moshi)
        okhttpCache = ServersModule.provideCache(folder = "TestCache", context = context)

        val accountManager = AccountManager.get(context)
        val syncServerManager = SyncServerManager(retrofit, mock(), okhttpCache)
        val syncAccountManager = SyncAccountManagerImpl(mock(), accountManager)

        syncManager = SyncManagerImpl(
            analyticsTracker = mock(),
            context = context,
            settings = mock(),
            syncAccountManager = syncAccountManager,
            syncServerManager = syncServerManager,
        )
        syncManager.signOut()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
        syncManager.signOut()
    }

    @Test
    fun signInEmailAndPasswordSuccessTest() {
        val responseBody = """
            {
                "email": "support+signin@pocketcasts.com",
                "uuid": "610c8140-9a2e-013b-4f44-566ad7a4dc9c",
                "isNew": true,
                "accessToken": "secret_access_token_signin",
                "tokenType": "Bearer",
                "expiresIn": 3600,
                "refreshToken": "secret_refresh_token_signin"
            }
        """
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(responseBody)
        mockWebServer.enqueue(response)

        val result = runBlocking {
            syncManager.loginWithEmailAndPassword(
                email = "support+signin@pocketcasts.com",
                password = "password_signin",
                signInSource = SignInSource.UserInitiated.Onboarding
            )
        }
        assert(result is LoginResult.Success)
        val successResult = result as LoginResult.Success
        assertEquals(AccessToken("secret_access_token_signin"), successResult.result.token)
        assertEquals("610c8140-9a2e-013b-4f44-566ad7a4dc9c", successResult.result.uuid)
        assertEquals(true, successResult.result.isNewAccount)

        // verify the request was sent correctly
        val request = mockWebServer.takeRequest()
        assertEquals("/user/login_pocket_casts", request.path)
        assertEquals("POST", request.method)
        assertEquals("""{"email":"support+signin@pocketcasts.com","password":"password_signin","scope":"mobile"}""", request.body.readUtf8())

        // verify the correct values were written to the account manager
        val accountManager = AccountManager.get(context)
        val account = accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).first()
        assertEquals("support+signin@pocketcasts.com", account.name)
        // verify access token
        val authToken = accountManager.getAuthToken(account, AccountConstants.TOKEN_TYPE, null, null, null, null).result
        assertEquals("secret_access_token_signin", authToken.getString(AccountManager.KEY_AUTHTOKEN))
        // verify refresh token
        assertEquals("secret_refresh_token_signin", accountManager.getPassword(account))
        // verify user data
        assertEquals("610c8140-9a2e-013b-4f44-566ad7a4dc9c", accountManager.getUserData(account, AccountConstants.UUID))
        assertEquals("Tokens", accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
    }

    @Test
    fun createUserSuccessTest() {
        val responseBody = """
            {
                "email": "support+register@pocketcasts.com",
                "uuid": "e0730740-9a55-013b-4f44-566ad7a4dc9c",
                "isNew": true,
                "accessToken": "secret_access_token_register",
                "tokenType": "Bearer",
                "expiresIn": 3600,
                "refreshToken": "secret_refresh_token_register"
            }
        """
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(responseBody)
        mockWebServer.enqueue(response)

        val result = runBlocking {
            syncManager.createUserWithEmailAndPassword(
                email = "support+register@pocketcasts.com",
                password = "password_register",
            )
        }
        assert(result is LoginResult.Success)
        val successResult = result as LoginResult.Success
        assertEquals(AccessToken("secret_access_token_register"), successResult.result.token)
        assertEquals("e0730740-9a55-013b-4f44-566ad7a4dc9c", successResult.result.uuid)
        assertEquals(true, successResult.result.isNewAccount)

        // verify the request was sent correctly
        val request = mockWebServer.takeRequest()
        assertEquals("/user/register_pocket_casts", request.path)
        assertEquals("POST", request.method)
        assertEquals("""{"email":"support+register@pocketcasts.com","password":"password_register","scope":"mobile"}""", request.body.readUtf8())

        // verify the correct values were written to the account manager
        val accountManager = AccountManager.get(context)
        val account = accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).first()
        assertEquals("support+register@pocketcasts.com", account.name)
        // verify access token
        val authToken = accountManager.getAuthToken(account, AccountConstants.TOKEN_TYPE, null, null, null, null).result
        assertEquals("secret_access_token_register", authToken.getString(AccountManager.KEY_AUTHTOKEN))
        // verify refresh token
        assertEquals("secret_refresh_token_register", accountManager.getPassword(account))
        // verify user data
        assertEquals("e0730740-9a55-013b-4f44-566ad7a4dc9c", accountManager.getUserData(account, AccountConstants.UUID))
        assertEquals("Tokens", accountManager.getUserData(account, AccountConstants.SIGN_IN_TYPE_KEY))
    }

    @Test
    fun createUserFailureTest() {
        val responseBody = """
            {
                "errorMessage": "Email taken",
                "errorMessageId": "login_email_taken"
            }
        """
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody(responseBody)
        mockWebServer.enqueue(response)

        val result = runBlocking {
            syncManager.createUserWithEmailAndPassword(
                email = "support@pocketcasts.com",
                password = "password",
            )
        }
        assert(result is LoginResult.Failed)
        val failedResult = result as LoginResult.Failed
        assertEquals("login_email_taken", failedResult.messageId)
        assertEquals("Email taken", failedResult.message)
    }
}
