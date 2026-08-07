package au.com.shiftyjelly.pocketcasts.ui.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefetchArtworkWorkerTest {
    private lateinit var context: Context
    private lateinit var podcastManager: PodcastManager
    private lateinit var imageLoader: ImageLoader
    private lateinit var diskCache: DiskCache

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        podcastManager = mock()
        imageLoader = mock()
        diskCache = mock()
        whenever(imageLoader.diskCache).thenReturn(diskCache)
    }

    @Test
    fun `cached artwork is skipped`() = runTest {
        val urls = setUpPodcast()
        val snapshot = mock<DiskCache.Snapshot>()
        whenever(diskCache.openSnapshot(any())).thenReturn(snapshot)

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(imageLoader, never()).execute(any())
        verify(snapshot, times(urls.size)).close()
    }

    @Test
    fun `selective network failure retries only the missing artwork`() = runTest {
        val urls = setUpPodcast()
        val failedUrl = urls.first()
        val snapshot = mock<DiskCache.Snapshot>()
        val cachedUrls = mutableSetOf<String>()
        var networkRecovered = false
        whenever(diskCache.openSnapshot(any())).thenAnswer { invocation ->
            val url = invocation.getArgument<String>(0)
            snapshot.takeIf { url in cachedUrls }
        }
        whenever(imageLoader.execute(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<ImageRequest>(0)
            val url = request.data.toString()
            if (url == failedUrl && !networkRecovered) {
                errorResult(request)
            } else {
                cachedUrls += url
                mock<ImageResult>()
            }
        }

        val firstResult = buildWorker().doWork()

        assertTrue(firstResult is ListenableWorker.Result.Retry)
        val requestCaptor = argumentCaptor<ImageRequest>()
        verify(imageLoader, times(urls.size)).execute(requestCaptor.capture())
        assertEquals(urls, requestCaptor.allValues.map { it.data.toString() })
        assertTrue(requestCaptor.allValues.all { it.memoryCachePolicy == CachePolicy.DISABLED })

        networkRecovered = true
        clearInvocations(imageLoader)

        val retryResult = buildWorker(runAttemptCount = 1).doWork()

        assertTrue(retryResult is ListenableWorker.Result.Success)
        verify(imageLoader).execute(requestCaptor.capture())
        assertEquals(failedUrl, requestCaptor.lastValue.data.toString())
    }

    @Test
    fun `successful network result retries when disk write failed`() = runTest {
        val urls = setUpPodcast()
        whenever(diskCache.openSnapshot(any())).thenReturn(null)
        whenever(imageLoader.execute(any())).thenReturn(mock())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(imageLoader, times(urls.size)).execute(any())
        verify(diskCache, times(urls.size * 2)).openSnapshot(any())
    }

    @Test
    fun `permanent failures stop retrying after the limit`() = runTest {
        val urls = setUpPodcast()
        whenever(diskCache.openSnapshot(any())).thenReturn(null)
        whenever(imageLoader.execute(any())).thenAnswer { invocation ->
            errorResult(invocation.getArgument(0))
        }

        val retryResult = buildWorker(runAttemptCount = 4).doWork()
        val finalResult = buildWorker(runAttemptCount = 5).doWork()

        assertTrue(retryResult is ListenableWorker.Result.Retry)
        assertTrue(finalResult is ListenableWorker.Result.Success)
        verify(imageLoader, times(urls.size * 2)).execute(any())
    }

    @Test
    fun `unexpected failure is retried`() = runTest {
        whenever(podcastManager.findSubscribedNoOrder()).doThrow(IOException("Database unavailable"))

        val result = buildWorker(runAttemptCount = 4).doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun `unexpected failure stops retrying after the limit`() = runTest {
        whenever(podcastManager.findSubscribedNoOrder()).doThrow(IOException("Database unavailable"))

        val result = buildWorker(runAttemptCount = 5).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `cancellation is propagated`() = runTest {
        whenever(podcastManager.findSubscribedNoOrder()).doThrow(CancellationException("Worker stopped"))

        var cancellation: CancellationException? = null
        try {
            buildWorker().doWork()
        } catch (exception: CancellationException) {
            cancellation = exception
        }

        assertEquals("Worker stopped", cancellation?.message)
    }

    private suspend fun setUpPodcast(): List<String> {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        return PodcastImage.getArtworkUrls(uuid = podcast.uuid, isWearOS = false)
    }

    private fun errorResult(request: ImageRequest): ErrorResult {
        return ErrorResult(
            image = null,
            request = request,
            throwable = IOException("Artwork CDN unavailable"),
        )
    }

    private fun buildWorker(runAttemptCount: Int = 0): PrefetchArtworkWorker {
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker {
                return PrefetchArtworkWorker(
                    context = appContext,
                    params = workerParameters,
                    podcastManager = podcastManager,
                    imageLoader = imageLoader,
                )
            }
        }
        return TestListenableWorkerBuilder<PrefetchArtworkWorker>(
            context = context,
            runAttemptCount = runAttemptCount,
        ).setWorkerFactory(workerFactory).build()
    }
}
