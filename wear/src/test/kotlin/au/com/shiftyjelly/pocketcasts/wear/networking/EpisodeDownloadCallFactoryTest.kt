package au.com.shiftyjelly.pocketcasts.wear.networking

import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloadRequest
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.okhttp.impl.RequestTypeHolder.Companion.requestType
import com.google.android.horologist.networks.okhttp.requestTypeOrNull
import java.io.IOException
import kotlin.reflect.KClass
import okhttp3.Call
import okhttp3.Callback
import okhttp3.EventListener
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeDownloadCallFactoryTest {
    private val delegate = FakeCallFactory()

    private val factory = EpisodeDownloadCallFactory(delegate)

    @Test
    fun `map a download that waits for wifi to a wifi only download`() {
        factory.newCall(downloadRequest(waitForWifi = true))

        assertEquals(WifiOnlyDownloadRequest, delegate.calls.single().request().requestTypeOrNull)
    }

    @Test
    fun `map a download on any network to a media download`() {
        factory.newCall(downloadRequest(waitForWifi = false))

        assertEquals(RequestType.MediaRequest.DownloadRequest, delegate.calls.single().request().requestTypeOrNull)
    }

    @Test
    fun `leave requests that are not episode downloads unchanged`() {
        val request = Request.Builder().url(URL).build()

        factory.newCall(request)

        assertNull(delegate.calls.single().request().requestTypeOrNull)
    }

    @Test
    fun `keep a request type that is already set`() {
        val request = downloadRequest(waitForWifi = true).newBuilder()
            .requestType(RequestType.MediaRequest.StreamRequest)
            .build()

        factory.newCall(request)

        assertEquals(RequestType.MediaRequest.StreamRequest, delegate.calls.single().request().requestTypeOrNull)
    }

    @Test
    fun `keep the range headers of a download`() {
        val request = downloadRequest(waitForWifi = true).newBuilder()
            .header("Range", "bytes=100-")
            .header("If-Range", "\"v1\"")
            .build()

        factory.newCall(request)

        val sentRequest = delegate.calls.single().request()
        assertEquals("bytes=100-", sentRequest.header("Range"))
        assertEquals("\"v1\"", sentRequest.header("If-Range"))
    }

    @Test
    fun `fail a download once when it is cancelled before the network is ready`() {
        val call = factory.newCall(downloadRequest(waitForWifi = true))
        val callback = RecordingCallback()
        call.enqueue(callback)

        call.cancel()
        call.cancel()
        delegate.calls.single().respond()

        assertEquals(1, callback.failures.size)
        assertEquals(0, callback.responses.size)
        assertTrue(delegate.calls.single().isCanceled())
    }

    @Test
    fun `pass the response of a download through once`() {
        val call = factory.newCall(downloadRequest(waitForWifi = false))
        val callback = RecordingCallback()
        call.enqueue(callback)

        delegate.calls.single().respond()
        call.cancel()

        assertEquals(1, callback.responses.size)
        assertEquals(0, callback.failures.size)
    }

    private fun downloadRequest(waitForWifi: Boolean) = Request.Builder()
        .url(URL)
        .tag(EpisodeDownloadRequest::class.java, EpisodeDownloadRequest(waitForWifi))
        .build()

    private class FakeCallFactory : Call.Factory {
        val calls = mutableListOf<FakeCall>()

        override fun newCall(request: Request): Call = FakeCall(request).also(calls::add)
    }

    private class FakeCall(private val request: Request) : Call {
        private var callback: Callback? = null
        private var isCanceled = false

        fun respond() {
            val response = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .build()
            callback?.onResponse(this, response)
        }

        override fun request() = request

        override fun execute() = throw UnsupportedOperationException()

        override fun enqueue(responseCallback: Callback) {
            callback = responseCallback
        }

        override fun cancel() {
            isCanceled = true
        }

        override fun isExecuted() = callback != null

        override fun isCanceled() = isCanceled

        override fun timeout() = Timeout.NONE

        override fun addEventListener(eventListener: EventListener) = Unit

        override fun <T : Any> tag(type: KClass<T>): T? = null

        override fun <T> tag(type: Class<out T>): T? = null

        override fun <T : Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        override fun <T : Any> tag(type: Class<T>, computeIfAbsent: () -> T): T = computeIfAbsent()

        override fun clone() = FakeCall(request)
    }

    private class RecordingCallback : Callback {
        val failures = mutableListOf<IOException>()
        val responses = mutableListOf<Response>()

        override fun onFailure(call: Call, e: IOException) {
            failures += e
        }

        override fun onResponse(call: Call, response: Response) {
            responses += response
        }
    }

    private companion object {
        const val URL = "https://example.com/episode.mp3"
    }
}
