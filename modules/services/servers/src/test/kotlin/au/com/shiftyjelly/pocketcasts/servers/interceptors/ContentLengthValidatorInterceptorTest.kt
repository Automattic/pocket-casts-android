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
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentLengthValidatorInterceptorTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun `complete body passes through`() {
        val body = readWholeBody(declaredLength = MB, deliveredBytes = MB.toInt())

        assertEquals(MB.toInt(), body.size)
    }

    @Test
    fun `grossly truncated body throws`() {
        assertThrows(ProtocolException::class.java) {
            readWholeBody(declaredLength = 2 * MB, deliveredBytes = MB.toInt())
        }
    }

    @Test
    fun `shortfall within absolute tolerance passes through`() {
        val body = readWholeBody(declaredLength = MB, deliveredBytes = 600_000)

        assertEquals(600_000, body.size)
    }

    @Test
    fun `shortfall above ninety percent delivered passes through`() {
        val deliveredBytes = (6 * MB - 600 * KB).toInt()

        val body = readWholeBody(declaredLength = 6 * MB, deliveredBytes = deliveredBytes)

        assertEquals(deliveredBytes, body.size)
    }

    @Test
    fun `unknown content length is not validated`() {
        val body = readWholeBody(declaredLength = -1L, deliveredBytes = 1_000)

        assertEquals(1_000, body.size)
    }

    @Test
    fun `head request is not validated`() {
        val body = readWholeBody(declaredLength = 2 * MB, deliveredBytes = 1_000, method = "HEAD")

        assertEquals(1_000, body.size)
    }

    @Test
    fun `non success response is not validated`() {
        val body = readWholeBody(declaredLength = 2 * MB, deliveredBytes = 1_000, code = 302)

        assertEquals(1_000, body.size)
    }

    @Test
    fun `disabled flag skips validation`() {
        FeatureFlag.setEnabled(Feature.VALIDATE_CONTENT_LENGTH, false)

        val body = readWholeBody(declaredLength = 2 * MB, deliveredBytes = 1_000)

        assertEquals(1_000, body.size)
    }

    @Test
    fun `closing before end of stream does not throw`() {
        execute(declaredLength = 2 * MB, deliveredBytes = MB.toInt()).use { response ->
            val read = response.body.source().read(Buffer(), 1_024)
            assertTrue(read > 0)
        }
    }

    private fun readWholeBody(
        declaredLength: Long,
        deliveredBytes: Int,
        method: String = "GET",
        code: Int = 200,
    ): ByteArray {
        return execute(declaredLength, deliveredBytes, method, code).use { response ->
            response.body.source().readByteArray()
        }
    }

    private fun execute(
        declaredLength: Long,
        deliveredBytes: Int,
        method: String = "GET",
        code: Int = 200,
    ): Response {
        val responder = Interceptor { chain ->
            val payload = Buffer().write(ByteArray(deliveredBytes))
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_2)
                .code(code)
                .message("")
                .body(payload.asResponseBody("audio/mpeg".toMediaType(), declaredLength))
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

    private companion object {
        const val KB = 1024L
        const val MB = 1024L * 1024L
    }
}
