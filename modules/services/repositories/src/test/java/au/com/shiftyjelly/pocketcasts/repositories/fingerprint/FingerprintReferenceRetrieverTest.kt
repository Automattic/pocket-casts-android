package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.GeneratedChaptersDownloadTriggerType
import com.automattic.eventhorizon.GeneratedChaptersFileDownloadedEvent
import com.automattic.eventhorizon.GeneratedChaptersFileType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

class FingerprintReferenceRetrieverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val eventSink = TestEventSink()
    private var isMetered = false

    private lateinit var retriever: FingerprintReferenceRetriever

    @Before
    fun setUp() {
        val connectivityManager = mock(ConnectivityManager::class.java)
        whenever(connectivityManager.isActiveNetworkMetered).thenAnswer { isMetered }
        val context = mock(Context::class.java)
        whenever(context.cacheDir).thenReturn(tempFolder.root)
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).doReturn(connectivityManager)
        retriever = FingerprintReferenceRetriever(OkHttpClient(), context, EventHorizon(eventSink))
    }

    @Test
    fun `saveCachedReference then loadCachedReference round trips`() = runTest {
        retriever.saveCachedReference("episode-1", byteArrayOf(1, 2, 3))

        assertArrayEquals(byteArrayOf(1, 2, 3), retriever.loadCachedReference("episode-1"))
    }

    @Test
    fun `loadCachedReference returns null when missing`() = runTest {
        assertNull(retriever.loadCachedReference("episode-1"))
    }

    @Test
    fun `fetchReferenceData returns Success for a 200 response`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            server.start()

            val result = retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.Playback)

            assertArrayEquals("{}".toByteArray(), (result as FingerprintReferenceRetriever.FetchResult.Success).data)
        }
    }

    @Test
    fun `fetchReferenceData returns NotFound for a 404 response`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            server.start()

            val result = retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.Playback)

            assertEquals(FingerprintReferenceRetriever.FetchResult.NotFound, result)
        }
    }

    @Test
    fun `fetchReferenceData returns Error for an unexpected status`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(400))
            server.start()

            val result = retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.Playback)

            assertTrue(result is FingerprintReferenceRetriever.FetchResult.Error)
        }
    }

    @Test
    fun `fetchReferenceData tracks the compressed bytes received`() = runTest {
        val json = "{\"checkpoints\":[]}".repeat(100).toByteArray()
        val gzipped = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(json) } }.toByteArray()
        isMetered = true
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(Buffer().write(gzipped)))
            server.start()

            val result = retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.ChapterTap)

            assertArrayEquals(json, (result as FingerprintReferenceRetriever.FetchResult.Success).data)
            assertEquals(
                GeneratedChaptersFileDownloadedEvent(
                    file = GeneratedChaptersFileType.Fingerprints,
                    bytes = gzipped.size.toLong(),
                    isMetered = true,
                    trigger = GeneratedChaptersDownloadTriggerType.ChapterTap,
                    success = true,
                    episodeUuid = "episode",
                    podcastUuid = "podcast",
                ),
                eventSink.pollEvent(),
            )
            assertTrue(eventSink.isEmpty())
        }
    }

    @Test
    fun `fetchReferenceData tracks a failed download`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(404))
            server.start()

            retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.TranscriptView)

            assertEquals(
                GeneratedChaptersFileDownloadedEvent(
                    file = GeneratedChaptersFileType.Fingerprints,
                    bytes = 0L,
                    isMetered = false,
                    trigger = GeneratedChaptersDownloadTriggerType.TranscriptView,
                    success = false,
                    episodeUuid = "episode",
                    podcastUuid = "podcast",
                ),
                eventSink.pollEvent(),
            )
        }
    }

    @Test
    fun `fetchReferenceData does not track a download that never reached the server`() = runTest {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)) }
            server.start()

            val result = retriever.fetchReferenceData(server.url("/").toString(), "podcast", "episode", GeneratedChaptersDownloadTriggerType.Playback)

            assertEquals(FingerprintReferenceRetriever.FetchResult.Error(hasResponse = false), result)
            assertTrue(eventSink.isEmpty())
        }
    }

    @Test
    fun `loading a cached reference does not track a download`() = runTest {
        retriever.saveCachedReference("episode-1", byteArrayOf(1, 2, 3))

        retriever.loadCachedReference("episode-1")

        assertTrue(eventSink.isEmpty())
    }

    @Test
    fun `saveCachedReference prunes oldest entries beyond cap`() = runTest {
        val cacheDir = File(tempFolder.root, "episode_fingerprints")
        for (i in 0 until 21) {
            retriever.saveCachedReference("episode-$i", byteArrayOf(i.toByte()))
            File(cacheDir, "episode-$i.ref.fp.json").setLastModified(1_000L * (i + 1))
        }

        retriever.saveCachedReference("episode-new", byteArrayOf(42))

        assertNull(retriever.loadCachedReference("episode-0"))
        assertArrayEquals(byteArrayOf(42), retriever.loadCachedReference("episode-new"))
    }
}
