package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import app.cash.turbine.test
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.File
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AlbumArtCacheTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val cacheDir = File(context.cacheDir, "album-art-cache-test")
    private val diskCache = DiskCache.Builder().directory(cacheDir.toOkioPath()).build()
    private val imageLoader = ImageLoader.Builder(context).diskCache(diskCache).build()
    private val albumArtCache = AlbumArtCache(imageLoader, TestScope())

    private val request = AlbumArtContentProvider.artworkFileRequest(context, "https://example.com/episode.jpg")
    private val retryingLoader = mock<ImageLoader>()

    @After
    fun tearDown() {
        imageLoader.shutdown()
        cacheDir.deleteRecursively()
    }

    @Test
    fun `reports artwork the provider has downloaded`() {
        cacheArtwork("https://example.com/episode.jpg")

        assertTrue(albumArtCache.isDownloaded("https://example.com/episode.jpg"))
        assertFalse(albumArtCache.isDownloaded("https://example.com/other.jpg"))
    }

    @Test
    fun `matches URLs that the provider rewrites when it requests them`() {
        val url = "https://EXAMPLE.com/episode artwork.jpg"
        cacheArtwork(url)

        assertTrue(albumArtCache.isDownloaded(url))
    }

    @Test
    fun `doesn't report local files or invalid URLs as downloaded`() {
        assertFalse(albumArtCache.isDownloaded(File(context.cacheDir, "artwork.jpg").path))
        assertFalse(albumArtCache.isDownloaded(""))
    }

    @Test
    fun `reports nothing as downloaded without a disk cache`() {
        val withoutDiskCache = AlbumArtCache(ImageLoader.Builder(context).diskCache(null).build(), TestScope())

        assertFalse(withoutDiskCache.isDownloaded("https://example.com/episode.jpg"))
    }

    @Test
    fun `downloads an image again after a network error and reports when it arrives`() = runTest {
        whenever(retryingLoader.execute(any())).thenReturn(networkError(), success())
        val cache = AlbumArtCache(retryingLoader, this)

        cache.lateDownloads.test {
            cache.retryFailedDownload(networkError())

            awaitItem()
        }
        verify(retryingLoader, times(2)).execute(request)
    }

    @Test
    fun `gives up after the last retry`() = runTest {
        whenever(retryingLoader.execute(any())).thenReturn(networkError())
        val cache = AlbumArtCache(retryingLoader, this)

        cache.lateDownloads.test {
            cache.retryFailedDownload(networkError())
            advanceUntilIdle()

            expectNoEvents()
        }
        verify(retryingLoader, times(AlbumArtCache.DOWNLOAD_RETRY_DELAYS.size)).execute(request)
    }

    @Test
    fun `doesn't retry errors other than network errors`() = runTest {
        // Coil reports an error status from the image host as an HttpException, which isn't an IOException.
        val hostError = ErrorResult(image = null, request = request, throwable = IllegalStateException("HTTP 404"))
        whenever(retryingLoader.execute(any())).thenReturn(hostError)
        val cache = AlbumArtCache(retryingLoader, this)

        cache.retryFailedDownload(hostError)
        advanceUntilIdle()
        verify(retryingLoader, never()).execute(any())

        cache.retryFailedDownload(networkError())
        advanceUntilIdle()
        // The first retry failed on the host's error, so there's no second one.
        verify(retryingLoader, times(1)).execute(request)
    }

    @Test
    fun `doesn't start a second retry for an image already being retried`() = runTest {
        whenever(retryingLoader.execute(any())).thenReturn(success())
        val cache = AlbumArtCache(retryingLoader, this)

        cache.retryFailedDownload(networkError())
        cache.retryFailedDownload(networkError())
        advanceTimeBy(AlbumArtCache.DOWNLOAD_RETRY_DELAYS.first())
        runCurrent()
        verify(retryingLoader, times(1)).execute(request)

        // Once the retries are over, a later failure is retried again.
        cache.retryFailedDownload(networkError())
        advanceUntilIdle()
        verify(retryingLoader, times(2)).execute(request)
    }

    @Test
    fun `emits once for late downloads that arrive close together`() = runTest {
        albumArtCache.lateDownloads.test {
            albumArtCache.onLateDownload()
            advanceTimeBy(500.milliseconds)
            albumArtCache.onLateDownload()
            albumArtCache.onLateDownload()
            expectNoEvents()

            awaitItem()
            advanceTimeBy(AlbumArtCache.LATE_DOWNLOAD_GROUPING * 2)
            expectNoEvents()

            albumArtCache.onLateDownload()
            awaitItem()
        }
    }

    private fun networkError() = ErrorResult(image = null, request = request, throwable = SocketTimeoutException())

    private fun success() = SuccessResult(image = mock(), request = request)

    private fun cacheArtwork(url: String) {
        // Cached under the key of the provider's own request, so the check is tested against what the provider stores.
        val request = AlbumArtContentProvider.artworkFileRequest(context, url.toHttpUrl().toString())
        val editor = requireNotNull(diskCache.openEditor(requireNotNull(request.diskCacheKey)))
        diskCache.fileSystem.write(editor.data) { writeUtf8("artwork") }
        editor.commit()
    }
}
