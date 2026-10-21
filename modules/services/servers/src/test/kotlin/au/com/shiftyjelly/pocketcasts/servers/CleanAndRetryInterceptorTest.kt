package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Rule
import org.junit.Test

class CleanAndRetryInterceptorTest {
    @get:Rule
    val server = MockWebServer()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()
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
    fun `replace User-Agent header with the OkHttp default when there is no retry User-Agent`() {
        val serverRequest = retryWithUserAgent(retryUserAgent = null)

        assertTrue(serverRequest.getHeader("User-Agent")!!.startsWith("okhttp"))
    }

    @Test
    fun `replace User-Agent header with the retry User-Agent`() {
        val serverRequest = retryWithUserAgent(retryUserAgent = "Dalvik/2.1.0 (Linux; U; Android 16)")

        assertEquals("Dalvik/2.1.0 (Linux; U; Android 16)", serverRequest.getHeader("User-Agent"))
    }

    @Test
    fun `replace User-Agent header with the OkHttp default when the flag is disabled`() {
        FeatureFlag.setEnabled(Feature.RETRY_WITH_PLATFORM_USER_AGENT, false)

        val serverRequest = retryWithUserAgent(retryUserAgent = "Dalvik/2.1.0 (Linux; U; Android 16)")

        assertTrue(serverRequest.getHeader("User-Agent")!!.startsWith("okhttp"))
    }

    @Test
    fun `keep the original User-Agent on the first attempt`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("User-Agent"), retryUserAgent = "retry"))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        assertEquals("value", server.takeRequest().getHeader("User-Agent"))
    }

    @Test
    fun `report the failed response before retrying`() {
        val retries = mutableListOf<Pair<String, Int>>()
        val client = OkHttpClient.Builder()
            .addInterceptor(
                CleanAndRetryInterceptor(
                    headersToRemove = emptyList(),
                    onRetry = { request, response -> retries += request.url.host to response.code },
                ),
            )
            .build()

        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse())
        client.newCall(Request.Builder().url(url).build()).execute()

        assertEquals(listOf(url.host to 503), retries)
    }

    @Test
    fun `do not report successful responses`() {
        val retries = mutableListOf<Int>()
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = emptyList(), onRetry = { _, response -> retries += response.code }))
            .build()

        server.enqueue(MockResponse())
        client.newCall(Request.Builder().url(url).build()).execute()

        assertTrue(retries.isEmpty())
    }

    private fun retryWithUserAgent(retryUserAgent: String?): RecordedRequest {
        val client = OkHttpClient.Builder()
            .addInterceptor(CleanAndRetryInterceptor(headersToRemove = listOf("User-Agent"), retryUserAgent = retryUserAgent))
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "value")
            .build()

        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse())
        client.newCall(request).execute()

        server.takeRequest() // Initial request
        return server.takeRequest()
    }
}
