package au.com.shiftyjelly.pocketcasts.repositories.download

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloader.Result
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpisodeDownloaderTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    private val server = MockWebServer()

    private val progressCache = DownloadProgressCache()

    private lateinit var episode: PodcastEpisode

    private val downloader = EpisodeDownloader(
        httpClient = ::OkHttpClient,
        progressCache = progressCache,
    )

    @Before
    fun setUp() {
        server.start()
        episode = PodcastEpisode(
            uuid = "episode-uuid",
            downloadUrl = server.url("/episode.mp3").toString(),
            publishedDate = Date(),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `download episode file`() {
        val downloadFile = tempDir.newFile("file.mp3")

        server.enqueue(MockResponse(body = "Hello, world!"))
        val result = downloader.download(
            episode = episode,
            downloadFile = downloadFile,
            tempFile = tempDir.newFile("file.tmp"),
        )

        assertEquals(Result.Success(downloadFile), result)
        assertEquals(100, progressCache.progressFlow(episode.uuid).value?.percentage)
        assertEquals(server.takeRequest().url, episode.downloadUrl?.toHttpUrl())
        assertEquals("Hello, world!", downloadFile.readText())
    }

    @Test
    fun `clean up resources on successful download`() {
        val tempFile = tempDir.newFile("file.tmp")

        server.enqueue(MockResponse(body = "Hello, world!"))
        downloader.download(
            episode = episode,
            downloadFile = tempDir.newFile("file.mp3"),
            tempFile = tempFile,
        )

        assertFalse(tempFile.exists())
    }

    @Test
    fun `fail to download episode on invalid url failure`() {
        val result = downloader.download(
            episode = episode.copy(downloadUrl = "this-is-not-url"),
            downloadFile = tempDir.newFile("file.mp3"),
            tempFile = tempDir.newFile("file.tmp"),
        )

        assertEquals(Result.InvalidDownloadUrl("this-is-not-url"), result)
    }

    @Test
    fun `clean up resources on invalid url failure`() {
        val downloadFile = tempDir.newFile("file.mp3")
        val tempFile = tempDir.newFile("file.tmp")

        downloader.download(
            episode = episode.copy(downloadUrl = "this-is-not-url"),
            downloadFile = downloadFile,
            tempFile = tempFile,
        )

        assertFalse(downloadFile.exists())
        assertFalse(tempFile.exists())
        assertNull(progressCache.progressFlow(episode.uuid).value)
    }

    @Test
    fun `fail to download episode on http failure`() {
        server.enqueue(MockResponse(code = 404))
        val result = downloader.download(
            episode = episode,
            downloadFile = tempDir.newFile("file.mp3"),
            tempFile = tempDir.newFile("file.tmp"),
        )

        assertEquals(Result.UnsuccessfulHttpCall(404), result)
    }

    @Test
    fun `clean up resources on http failure`() {
        val downloadFile = tempDir.newFile("file.mp3")
        val tempFile = tempDir.newFile("file.tmp")

        server.enqueue(MockResponse(code = 404))
        downloader.download(
            episode = episode,
            downloadFile = downloadFile,
            tempFile = tempFile,
        )

        assertFalse(downloadFile.exists())
        assertFalse(tempFile.exists())
        assertNull(progressCache.progressFlow(episode.uuid).value)
    }

    @Test
    fun `fail to download episode on connection failure`() {
        val response = MockResponse.Builder()
            .onRequestStart(SocketEffect.CloseSocket())
            .build()

        server.enqueue(response)
        val result = downloader.download(
            episode = episode,
            downloadFile = tempDir.newFile("file.mp3"),
            tempFile = tempDir.newFile("file.tmp"),
        )

        assertTrue(result is Result.ExceptionFailure)
    }

    @Test
    fun `clean up resources on connection failure`() {
        val downloadFile = tempDir.newFile("file.mp3")
        val tempFile = tempDir.newFile("file.tmp")
        val response = MockResponse.Builder()
            .onRequestStart(SocketEffect.CloseSocket())
            .build()

        server.enqueue(response)
        downloader.download(
            episode = episode,
            downloadFile = downloadFile,
            tempFile = tempFile,
        )

        assertFalse(downloadFile.exists())
        assertFalse(tempFile.exists())
        assertNull(progressCache.progressFlow(episode.uuid).value)
    }

    @Test
    fun `update download progress`() = runTest {
        val contentLength = 30_000L
        val response = MockResponse.Builder()
            .body(Buffer().write(Random.nextBytes(contentLength.toInt())))
            .build()

        progressCache.progressFlow(episode.uuid).test {
            // Verify initial no progress
            assertNull(awaitItem())

            server.enqueue(response)
            downloader.download(
                episode = episode,
                downloadFile = tempDir.newFile("file.mp3"),
                tempFile = tempDir.newFile("file.tmp"),
            )

            // Verify zero progress before download starts
            var progress = awaitItem()
            assertEquals(DownloadProgress(0, null), progress)

            // Verify all progress steps. It's not possible to check intermediate
            // progress exact values because HTTP client doesn't read response body
            // in byte chunks of a constant size.
            while (progress?.downloadedByteCount != contentLength) {
                val progressStep = requireNotNull(awaitItem())
                assertEquals(progressStep.contentLength, contentLength)
                assertTrue(progressStep.downloadedByteCount <= contentLength)
                progress = progressStep
            }

            // Verify that last step downloaded fully
            assertEquals(DownloadProgress(contentLength, contentLength), progress)

            cancel()
        }
    }
}
