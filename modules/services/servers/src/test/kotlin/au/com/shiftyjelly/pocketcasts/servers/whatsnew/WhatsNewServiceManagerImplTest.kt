package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

@RunWith(RobolectricTestRunner::class)
class WhatsNewServiceManagerImplTest {
    @get:Rule
    val server = MockWebServer()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val client by lazy {
        OkHttpClient.Builder()
            .cache(Cache(tempFolder.newFolder("http-cache"), 1024 * 1024))
            .build()
    }

    private val service by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(NetworkModule().provideMoshi()))
            .build()
            .create<WhatsNewCatalogService>()
    }

    private var locale = Locale.GERMAN
    private val serviceManager by lazy { WhatsNewServiceManagerImpl(service) { locale } }

    @Test
    fun `asks for the catalog published for the app's locale`() = runTest {
        server.enqueue(catalogResponse())

        val catalog = serviceManager.getCatalog()

        assertEquals("/whats-new/v1/android/de.json", server.takeRequest().path)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `revalidates the catalog when asked to skip the cache`() = runTest {
        server.enqueue(catalogResponse())

        serviceManager.getCatalog(CacheControl.FORCE_NETWORK)

        assertEquals("no-cache", server.takeRequest().getHeader("Cache-Control"))
    }

    @Test
    fun `reads the cached catalog without the network`() = runTest {
        server.enqueue(catalogResponse())
        serviceManager.getCatalog()

        val catalog = serviceManager.getCatalog(CacheControl.FORCE_CACHE)

        assertEquals(1, server.requestCount)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `reads the cached english catalog when the locale was never cached`() = runTest {
        locale = Locale.FRENCH
        server.enqueue(MockResponse().setResponseCode(HTTP_FORBIDDEN))
        server.enqueue(catalogResponse())
        serviceManager.getCatalog()

        val catalog = serviceManager.getCatalog(CacheControl.FORCE_CACHE)

        assertEquals(2, server.requestCount)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `an empty cache is reported as a failure`() = runTest {
        locale = Locale.FRENCH

        val error = assertThrows(HttpException::class.java) {
            runBlocking { serviceManager.getCatalog(CacheControl.FORCE_CACHE) }
        }

        assertEquals(HTTP_GATEWAY_TIMEOUT, error.code())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `falls back to the english catalog when the bucket has no catalog for the locale`() = runTest {
        locale = Locale.FRENCH
        server.enqueue(MockResponse().setResponseCode(HTTP_FORBIDDEN))
        server.enqueue(catalogResponse())

        val catalog = serviceManager.getCatalog()

        assertEquals("/whats-new/v1/android/fr.json", server.takeRequest().path)
        assertEquals("/whats-new/v1/android/en.json", server.takeRequest().path)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `falls back to the english catalog when the locale is not found`() = runTest {
        locale = Locale.FRENCH
        server.enqueue(MockResponse().setResponseCode(HTTP_NOT_FOUND))
        server.enqueue(catalogResponse())

        val catalog = serviceManager.getCatalog()

        assertEquals("/whats-new/v1/android/fr.json", server.takeRequest().path)
        assertEquals("/whats-new/v1/android/en.json", server.takeRequest().path)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `a failure other than a missing catalog is not retried in english`() = runTest {
        locale = Locale.FRENCH
        server.enqueue(MockResponse().setResponseCode(500))

        assertThrows(HttpException::class.java) { fetchCatalog() }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a missing english catalog is not asked for twice`() = runTest {
        locale = Locale.ENGLISH
        server.enqueue(MockResponse().setResponseCode(HTTP_FORBIDDEN))

        assertThrows(HttpException::class.java) { fetchCatalog() }
        assertEquals(1, server.requestCount)
    }

    private fun fetchCatalog() = runBlocking { serviceManager.getCatalog() }

    private fun catalogResponse() = MockResponse().setHeader("Cache-Control", "max-age=1800").setBody(
        """
        {
          "schemaVersion": 1,
          "generatedAt": "2026-09-22T11:39:17Z",
          "platform": "android",
          "locale": "en",
          "messages": [
            {
              "id": "m1",
              "type": "new_feature",
              "publishedAt": "2026-09-18T05:11:26Z",
              "targeting": { "audiences": ["free", "plus", "patron"] },
              "title": "Browse by network",
              "pages": [{ "heading": "You already trust the name", "description": "Browse networks in Discover." }]
            }
          ]
        }
        """.trimIndent(),
    )
}
