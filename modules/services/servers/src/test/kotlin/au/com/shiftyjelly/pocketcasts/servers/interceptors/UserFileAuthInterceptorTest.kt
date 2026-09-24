package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import java.net.HttpURLConnection
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UserFileAuthInterceptorTest {
    @get:Rule
    val apiServer = MockWebServer()

    @get:Rule
    val otherServer = MockWebServer()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `adds authorization header to user file requests`() {
        val tokenHandler = FakeTokenHandler(AccessToken("access-token"))
        val client = newClient(tokenHandler)
        apiServer.enqueue(MockResponse())

        client.newCall(userFileRequest()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = apiServer.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("Bearer access-token", request?.getHeader("Authorization"))
    }

    @Test
    fun `does not add authorization header for other hosts`() {
        val tokenHandler = FakeTokenHandler(AccessToken("access-token"))
        val client = newClient(tokenHandler)
        otherServer.enqueue(MockResponse())

        val url = otherServer.url("/files/url/token/episode-uuid")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = requireNotNull(otherServer.takeRequest(5, TimeUnit.SECONDS))
        assertNull(request.getHeader("Authorization"))
        assertEquals(0, tokenHandler.getAccessTokenCalls)
    }

    @Test
    fun `does not add authorization header for other paths on the api host`() {
        val tokenHandler = FakeTokenHandler(AccessToken("access-token"))
        val client = newClient(tokenHandler)
        apiServer.enqueue(MockResponse())

        val url = apiServer.url("/files/url/episode-uuid")
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = requireNotNull(apiServer.takeRequest(5, TimeUnit.SECONDS))
        assertNull(request.getHeader("Authorization"))
        assertEquals(0, tokenHandler.getAccessTokenCalls)
    }

    @Test
    fun `does not forward authorization header when redirected to another host`() {
        val tokenHandler = FakeTokenHandler(AccessToken("access-token"))
        val client = newClient(tokenHandler)
        val redirect = MockResponse()
            .setResponseCode(HTTP_TEMPORARY_REDIRECT)
            .setHeader("Location", otherServer.url("/signed-file.mp3"))
        apiServer.enqueue(redirect)
        otherServer.enqueue(MockResponse())

        client.newCall(userFileRequest()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val apiRequest = requireNotNull(apiServer.takeRequest(5, TimeUnit.SECONDS))
        val storageRequest = requireNotNull(otherServer.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("Bearer access-token", apiRequest.getHeader("Authorization"))
        assertNull(storageRequest.getHeader("Authorization"))
    }

    @Test
    fun `retries with a refreshed token when unauthorized`() {
        val tokenHandler = FakeTokenHandler(AccessToken("expired-access-token"), AccessToken("fresh-access-token"))
        val client = newClient(tokenHandler)
        apiServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))
        apiServer.enqueue(MockResponse())

        client.newCall(userFileRequest()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val firstRequest = requireNotNull(apiServer.takeRequest(5, TimeUnit.SECONDS))
        val secondRequest = requireNotNull(apiServer.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("Bearer expired-access-token", firstRequest.getHeader("Authorization"))
        assertEquals("Bearer fresh-access-token", secondRequest.getHeader("Authorization"))
        assertTrue(tokenHandler.invalidatedAccessToken)
    }

    @Test
    fun `does not refresh the token when the redirected storage host is unauthorized`() {
        val tokenHandler = FakeTokenHandler(AccessToken("access-token"), AccessToken("fresh-access-token"))
        val client = newClient(tokenHandler)
        val redirect = MockResponse()
            .setResponseCode(HTTP_TEMPORARY_REDIRECT)
            .setHeader("Location", otherServer.url("/signed-file.mp3"))
        apiServer.enqueue(redirect)
        otherServer.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))

        client.newCall(userFileRequest()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.code)
        }

        assertEquals(1, tokenHandler.getAccessTokenCalls)
        assertFalse(tokenHandler.invalidatedAccessToken)
    }

    private companion object {
        const val HTTP_TEMPORARY_REDIRECT = 307
    }

    private fun userFileRequest() = Request.Builder()
        .url(apiServer.url("/files/url/token/episode-uuid"))
        .build()

    private fun newClient(tokenHandler: TokenHandler): OkHttpClient {
        val interceptor = UserFileAuthInterceptor(apiUrl = apiServer.url("/"), tokenHandler = tokenHandler)
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    private class FakeTokenHandler(
        vararg tokens: AccessToken,
    ) : TokenHandler {
        private val tokens = ArrayDeque(tokens.toList())

        var getAccessTokenCalls = 0
            private set

        var invalidatedAccessToken = false
            private set

        override suspend fun getAccessToken(): AccessToken? {
            getAccessTokenCalls++
            return tokens.pollFirst()
        }

        override fun invalidateAccessToken() {
            invalidatedAccessToken = true
        }
    }
}
