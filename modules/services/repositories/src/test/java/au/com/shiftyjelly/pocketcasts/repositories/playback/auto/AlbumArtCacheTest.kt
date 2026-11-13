package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import app.cash.turbine.test
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.disk.DiskCache
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
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
    private val albumArtCache = AlbumArtCache(imageLoader)

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
        val withoutDiskCache = AlbumArtCache(ImageLoader.Builder(context).diskCache(null).build())

        assertFalse(withoutDiskCache.isDownloaded("https://example.com/episode.jpg"))
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

    private fun cacheArtwork(url: String) {
        // Cached under the key of the provider's own request, so the check is tested against what the provider stores.
        val request = AlbumArtContentProvider.artworkFileRequest(context, url.toHttpUrl().toString())
        val editor = requireNotNull(diskCache.openEditor(requireNotNull(request.diskCacheKey)))
        diskCache.fileSystem.write(editor.data) { writeUtf8("artwork") }
        editor.commit()
    }
}
