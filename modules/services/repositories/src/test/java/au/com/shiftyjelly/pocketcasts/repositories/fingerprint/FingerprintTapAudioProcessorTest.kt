package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class FingerprintTapAudioProcessorTest {

    private val tap = mock<FingerprintPcmTap>()
    private val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)

    private fun processor(isEnabled: Boolean) = FingerprintTapAudioProcessor(tap, isEnabled = { isEnabled }).apply {
        configure(format)
        flush(AudioProcessor.StreamMetadata.DEFAULT)
    }

    @Test
    fun `stays inactive when disabled`() {
        val processor = FingerprintTapAudioProcessor(tap, isEnabled = { false })
        assertSame(AudioProcessor.AudioFormat.NOT_SET, processor.configure(format))
        assertFalse(processor.isActive)
    }

    @Test
    fun `keeps the input format when enabled`() {
        val processor = FingerprintTapAudioProcessor(tap, isEnabled = { true })
        assertSame(format, processor.configure(format))
        assertTrue(processor.isActive)
    }

    @Test
    fun `passes input through unchanged`() {
        val processor = processor(isEnabled = true)
        val bytes = ByteArray(16) { it.toByte() }
        val input = ByteBuffer.wrap(bytes.copyOf())
        processor.queueInput(input)
        assertEquals(0, input.remaining())
        val output = processor.output
        val copied = ByteArray(output.remaining())
        output.get(copied)
        assertArrayEquals(bytes, copied)
    }

    @Test
    fun `copies the pcm into the tap as a read-only view`() {
        val processor = processor(isEnabled = true)
        val bytes = ByteArray(16) { it.toByte() }
        processor.queueInput(ByteBuffer.wrap(bytes.copyOf()))
        val captor = argumentCaptor<ByteBuffer>()
        verify(tap).onPcm(captor.capture(), eq(format))
        val seen = captor.firstValue
        assertTrue(seen.isReadOnly)
        val copied = ByteArray(seen.remaining())
        seen.get(copied)
        assertArrayEquals(bytes, copied)
    }

    @Test
    fun `ignores empty input`() {
        val processor = processor(isEnabled = true)
        processor.queueInput(ByteBuffer.allocate(0))
        verifyNoInteractions(tap)
        assertEquals(0, processor.output.remaining())
    }
}
