package au.com.shiftyjelly.pocketcasts.servers.adapters

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import okhttp3.Call as OkHttpCall

class ExecutorEnqueueAdapterFactoryTest {
    @get:Rule
    val server = MockWebServer()

    interface Service {
        @GET("/")
        suspend fun call()
    }

    @Test
    fun `call factory is not executed on the calling thread`() = runBlocking {
        val mainThread = Thread.currentThread()
        val calledOnMain = AtomicBoolean(true) // Setting to true to avoid false positive in the assertion
        val executor = Executors.newSingleThreadExecutor()

        val callFactory = OkHttpCall.Factory { request ->
            calledOnMain.set(mainThread === Thread.currentThread())
            OkHttpClient().newCall(request)
        }
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .callFactory(callFactory)
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(executor))
            .build()
            .create<Service>()

        server.enqueue(MockResponse())
        service.call()
        executor.shutdown()

        assertFalse(calledOnMain.get())
    }
}
