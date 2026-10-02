package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.net.ProtocolException
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.blackholeSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class ContentLengthValidatorInterceptorTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @get:Rule
    val server = MockWebServer()

    @Test
    fun `complete response over the network passes through with body intact`() {
        server.enqueue(MockResponse().setBody("hello world"))
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(ContentLengthValidatorInterceptor())
            .build()

        client.newCall(Request.Builder().url(server.url("/episode.mp3")).build()).execute().use { response ->
            assertEquals("hello world", response.body.string())
        }
    }

    @Test
    fun `complete body passes through`() {
        assertEquals(MB, readBody(declaredLength = MB, deliveredBytes = MB))
    }

    @Test
    fun `grossly truncated body throws`() {
        assertThrows(ProtocolException::class.java) {
            readBody(declaredLength = 2 * MB, deliveredBytes = MB)
        }
    }

    @Test
    fun `large truncation beyond the capped tolerance throws`() {
        assertThrows(ProtocolException::class.java) {
            readBody(declaredLength = 200 * MB, deliveredBytes = 200 * MB - 3 * MB)
        }
    }

    @Test
    fun `shortfall within the capped tolerance passes through`() {
        val deliveredBytes = 200 * MB - MB

        assertEquals(deliveredBytes, readBody(declaredLength = 200 * MB, deliveredBytes = deliveredBytes))
    }

    @Test
    fun `shortfall within the absolute floor passes through`() {
        val deliveredBytes = MB - 400 * KB

        assertEquals(deliveredBytes, readBody(declaredLength = MB, deliveredBytes = deliveredBytes))
    }

    @Test
    fun `shortfall equal to the tolerance passes through`() {
        val deliveredBytes = MB - 512 * KB

        assertEquals(deliveredBytes, readBody(declaredLength = MB, deliveredBytes = deliveredBytes))
    }

    @Test
    fun `unknown content length is not validated`() {
        assertEquals(1_000L, readBody(declaredLength = -1L, deliveredBytes = 1_000L))
    }

    @Test
    fun `head request is not validated`() {
        assertEquals(1_000L, readBody(declaredLength = 2 * MB, deliveredBytes = 1_000L, method = "HEAD"))
    }

    @Test
    fun `non success response is not validated`() {
        assertEquals(1_000L, readBody(declaredLength = 2 * MB, deliveredBytes = 1_000L, code = 302))
    }

    @Test
    fun `truncated partial content throws`() {
        assertThrows(ProtocolException::class.java) {
            readBody(declaredLength = 2 * MB, deliveredBytes = MB, code = 206)
        }
    }

    @Test
    fun `disabled flag skips validation`() {
        FeatureFlag.setEnabled(Feature.VALIDATE_CONTENT_LENGTH, false)

        assertEquals(1_000L, readBody(declaredLength = 2 * MB, deliveredBytes = 1_000L))
    }

    @Test
    fun `closing before end of stream does not throw`() {
        execute(declaredLength = 2 * MB, deliveredBytes = MB).use { response ->
            response.body.source().read(Buffer(), 1_024)
        }
    }

    private fun readBody(
        declaredLength: Long,
        deliveredBytes: Long,
        method: String = "GET",
        code: Int = 200,
    ): Long {
        return execute(declaredLength, deliveredBytes, method, code).use { response ->
            response.body.source().readAll(blackholeSink())
        }
    }

    private fun execute(
        declaredLength: Long,
        deliveredBytes: Long,
        method: String = "GET",
        code: Int = 200,
    ): Response {
        val responder = Interceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(code)
                .message("")
                .body(ZeroSource(deliveredBytes).buffer().asResponseBody("audio/mpeg".toMediaType(), declaredLength))
                .build()
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(ContentLengthValidatorInterceptor())
            .addInterceptor(responder)
            .build()
        val request = Request.Builder()
            .url("https://example.com/episode.mp3")
            .method(method, null)
            .build()
        return client.newCall(request).execute()
    }

    private class ZeroSource(private var remaining: Long) : Source {
        override fun read(sink: Buffer, byteCount: Long): Long {
            if (remaining <= 0L) {
                return -1L
            }
            val count = minOf(byteCount, remaining, CHUNK.size.toLong())
            sink.write(CHUNK, 0, count.toInt())
            remaining -= count
            return count
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }

    private companion object {
        const val KB = 1024L
        const val MB = 1024L * 1024L
        val CHUNK = ByteArray(8 * 1024)
    }
}
