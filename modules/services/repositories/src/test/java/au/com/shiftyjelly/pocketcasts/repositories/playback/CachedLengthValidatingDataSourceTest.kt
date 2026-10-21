package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@UnstableApi
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class CachedLengthValidatingDataSourceTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var cache: SimpleCache
    private val upstream = FakeUpstream()
    private lateinit var dataSource: CachedLengthValidatingDataSource

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.VALIDATE_CACHED_MEDIA_LENGTH, true)
        cache = SimpleCache(
            temporaryFolder.newFolder(),
            NoOpCacheEvictor(),
            StandaloneDatabaseProvider(RuntimeEnvironment.getApplication()),
        )
        dataSource = CachedLengthValidatingDataSource(upstream, cache)
    }

    @After
    fun tearDown() {
        cache.release()
    }

    @Test
    fun `stores the served length when the cache has none`() {
        upstream.headers = mapOf("Content-Length" to "1000")

        dataSource.open(dataSpec())

        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `passes when the served length matches the cached length`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Range" to "bytes 400-999/1000", "Content-Length" to "600")

        assertEquals(600L, dataSource.open(dataSpec(position = 400)))
        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `throws when a range response serves a shorter build`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Range" to "bytes 400-899/900", "Content-Length" to "500")

        assertThrows(MediaBuildChangedException::class.java) { dataSource.open(dataSpec(position = 400)) }
        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `throws when a range response serves a longer build`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Range" to "bytes 400-999/1200", "Content-Length" to "600")

        assertThrows(MediaBuildChangedException::class.java) { dataSource.open(dataSpec(position = 400)) }
    }

    @Test
    fun `throws when a full response serves a different build`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Length" to "1200")

        assertThrows(MediaBuildChangedException::class.java) { dataSource.open(dataSpec()) }
    }

    @Test
    fun `passes later opens after a build change`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Length" to "1200")
        assertThrows(MediaBuildChangedException::class.java) { dataSource.open(dataSpec()) }
        dataSource.close()

        upstream.headers = mapOf("Content-Range" to "bytes 400-1299/1300", "Content-Length" to "900")

        assertEquals(900L, dataSource.open(dataSpec(position = 400)))
        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `ignores an unknown document size`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Range" to "bytes 400-899/*")

        dataSource.open(dataSpec(position = 400))

        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `does not store a length when the response has none`() {
        upstream.headers = emptyMap()

        dataSource.open(dataSpec())

        assertEquals(C.LENGTH_UNSET.toLong(), cachedLength())
    }

    @Test
    fun `reads header names case insensitively`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("content-range" to "bytes 400-899/900")

        assertThrows(MediaBuildChangedException::class.java) { dataSource.open(dataSpec(position = 400)) }
    }

    @Test
    fun `skips validation without a cache key`() {
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Length" to "1200")

        dataSource.open(DataSpec(Uri.parse(URL)))

        assertEquals(1000L, cachedLength())
    }

    @Test
    fun `skips validation when the flag is disabled`() {
        FeatureFlag.setEnabled(Feature.VALIDATE_CACHED_MEDIA_LENGTH, false)
        storeCachedLength(1000)
        upstream.headers = mapOf("Content-Length" to "1200")

        dataSource.open(dataSpec())

        assertEquals(1000L, cachedLength())
    }

    private fun dataSpec(position: Long = 0) = DataSpec.Builder()
        .setUri(URL)
        .setKey(KEY)
        .setPosition(position)
        .build()

    private fun storeCachedLength(length: Long) {
        val mutations = ContentMetadataMutations()
        ContentMetadataMutations.setContentLength(mutations, length)
        cache.applyContentMetadataMutations(KEY, mutations)
    }

    private fun cachedLength() = ContentMetadata.getContentLength(cache.getContentMetadata(KEY))

    private class FakeUpstream : DataSource {
        var headers: Map<String, String> = emptyMap()

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            return headers.entries.firstOrNull { it.key.equals("Content-Length", ignoreCase = true) }
                ?.value
                ?.toLong()
                ?: C.LENGTH_UNSET.toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int) = C.RESULT_END_OF_INPUT

        override fun getUri(): Uri = Uri.parse(URL)

        override fun getResponseHeaders(): Map<String, List<String>> = headers.mapValues { listOf(it.value) }

        override fun close() = Unit
    }

    private companion object {
        const val URL = "https://example.com/episode.mp3"
        const val KEY = "episode-uuid"
    }
}
