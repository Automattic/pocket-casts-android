package au.com.shiftyjelly.pocketcasts.servers.interceptors

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class BasicAuthInterceptorTest {
    @get:Rule
    val server = MockWebServer()

    private val client = OkHttpClient.Builder()
        .addInterceptor(BasicAuthInterceptor())
        .build()
    private val url = server.url("/")
    private val urlWithCredentials = url.newBuilder()
        .username("username")
        .password("password")
        .build()

    @Test
    fun `when unauthorized add an authorization header`() {
        val request = Request.Builder()
            .url(urlWithCredentials)
            .build()

        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Basic realm=\"Please enter username and password\""),
        )
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        assertEquals(2, server.requestCount)
        server.takeRequest()
        val serverRequest = server.takeRequest()
        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", serverRequest.getHeader("Authorization"))
    }

    @Test
    fun `when unauthorized twice don't retry`() {
        val request = Request.Builder()
            .url(urlWithCredentials)
            .build()

        val responseUnauthorized = MockResponse()
            .setResponseCode(401)
            .setHeader("WWW-Authenticate", "Basic realm=\"Please enter username and password\"")

        server.enqueue(responseUnauthorized)
        server.enqueue(responseUnauthorized)
        client.newCall(request).execute()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `when successful don't retry`() {
        val request = Request.Builder()
            .url(urlWithCredentials)
            .build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        assertEquals(1, server.requestCount)
        val serverRequest = server.takeRequest()
        assertNull(serverRequest.getHeader("Authorization"))
    }

    @Test
    fun `when missing credentials don't add authorization header`() {
        val request = Request.Builder()
            .url(url)
            .build()

        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("WWW-Authenticate", "Basic realm=\"Please enter username and password\""),
        )
        client.newCall(request).execute()

        assertEquals(1, server.requestCount)
        val serverRequest = server.takeRequest()
        assertNull(serverRequest.getHeader("Authorization"))
    }
}
