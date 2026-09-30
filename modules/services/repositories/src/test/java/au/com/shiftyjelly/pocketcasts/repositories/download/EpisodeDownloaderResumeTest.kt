package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.EpisodeDownloader.Result
import java.io.File
import java.util.Date
import kotlin.random.Random
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpisodeDownloaderResumeTest {
    @get:Rule
    val tempDir = TemporaryFolder()

    private val server = MockWebServer()

    private val progressCache = DownloadProgressCache()

    private val payload = Random.nextBytes(300 * 1024)

    private lateinit var episode: PodcastEpisode

    private lateinit var downloadFile: File

    private lateinit var tempFile: File

    private val downloader = EpisodeDownloader(
        httpClient = ::OkHttpClient,
        progressCache = progressCache,
        minContentLength = 10L,
        isResumeEnabled = { true },
    )

    @Before
    fun setUp() {
        server.start()
        episode = PodcastEpisode(
            uuid = "episode-uuid",
            downloadUrl = server.url("/episode.mp3").toString(),
            publishedDate = Date(),
        )
        downloadFile = tempDir.newFile("file.mp3")
        tempFile = File(tempDir.root, "file.tmp")
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `keep partial download and its validator after a dropped connection`() {
        server.enqueue(
            fullResponse()
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )

        val result = download()

        assertTrue(result is Result.ExceptionFailure)
        assertTrue(tempFile.length() in PartialDownload.OVERLAP_BYTE_COUNT until payload.size)
        assertEquals(PartialDownload(ENTITY_TAG, null, payload.size.toLong()), PartialDownload.readFrom(tempFile))
    }

    @Test
    fun `resume a dropped download on the next attempt`() {
        server.enqueue(
            fullResponse()
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )
        download()
        val partialLength = tempFile.length()
        server.enqueue(partialResponse(from = partialLength - PartialDownload.OVERLAP_BYTE_COUNT).build())

        val result = download()

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        assertFalse(tempFile.exists())
        assertFalse(metadataFile().exists())
    }

    @Test
    fun `resume from the overlap with the entity tag`() {
        givenPartialDownload(length = 200 * 1024)
        val offset = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT
        server.enqueue(partialResponse(from = offset).build())

        val result = download()

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        val request = server.takeRequest()
        assertEquals("bytes=$offset-", request.headers["Range"])
        assertEquals(ENTITY_TAG, request.headers["If-Range"])
        assertEquals(1, server.requestCount)
        assertEquals(DownloadProgress(payload.size.toLong(), payload.size.toLong()), progressCache.progressFlow(episode.uuid).value)
    }

    @Test
    fun `resume with the last modified date when there is no entity tag`() {
        givenPartialDownload(length = 200 * 1024, entityTag = null, lastModified = LAST_MODIFIED)
        val offset = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT
        server.enqueue(partialResponse(from = offset, entityTag = null, lastModified = LAST_MODIFIED).build())

        val result = download()

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        assertEquals(LAST_MODIFIED, server.takeRequest().headers["If-Range"])
    }

    @Test
    fun `resume a partial download that already has every byte`() {
        givenPartialDownload(length = payload.size)
        server.enqueue(partialResponse(from = payload.size - PartialDownload.OVERLAP_BYTE_COUNT).build())

        val result = download()

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
    }

    @Test
    fun `do not save resume metadata without a validator`() {
        server.enqueue(
            fullResponse(entityTag = null)
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )

        download()

        assertFalse(metadataFile().exists())
        assertNull(PartialDownload.readFrom(tempFile))
    }

    @Test
    fun `do not save resume metadata for a weak entity tag`() {
        server.enqueue(
            fullResponse(entityTag = "W/\"weak\"")
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )

        download()

        assertFalse(metadataFile().exists())
    }

    @Test
    fun `download from the start without a range when the partial is shorter than the overlap`() {
        givenPartialDownload(length = 1024)
        server.enqueue(fullResponse().build())

        val result = download()

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        assertNull(server.takeRequest().headers["Range"])
    }

    @Test
    fun `download from the start when the server ignores the range`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(fullResponse().build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the entity tag changed`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(partialResponse(from = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT, entityTag = "\"other\"").build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the file length changed`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(partialResponse(from = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT, total = payload.size + 1L).build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the range starts somewhere else`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(partialResponse(from = 0).build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the overlapping bytes differ`() {
        givenPartialDownload(length = 200 * 1024)
        val otherPayload = Random.nextBytes(payload.size)
        server.enqueue(partialResponse(from = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT, bytes = otherPayload).build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the range is not satisfiable`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(MockResponse.Builder().code(416).build())
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `download from the start when the resumed content type is invalid`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(
            partialResponse(from = 200 * 1024 - PartialDownload.OVERLAP_BYTE_COUNT)
                .setHeader("Content-Type", "text/html")
                .build(),
        )
        server.enqueue(fullResponse().build())

        val result = download()

        assertDownloadedFromStartAfterRejection(result)
    }

    @Test
    fun `discard the partial download on an http failure`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(MockResponse.Builder().code(500).build())
        server.enqueue(MockResponse.Builder().code(500).build())

        val result = download()

        assertTrue(result is Result.UnsuccessfulHttpCall)
        assertFalse(tempFile.exists())
        assertFalse(metadataFile().exists())
    }

    @Test
    fun `discard the partial download on request`() {
        givenPartialDownload(length = 200 * 1024)

        downloader.discardPartialDownload(tempFile)

        assertFalse(tempFile.exists())
        assertFalse(metadataFile().exists())
    }

    @Test
    fun `ignore a partial download when resume is disabled`() {
        givenPartialDownload(length = 200 * 1024)
        server.enqueue(fullResponse().build())
        val disabledDownloader = EpisodeDownloader(
            httpClient = ::OkHttpClient,
            progressCache = progressCache,
            minContentLength = 10L,
            isResumeEnabled = { false },
        )

        val result = disabledDownloader.download(episode, downloadFile, tempFile)

        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        assertNull(server.takeRequest().headers["Range"])
        assertFalse(tempFile.exists())
        assertFalse(metadataFile().exists())
    }

    @Test
    fun `do not keep a partial download when resume is disabled`() {
        server.enqueue(
            fullResponse()
                .onResponseBody(SocketEffect.ShutdownConnection)
                .build(),
        )
        val disabledDownloader = EpisodeDownloader(
            httpClient = ::OkHttpClient,
            progressCache = progressCache,
            minContentLength = 10L,
            isResumeEnabled = { false },
        )

        disabledDownloader.download(episode, downloadFile, tempFile)

        assertFalse(tempFile.exists())
        assertFalse(metadataFile().exists())
    }

    private fun download() = downloader.download(episode, downloadFile, tempFile)

    private fun assertDownloadedFromStartAfterRejection(result: Result) {
        assertEquals(Result.Success(downloadFile), result)
        assertArrayEquals(payload, downloadFile.readBytes())
        assertEquals(2, server.requestCount)
        server.takeRequest()
        assertNull(server.takeRequest().headers["Range"])
    }

    private fun givenPartialDownload(length: Int, entityTag: String? = ENTITY_TAG, lastModified: String? = null) {
        tempFile.writeBytes(payload.copyOf(length))
        PartialDownload(entityTag, lastModified, payload.size.toLong()).writeTo(tempFile)
    }

    private fun fullResponse(entityTag: String? = ENTITY_TAG, lastModified: String? = null) = MockResponse.Builder()
        .apply { entityTag?.let { addHeader("ETag", it) } }
        .apply { lastModified?.let { addHeader("Last-Modified", it) } }
        .addHeader("Content-Type", "audio/mpeg")
        .body(Buffer().write(payload))

    private fun partialResponse(
        from: Long,
        entityTag: String? = ENTITY_TAG,
        lastModified: String? = null,
        total: Long = payload.size.toLong(),
        bytes: ByteArray = payload,
    ) = MockResponse.Builder()
        .code(206)
        .apply { entityTag?.let { addHeader("ETag", it) } }
        .apply { lastModified?.let { addHeader("Last-Modified", it) } }
        .addHeader("Content-Type", "audio/mpeg")
        .addHeader("Content-Range", "bytes $from-${total - 1}/$total")
        .body(Buffer().write(bytes, from.toInt(), bytes.size - from.toInt()))

    private fun metadataFile() = File("${tempFile.path}.resume")

    private companion object {
        const val ENTITY_TAG = "\"v1\""
        const val LAST_MODIFIED = "Tue, 29 Sep 2026 10:00:00 GMT"
    }
}
