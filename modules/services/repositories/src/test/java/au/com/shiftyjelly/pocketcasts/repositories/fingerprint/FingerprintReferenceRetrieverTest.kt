package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.content.Context
import java.io.File
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class FingerprintReferenceRetrieverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var retriever: FingerprintReferenceRetriever

    @Before
    fun setUp() {
        val context = mock(Context::class.java)
        whenever(context.cacheDir).thenReturn(tempFolder.root)
        retriever = FingerprintReferenceRetriever(OkHttpClient(), context)
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
