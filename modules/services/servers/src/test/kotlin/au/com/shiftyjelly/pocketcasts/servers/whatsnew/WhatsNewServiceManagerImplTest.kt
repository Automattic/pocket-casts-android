package au.com.shiftyjelly.pocketcasts.servers.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.di.NetworkModule
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.util.Locale
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
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

    private val service = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(MoshiConverterFactory.create(NetworkModule().provideMoshi()))
        .build()
        .create<WhatsNewCatalogService>()

    private var locale = Locale.US
    private val serviceManager = WhatsNewServiceManagerImpl(service) { locale }

    @Test
    fun `asks for the catalog published for the app's locale`() = runTest {
        server.enqueue(catalogResponse())

        val catalog = serviceManager.getCatalog()

        assertEquals("/whats-new/v1/android/en.json", server.takeRequest().path)
        assertEquals("Browse by network", catalog.messages.single().title)
    }

    @Test
    fun `falls back to the english catalog when the locale is not published`() = runTest {
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

        assertTrue(catalogError() is HttpException)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `a missing english catalog is not asked for twice`() = runTest {
        server.enqueue(MockResponse().setResponseCode(HTTP_NOT_FOUND))

        assertTrue(catalogError() is HttpException)
        assertEquals(1, server.requestCount)
    }

    private suspend fun catalogError() = runCatching { serviceManager.getCatalog() }.exceptionOrNull()

    private fun catalogResponse() = MockResponse().setBody(
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
