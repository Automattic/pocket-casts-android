package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AlbumArtContentProviderTest {

    private val context = RuntimeEnvironment.getApplication<Context>()
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
    fun `refuses a missing file inside the cache dir`() {
        val missing = File(context.cacheDir, "does-not-exist.jpg")
        val uri = missing.absolutePath.toUri().asAlbumArtContentUri(context)

        assertNull(provider.openFile(uri, "r"))
    }
}
