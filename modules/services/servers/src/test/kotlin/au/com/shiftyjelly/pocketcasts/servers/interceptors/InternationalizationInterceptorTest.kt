package au.com.shiftyjelly.pocketcasts.servers.interceptors

import au.com.shiftyjelly.pocketcasts.servers.interceptors.InternationalizationInterceptor.Companion.APP_LANGUAGE_HEADER
import au.com.shiftyjelly.pocketcasts.servers.interceptors.InternationalizationInterceptor.Companion.USER_REGION_HEADER
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InternationalizationInterceptorTest {
    @get:Rule
    val server = MockWebServer()
    private val url = server.url("/")

    private var locale = Locale.US
    private var region = "gb"
    private val client = OkHttpClient.Builder()
        .addInterceptor(
            InternationalizationInterceptor(
                allowedHosts = listOf(url.host),
                provideLocale = { locale },
                provideRegion = { region },
            ),
        )
        .build()

    @Test
    fun `add i18n headers`() {
        val request = Request.Builder().url(url).build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        var serverRequest = server.takeRequest()
        assertEquals("en-US", serverRequest.getHeader(APP_LANGUAGE_HEADER))
        assertEquals("gb", serverRequest.getHeader(USER_REGION_HEADER))

        locale = Locale.GERMANY
        region = "us"

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        serverRequest = server.takeRequest()
        assertEquals("de-DE", serverRequest.getHeader(APP_LANGUAGE_HEADER))
        assertEquals("us", serverRequest.getHeader(USER_REGION_HEADER))
    }

    @Test
    fun `add partial language header`() {
        locale = Locale.of("pl")
        val request = Request.Builder().url(url).build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        val serverRequest = server.takeRequest()
        assertEquals("pl", serverRequest.getHeader(APP_LANGUAGE_HEADER))
    }

    @Test
    fun `do not add empty headers`() {
        locale = Locale.ROOT
        region = ""
        val request = Request.Builder().url(url).build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        val serverRequest = server.takeRequest()
        assertEquals(null, serverRequest.getHeader(APP_LANGUAGE_HEADER))
        assertEquals(null, serverRequest.getHeader(USER_REGION_HEADER))
    }

    @Test
    fun `do not add headers to unknown hosts`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(
                InternationalizationInterceptor(
                    allowedHosts = listOf("allowed-host.com"),
                    provideLocale = { locale },
                    provideRegion = { region },
                ),
            )
            .build()
        val request = Request.Builder().url(url).build()

        server.enqueue(MockResponse())
        client.newCall(request).execute()

        val serverRequest = server.takeRequest()
        assertEquals(null, serverRequest.getHeader(APP_LANGUAGE_HEADER))
        assertEquals(null, serverRequest.getHeader(USER_REGION_HEADER))
    }
}
