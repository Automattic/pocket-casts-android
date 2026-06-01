package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.sync.exception.RefreshTokenExpiredException
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.net.HttpURLConnection
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TokenInterceptorTest {
    @get:Rule
    val server = MockWebServer()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `adds authorization header when token is available`() {
        val tokenHandler = FakeTokenHandler(TokenResult.Token(AccessToken("access-token")))
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url(server.url("/podcasts/search")).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("Bearer access-token", request?.getHeader("Authorization"))
    }

    @Test
    fun `does not ask for token on unauthenticated endpoints`() {
        val tokenHandler = FakeTokenHandler(TokenResult.Token(AccessToken("access-token")))
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url(server.url("/security/token")).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertNull(request.getHeader("Authorization"))
        assertEquals(0, tokenHandler.getAccessTokenCalls)

    @Test
    fun `throws when refresh token has expired and fallback is disabled`() {
        FeatureFlag.setEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK, false)
        val tokenHandler = FakeTokenHandler(TokenResult.Expired)
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse())

        assertThrows(RefreshTokenExpiredException::class.java) {
            client.newCall(Request.Builder().url(server.url("/podcasts/search")).build()).execute()
        }
        assertEquals(1, tokenHandler.getAccessTokenCalls)
    }

    @Test
    fun `continues without authorization when refresh token has expired and fallback is enabled`() {
        FeatureFlag.setEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK, true)
        val tokenHandler = FakeTokenHandler(TokenResult.Expired)
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url(server.url("/podcasts/search")).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertNull(request.getHeader("Authorization"))
        assertEquals(1, tokenHandler.getAccessTokenCalls)

    @Test
    fun `throws when refreshed token has expired and fallback is disabled`() {
        FeatureFlag.setEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK, false)
        val tokenHandler = FakeTokenHandler(
            TokenResult.Token(AccessToken("expired-access-token")),
            TokenResult.Expired,
        )
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))

        assertThrows(RefreshTokenExpiredException::class.java) {
            client.newCall(Request.Builder().url(server.url("/podcasts/search")).build()).execute()
        }

        val firstRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertEquals("Bearer expired-access-token", firstRequest?.getHeader("Authorization"))
        assertEquals(2, tokenHandler.getAccessTokenCalls)
        assertTrue(tokenHandler.invalidatedAccessToken)
    }

    @Test
    fun `continues without authorization when refreshed token has expired and fallback is enabled`() {
        FeatureFlag.setEnabled(Feature.INTERCEPTOR_REFRESH_TOKEN_FALLBACK, true)
        val tokenHandler = FakeTokenHandler(
            TokenResult.Token(AccessToken("expired-access-token")),
            TokenResult.Expired,
        )
        val client = newClient(tokenHandler)
        server.enqueue(MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED))
        server.enqueue(MockResponse())

        client.newCall(Request.Builder().url(server.url("/podcasts/search")).build()).execute().use { response ->
            assertEquals(HttpURLConnection.HTTP_OK, response.code)
        }

        val firstRequest = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        val secondRequest = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS))
        assertEquals("Bearer expired-access-token", firstRequest.getHeader("Authorization"))
        assertNull(secondRequest.getHeader("Authorization"))
        assertEquals(2, tokenHandler.getAccessTokenCalls)
        assertTrue(tokenHandler.invalidatedAccessToken)
    }

    private fun newClient(tokenHandler: TokenHandler): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(InterceptorModule.provideTokenInterceptor(tokenHandler))
            .build()
    }

    private class FakeTokenHandler(
        vararg results: TokenResult,
    ) : TokenHandler {
        private val results = ArrayDeque<TokenResult>().apply { addAll(results.toList()) }

        var getAccessTokenCalls = 0
            private set

        var invalidatedAccessToken = false
            private set

        override suspend fun getAccessToken(): AccessToken? {
            getAccessTokenCalls++
            check(results.isNotEmpty()) { "No token result configured" }
            return when (val result = results.removeFirst()) {
                TokenResult.Expired -> throw RefreshTokenExpiredException()
                is TokenResult.Token -> result.accessToken
            }
        }

        override fun invalidateAccessToken() {
            invalidatedAccessToken = true
        }
    }

    private sealed class TokenResult {
        data class Token(val accessToken: AccessToken?) : TokenResult()
        object Expired : TokenResult()
    }
}
