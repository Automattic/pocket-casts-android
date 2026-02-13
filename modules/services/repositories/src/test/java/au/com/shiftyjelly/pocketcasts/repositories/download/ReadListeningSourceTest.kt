package au.com.shiftyjelly.pocketcasts.repositories.download

import kotlin.random.Random
import okio.Buffer
import okio.ByteString
import okio.Source
import okio.blackholeSink
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadListeningSourceTest {
    @Test
    fun `read full content`() {
        var bytesRead = 0L
        val contentLength = 3 * OKIO_SEGMENT_SIZE

        TestSource(contentLength.toInt()).withReadListener { bytesRead = it }.buffer().use { source ->
            source.readAll(blackholeSink())
        }

        assertEquals(contentLength, bytesRead)
    }

    @Test
    fun `read content progressively`() {
        val bytesRead = mutableListOf<Long>()
        val contentLength = 150 + 2 * OKIO_SEGMENT_SIZE

        TestSource(contentLength.toInt()).withReadListener { bytesRead += it }.buffer().use { source ->
            source.readByteString(byteCount = OKIO_SEGMENT_SIZE)
            source.readByteString(byteCount = OKIO_SEGMENT_SIZE)
            source.readByteString()
        }

        assertEquals(
            listOf(
                OKIO_SEGMENT_SIZE,
                OKIO_SEGMENT_SIZE * 2,
                contentLength,
            ),
            bytesRead,
        )
    }
}

private const val OKIO_SEGMENT_SIZE = 8_192L

private class TestSource(
    contentLength: Int,
) : Source {
    private val content = Buffer().write(Random.nextBytes(contentLength))

    override fun read(sink: Buffer, byteCount: Long): Long = content.read(sink, byteCount)

    override fun close() = content.close()

    override fun timeout() = content.timeout()
}
