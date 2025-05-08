package au.com.shiftyjelly.pocketcasts.servers

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test

class CleanAndRetryInterceptorTest {
    @get:Rule
    val server = MockWebServer()
    private val url = server.url("/")

    @Test
    fun `do not remove header from request if response is successful`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("x-custom")))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-custom", "value")
            .build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        val serverRequest = server.takeRequest()

        assertEquals("value", serverRequest.getHeader("X-custom"))
    }

    @Test
    fun `do not remove header from request if response is 304`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("x-custom")))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-custom", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(304))
        client.newCall(request).execute()

        val serverRequest = server.takeRequest()

        assertEquals("value", serverRequest.getHeader("X-custom"))
    }

    @Test
    fun `remove header from request if response is error`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("x-custom")))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-custom", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        server.takeRequest() // Initial request
        val serverRequest = server.takeRequest()

        assertNull(serverRequest.getHeader("x-custom"))
    }

    @Test
    fun `do not remove header that doesn't match filter list`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("x-custom-1")))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-custom-2", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        server.takeRequest() // Initial request
        val serverRequest = server.takeRequest()

        assertEquals("value", serverRequest.getHeader("x-custom-2"))
    }

    @Test
    fun `replace User-Agent header with the default one`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("User-Agent")))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        server.takeRequest() // Initial request
        val serverRequest = server.takeRequest()

        assertTrue(serverRequest.getHeader("User-Agent")!!.startsWith("okhttp"))
    }
}
