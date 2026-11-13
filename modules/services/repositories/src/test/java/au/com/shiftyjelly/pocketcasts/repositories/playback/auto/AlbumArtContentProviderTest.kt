package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import coil3.request.CachePolicy
import coil3.size.Size
import java.io.File
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AlbumArtContentProviderTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val provider = Robolectric.buildContentProvider(AlbumArtContentProvider::class.java).create().get()

    @Test
    fun `serves artwork file stored inside the cache dir`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val artwork = File(context.cacheDir, "artwork.jpg").apply { writeBytes(bytes) }
        val uri = artwork.absolutePath.toUri().asAlbumArtContentUri(context)

        val descriptor = provider.openFile(uri, "r")

        assertNotNull(descriptor)
        descriptor?.use {
            ParcelFileDescriptor.AutoCloseInputStream(it).use { input ->
                assertArrayEquals(bytes, input.readBytes())
            }
        }
    }

    @Test
    fun `serves artwork file stored inside the files dir`() {
        val artwork = File(context.filesDir, "artwork.jpg").apply { writeBytes(byteArrayOf(9)) }
        val uri = artwork.absolutePath.toUri().asAlbumArtContentUri(context)

        assertNotNull(provider.openFile(uri, "r"))
    }

    @Test
    fun `refuses files outside the app's private storage`() {
        // A sibling of the cache/files dirs, e.g. where databases and shared_prefs live.
        val secret = File(context.filesDir.parentFile, "secret.txt").apply { writeBytes(byteArrayOf(0)) }
        val uri = secret.absolutePath.toUri().asAlbumArtContentUri(context)

        assertNull(provider.openFile(uri, "r"))
    }

    @Test
    fun `refuses path traversal that escapes the cache dir`() {
        // Target really exists, so a null result proves the containment check rejected it
        // rather than the file simply being missing.
        val secret = File(context.cacheDir.parentFile, "secret.db").apply { writeBytes(byteArrayOf(0)) }
        val traversalPath = "${context.cacheDir.absolutePath}/../${secret.name}"
        val uri = traversalPath.toUri().asAlbumArtContentUri(context)

        assertNull(provider.openFile(uri, "r"))
    }

    @Test
    fun `serves the fallback when the artwork is missing`() {
        val missing = File(context.cacheDir, "missing-episode-artwork.jpg")
        val fallbackBytes = byteArrayOf(7, 7, 7)
        val fallback = File(context.cacheDir, "podcast-artwork.jpg").apply { writeBytes(fallbackBytes) }
        val uri = missing.absolutePath.toUri().asAlbumArtContentUri(context, fallback = fallback.absolutePath.toUri())

        val descriptor = provider.openFile(uri, "r")

        assertNotNull(descriptor)
        descriptor?.use {
            ParcelFileDescriptor.AutoCloseInputStream(it).use { input ->
                assertArrayEquals(fallbackBytes, input.readBytes())
            }
        }
    }

    @Test
    fun `prefers the artwork over the fallback when both exist`() {
        val artworkBytes = byteArrayOf(1)
        val artwork = File(context.cacheDir, "episode-artwork.jpg").apply { writeBytes(artworkBytes) }
        val fallback = File(context.cacheDir, "podcast-artwork.jpg").apply { writeBytes(byteArrayOf(2)) }
        val uri = artwork.absolutePath.toUri().asAlbumArtContentUri(context, fallback = fallback.absolutePath.toUri())

        val descriptor = provider.openFile(uri, "r")

        assertNotNull(descriptor)
        descriptor?.use {
            ParcelFileDescriptor.AutoCloseInputStream(it).use { input ->
                assertArrayEquals(artworkBytes, input.readBytes())
            }
        }
    }

    @Test
    fun `refuses a fallback outside the app's private storage`() {
        val missing = File(context.cacheDir, "missing-episode-artwork.jpg")
        val secret = File(context.filesDir.parentFile, "secret.txt").apply { writeBytes(byteArrayOf(0)) }
        val uri = missing.absolutePath.toUri().asAlbumArtContentUri(context, fallback = secret.absolutePath.toUri())

        assertNull(provider.openFile(uri, "r"))
    }

    @Test
    fun `leaves out a fallback that matches the artwork`() {
        val artwork = File(context.cacheDir, "artwork.jpg").absolutePath.toUri()

        val uri = artwork.asAlbumArtContentUri(context, fallback = artwork)

        assertNull(uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM))
    }

    @Test
    fun `downloads artwork without a full size decode or memory cache entry`() {
        val request = AlbumArtContentProvider.artworkFileRequest(context, "https://example.com/episode.jpg")

        val size = runBlocking { request.sizeResolver.size() }
        val decodeSize = AlbumArtContentProvider.ARTWORK_DECODE_SIZE_PX
        assertEquals(Size(decodeSize, decodeSize), size)
        assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
    }

    @Test
    fun `gives up on the artwork sooner when there is a fallback`() {
        val lookups = mutableListOf<Pair<String, Duration>>()
        val fallbackFile = File(context.cacheDir, "podcast-artwork.jpg")

        val file = AlbumArtContentProvider.findArtworkOrFallback(
            artwork = "episode-artwork",
            fallback = "podcast-artwork",
            findFile = { urlOrPath, timeout ->
                lookups += urlOrPath to timeout
                fallbackFile.takeIf { urlOrPath == "podcast-artwork" }
            },
        )

        assertEquals(fallbackFile, file)
        val expectedLookups = listOf(
            "episode-artwork" to AlbumArtContentProvider.ARTWORK_WITH_FALLBACK_TIMEOUT,
            "podcast-artwork" to AlbumArtContentProvider.ARTWORK_TIMEOUT,
        )
        assertEquals(expectedLookups, lookups)
    }

    @Test
    fun `gives artwork without a fallback the full time limit`() {
        val lookups = mutableListOf<Pair<String, Duration>>()

        AlbumArtContentProvider.findArtworkOrFallback(
            artwork = "podcast-artwork",
            fallback = null,
            findFile = { urlOrPath, timeout ->
                lookups += urlOrPath to timeout
                null
            },
        )

        assertEquals(listOf("podcast-artwork" to AlbumArtContentProvider.ARTWORK_TIMEOUT), lookups)
    }

    @Test
    fun `tries the fallback when the artwork lookup throws`() {
        val fallbackFile = File(context.cacheDir, "podcast-artwork.jpg")

        val file = AlbumArtContentProvider.findArtworkOrFallback(
            artwork = "episode-artwork",
            fallback = "podcast-artwork",
            findFile = { urlOrPath, _ ->
                if (urlOrPath == "episode-artwork") throw IOException("Disk cache unavailable")
                fallbackFile
            },
        )

        assertEquals(fallbackFile, file)
    }

    @Test
    fun `returns artwork that loads within the time limit`() = runTest {
        val artwork = AlbumArtContentProvider.awaitWithTimeout(backgroundScope, timeout = 5.seconds) {
            delay(4.seconds)
            "artwork"
        }

        assertEquals("artwork", artwork)
    }

    @Test
    fun `stops waiting at the time limit but keeps loading the artwork`() = runTest {
        var loaded = false

        val artwork = AlbumArtContentProvider.awaitWithTimeout(backgroundScope, timeout = 5.seconds) {
            delay(6.seconds)
            loaded = true
            "artwork"
        }

        assertNull(artwork)
        assertEquals(5.seconds.inWholeMilliseconds, currentTime)
        assertFalse(loaded)
        advanceTimeBy(2.seconds)
        assertTrue(loaded)
    }

    @Test
    fun `refuses a missing file inside the cache dir`() {
        val missing = File(context.cacheDir, "does-not-exist.jpg")
        val uri = missing.absolutePath.toUri().asAlbumArtContentUri(context)

        assertNull(provider.openFile(uri, "r"))
    }
}
