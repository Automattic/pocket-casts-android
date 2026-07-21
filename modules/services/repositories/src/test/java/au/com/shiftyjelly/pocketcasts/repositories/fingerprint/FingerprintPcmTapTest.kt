package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import app.cash.turbine.test
import java.nio.ByteBuffer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
            assertArrayEquals(bytes, chunk.data)
            assertEquals(C.ENCODING_PCM_16BIT, chunk.encoding)
            assertEquals(SAMPLE_RATE, chunk.sampleRate)
            assertEquals(1, chunk.channelCount)
            assertEquals(0, buffer.position())
        }
    }

    companion object {
        private const val SAMPLE_RATE = 1000
    }
}
