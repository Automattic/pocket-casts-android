package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class StreamingMediaDataSourceTest {

    private val uri = Uri.parse("https://example.com/audio.mp3")
    private val data = ByteArray(200) { it.toByte() }
    private val factory = FakeDataSourceFactory(data)
    private val buffer = ByteArray(64)

    @Test
    fun `getSize returns the length reported when opening at zero`() {
        val source = StreamingMediaDataSource(factory, uri)

        assertEquals(200L, source.size)
    }

    @Test
    fun `sequential reads reuse the open source`() {
        val source = StreamingMediaDataSource(factory, uri)

        assertEquals(10, source.readAt(0, buffer, 0, 10))
        assertEquals(10, source.readAt(10, buffer, 0, 10))

        assertEquals(1, factory.createdSources.size)
        assertEquals(10.toByte(), buffer[0])
    }

    @Test
    fun `readAt reopens when the requested position does not follow the last read`() {
        val source = StreamingMediaDataSource(factory, uri)

        assertEquals(10, source.readAt(0, buffer, 0, 10))
        assertEquals(10, source.readAt(50, buffer, 0, 10))

        assertEquals(2, factory.createdSources.size)
        assertEquals(50L, factory.createdSources[1].openedAt)
        assertEquals(50.toByte(), buffer[0])
    }

    @Test
    fun `readAt returns end of stream when reading past the end`() {
        val source = StreamingMediaDataSource(factory, uri)

        assertEquals(-1, source.readAt(200, buffer, 0, 10))
    }

    @Test
    fun `follow loop waits until the cache covers the read then reads`() {
        val calls = AtomicInteger()
        val source = StreamingMediaDataSource(
            dataSourceFactory = factory,
            uri = uri,
            cachedLengthAt = { _, _ -> if (calls.incrementAndGet() < 3) 0L else 100L },
        )

        assertEquals(10, source.readAt(0, buffer, 0, 10))
        assertTrue(calls.get() >= 3)
    }

    @Test
    fun `readAt returns end of stream when the caller is no longer active`() {
        val source = StreamingMediaDataSource(factory, uri, isActive = { false })

        assertEquals(-1, source.readAt(0, buffer, 0, 10))
        assertTrue(factory.createdSources.isEmpty())
    }

    @Test
    fun `readAt returns end of stream when the caller becomes inactive between reads`() {
        var active = true
        val source = StreamingMediaDataSource(factory, uri, isActive = { active })

        assertEquals(10, source.readAt(0, buffer, 0, 10))
        active = false
        assertEquals(-1, source.readAt(10, buffer, 0, 10))
    }

    private class FakeDataSourceFactory(private val data: ByteArray) : DataSource.Factory {
        val createdSources = mutableListOf<FakeDataSource>()

        override fun createDataSource(): DataSource = FakeDataSource(data).also { createdSources += it }
    }

    private class FakeDataSource(private val data: ByteArray) : DataSource {
        var openedAt = -1L
        private var readPosition = 0

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            openedAt = dataSpec.position
            readPosition = dataSpec.position.toInt()
            return data.size - dataSpec.position
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (readPosition >= data.size) return C.RESULT_END_OF_INPUT
            val toRead = minOf(length, data.size - readPosition)
            System.arraycopy(data, readPosition, buffer, offset, toRead)
            readPosition += toRead
            return toRead
        }

        override fun getUri(): Uri? = null

        override fun close() = Unit
    }
}
