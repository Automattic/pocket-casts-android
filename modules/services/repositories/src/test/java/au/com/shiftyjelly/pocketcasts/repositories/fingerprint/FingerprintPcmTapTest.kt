package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import app.cash.turbine.test
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintPcmTapTest {

    private val tap = FingerprintPcmTap()
    private val format = AudioProcessor.AudioFormat(SAMPLE_RATE, 1, C.ENCODING_PCM_16BIT)

    private fun pcmBuffer(frames: Int) = ByteBuffer.allocate(frames * format.bytesPerFrame)

    @Test
    fun `emits nothing until the sink provides an anchor`() = runTest {
        tap.chunks.test {
            tap.onPcm(pcmBuffer(frames = 100), format)
            expectNoEvents()
        }
    }

    @Test
    fun `stamps the first chunk with the sink's media time`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(10.0, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `extrapolates positions from frame counts between sink buffers`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            awaitItem()
            tap.onPcm(pcmBuffer(frames = 50), format)
            assertEquals(10.1, awaitItem().positionSec, 1e-6)
            tap.onPcm(pcmBuffer(frames = 50), format)
            assertEquals(10.15, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `keeps the anchor when sink times drift within tolerance`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            awaitItem()
            tap.onSinkBuffer(10_050_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(10.1, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `re-anchors when the sink time jumps past tolerance`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            awaitItem()
            tap.onSinkBuffer(60_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(60.0, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `a consumed sink time cannot re-anchor later buffers`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            awaitItem()
            tap.onPcm(pcmBuffer(frames = 300), format)
            assertEquals(10.1, awaitItem().positionSec, 1e-6)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(10.4, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `sink flush clears the anchor until a new buffer arrives`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(10_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            awaitItem()
            tap.onSinkFlush()
            tap.onPcm(pcmBuffer(frames = 100), format)
            expectNoEvents()
            tap.onSinkBuffer(5_000_000L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(5.0, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `tracks position while unsubscribed without emitting`() = runTest {
        tap.onSinkBuffer(0L)
        tap.onPcm(pcmBuffer(frames = 200), format)
        tap.chunks.test {
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(0.2, awaitItem().positionSec, 1e-6)
        }
    }

    @Test
    fun `copies the pcm payload and format into the chunk`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(1_000_000L)
            val bytes = ByteArray(10) { it.toByte() }
            val buffer = ByteBuffer.wrap(bytes)
            tap.onPcm(buffer, format)
            val chunk = awaitItem()
            assertEquals(bytes.size, chunk.size)
            assertArrayEquals(bytes, chunk.data.copyOf(chunk.size))
            assertEquals(C.ENCODING_PCM_16BIT, chunk.encoding)
            assertEquals(SAMPLE_RATE, chunk.sampleRate)
            assertEquals(1, chunk.channelCount)
            assertEquals(0, buffer.position())
        }
    }

    @Test
    fun `reuses a recycled array for the next chunk`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            val first = awaitItem()
            first.recycle()
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertSame(first.data, awaitItem().data)
        }
    }

    @Test
    fun `does not reuse an array the consumer still holds`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            val first = awaitItem()
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertNotSame(first.data, awaitItem().data)
        }
    }

    @Test
    fun `recycling twice hands the array to only one chunk`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            val first = awaitItem()
            first.recycle()
            first.recycle()
            tap.onPcm(pcmBuffer(frames = 100), format)
            val second = awaitItem()
            tap.onPcm(pcmBuffer(frames = 100), format)
            val third = awaitItem()
            assertSame(first.data, second.data)
            assertNotSame(second.data, third.data)
        }
    }

    @Test
    fun `allocates a larger array when the pooled one is too small`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 10), format)
            val small = awaitItem()
            small.recycle()
            tap.onPcm(pcmBuffer(frames = 100), format)
            val large = awaitItem()
            assertNotSame(small.data, large.data)
            assertEquals(100 * format.bytesPerFrame, large.size)
        }
    }

    @Test
    fun `reuses a larger pooled array for a smaller buffer`() = runTest {
        tap.chunks.test {
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            val large = awaitItem()
            large.recycle()
            val bytes = ByteArray(4) { (it + 1).toByte() }
            tap.onPcm(ByteBuffer.wrap(bytes), format)
            val small = awaitItem()
            assertSame(large.data, small.data)
            assertEquals(bytes.size, small.size)
            assertArrayEquals(bytes, small.data.copyOf(small.size))
        }
    }

    @Test
    fun `keeps the newest chunks in distinct arrays when the consumer lags`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<FingerprintPcmTap.PcmChunk>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            tap.chunks.collect { chunk ->
                received += chunk
                gate.await()
            }
        }
        tap.onSinkBuffer(0L)
        repeat(LAGGING_CHUNK_COUNT) { tap.onPcm(pcmBuffer(frames = 1), format) }
        gate.complete(Unit)
        testScheduler.runCurrent()

        val buffered = received.drop(1)
        assertEquals(CHUNK_BUFFER_CAPACITY, buffered.size)
        assertEquals((LAGGING_CHUNK_COUNT - 1).toDouble() / SAMPLE_RATE, buffered.last().positionSec, 1e-6)
        // Arrays compare by identity, so this fails if two live chunks share one.
        assertEquals(received.size, received.map { it.data }.distinct().size)
    }

    @Test
    fun `a new collector replaces the previous one`() = runTest {
        val first = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { tap.chunks.collect {} }
        tap.chunks.test {
            testScheduler.runCurrent()
            assertTrue(first.isCancelled)
            tap.onSinkBuffer(0L)
            tap.onPcm(pcmBuffer(frames = 100), format)
            assertEquals(0.0, awaitItem().positionSec, 1e-6)
        }
    }

    companion object {
        private const val SAMPLE_RATE = 1000
        private const val CHUNK_BUFFER_CAPACITY = 64
        private const val LAGGING_CHUNK_COUNT = 100
    }
}
