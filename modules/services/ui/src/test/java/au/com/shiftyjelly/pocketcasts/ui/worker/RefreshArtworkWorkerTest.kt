package au.com.shiftyjelly.pocketcasts.ui.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.colors.ColorManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.size.Size
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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RefreshArtworkWorkerTest {
    private lateinit var context: Context
    private lateinit var settings: Settings
    private lateinit var podcastManager: PodcastManager
    private lateinit var colorManager: ColorManager
    private lateinit var imageLoader: ImageLoader
    private lateinit var coilManager: CoilManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        settings = mock()
        podcastManager = mock()
        colorManager = mock()
        imageLoader = mock()
        coilManager = CoilManager(imageLoader)
    }

    @Test
    fun `refresh caches every podcast artwork size without a full decode`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any())).thenReturn(mock<ImageResult>())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val requestCaptor = argumentCaptor<ImageRequest>()
        verify(imageLoader, times(3)).execute(requestCaptor.capture())
        val requests = requestCaptor.allValues
        assertEquals(
            PodcastImage.getArtworkUrls(uuid = podcast.uuid, isWearOS = false),
            requests.map { it.data.toString() },
        )
        assertTrue(requests.all { it.memoryCachePolicy == CachePolicy.DISABLED })
        assertTrue(requests.all { it.diskCachePolicy == CachePolicy.ENABLED })
        for (request in requests) {
            assertEquals(Size(1, 1), request.sizeResolver.size())
        }
        verify(colorManager).updateColors(listOf(podcast))
    }

    @Test
    fun `artwork failure does not prevent remaining sizes from refreshing`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any()))
            .thenThrow(IllegalStateException("Artwork loader unavailable"))
            .thenReturn(mock<ImageResult>(), mock<ImageResult>())

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        verify(imageLoader, times(3)).execute(any())
    }

    @Test
    fun `first attempt clears the cache before loading podcasts`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any())).thenReturn(mock<ImageResult>())
        coilManager = mock()
        whenever(coilManager.imageLoader).thenReturn(imageLoader)

        val result = buildWorker(runAttemptCount = 0).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        inOrder(coilManager, podcastManager) {
            verify(coilManager).clearAll()
            verify(podcastManager).findSubscribedNoOrder()
        }
    }

    @Test
    fun `retry preserves artwork restored by the previous attempt`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any())).thenReturn(mock<ImageResult>())
        coilManager = mock()
        whenever(coilManager.imageLoader).thenReturn(imageLoader)

        val result = buildWorker(runAttemptCount = 1).doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        verify(coilManager, never()).clearAll()
    }

    @Test
    fun `persistent artwork failure stops retrying at the limit`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any())).thenThrow(IllegalStateException("Artwork loader unavailable"))

        val retryResult = buildWorker(runAttemptCount = 4).doWork()
        val finalResult = buildWorker(runAttemptCount = 5).doWork()

        assertTrue(retryResult is ListenableWorker.Result.Retry)
        assertTrue(finalResult is ListenableWorker.Result.Failure)
        verify(imageLoader, times(6)).execute(any())
    }

    @Test
    fun `cancellation is propagated`() = runTest {
        val podcast = Podcast(uuid = "podcast-uuid")
        whenever(podcastManager.findSubscribedNoOrder()).doReturn(listOf(podcast))
        whenever(imageLoader.execute(any())).doThrow(CancellationException("Worker stopped"))

        var cancellation: CancellationException? = null
        try {
            buildWorker().doWork()
        } catch (exception: CancellationException) {
            cancellation = exception
        }

        assertEquals("Worker stopped", cancellation?.message)
    }

    private fun buildWorker(runAttemptCount: Int = 0): RefreshArtworkWorker {
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker {
                return RefreshArtworkWorker(
                    context = appContext,
                    params = workerParameters,
                    settings = settings,
                    podcastManager = podcastManager,
                    colorManager = colorManager,
                    coilManager = coilManager,
                )
            }
        }
        return TestListenableWorkerBuilder<RefreshArtworkWorker>(
            context = context,
            runAttemptCount = runAttemptCount,
        )
            .setWorkerFactory(workerFactory)
            .build()
    }
}
