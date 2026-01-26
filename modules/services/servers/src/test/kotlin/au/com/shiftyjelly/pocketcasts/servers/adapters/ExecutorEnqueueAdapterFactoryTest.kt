package au.com.shiftyjelly.pocketcasts.servers.adapters

import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import okhttp3.Call as OkHttpCall

class ExecutorEnqueueAdapterFactoryTest {
    @get:Rule
    val server = MockWebServer()

    private val factoryExecutor = Executors.newSingleThreadExecutor()

    interface Service {
        @GET("/")
        suspend fun coroutineCall()

        @GET("/")
        fun regularCall(): Call<Void>
    }

    @After
    fun tearDown() {
        factoryExecutor.shutdown()
    }

    @Test
    fun `call factory is not executed on the calling thread when enqueueing call`() = runBlocking {
        val mainThread = Thread.currentThread()
        val calledOnMain = AtomicBoolean(true) // Setting to true to avoid false positive in the assertion

        val callFactory = OkHttpCall.Factory { request ->
            calledOnMain.set(mainThread === Thread.currentThread())
            OkHttpClient().newCall(request)
        }
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .callFactory(callFactory)
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(factoryExecutor))
            .build()
            .create<Service>()

        server.enqueue(MockResponse())
        service.coroutineCall()

        assertFalse(calledOnMain.get())
    }

    @Test
    fun `call can be cancelled`() {
        val latch = CountDownLatch(1)
        var exception: Throwable? = null

        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(factoryExecutor))
            .build()
            .create<Service>()

        val call = service.regularCall()
        call.enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                exception = t
                latch.countDown()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) = fail("Should never happen")
        })

        call.cancel()
        latch.await()

        assertTrue(exception is IOException)
        assertEquals("Canceled", exception?.message)
    }

    @Test
    fun `call can be enqueued`() {
        val latch = CountDownLatch(1)
        var callResponse: Response<Void>? = null

        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(factoryExecutor))
            .build()
            .create<Service>()

        server.enqueue(MockResponse())
        service.regularCall().enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                callResponse = response
                latch.countDown()
            }

            override fun onFailure(call: Call<Void>, t: Throwable) = fail("Should never happen")
        })

        latch.await()

        assertTrue(callResponse?.isSuccessful == true)
    }

    @Test
    fun `call can be executed`() {
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(factoryExecutor))
            .build()
            .create<Service>()

        server.enqueue(MockResponse())
        val response = service.regularCall().execute()

        assertTrue(response.isSuccessful)
    }

    @Test
    fun `call can be cloned`() {
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(factoryExecutor))
            .build()
            .create<Service>()

        server.enqueue(MockResponse())
        val call = service.regularCall()
        call.execute()

        server.enqueue(MockResponse())
        val response = call.clone().execute()

        assertTrue(response.isSuccessful)
    }
}
