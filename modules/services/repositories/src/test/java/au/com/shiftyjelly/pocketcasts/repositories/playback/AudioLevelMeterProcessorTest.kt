package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioLevelMeterProcessorTest {

    private val monoFormat = AudioProcessor.AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
    private val stereoFormat = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)

    private fun processor(format: AudioProcessor.AudioFormat = monoFormat) = AudioLevelMeterProcessor(isEnabled = { true }).apply {
        configure(format)
        flush(AudioProcessor.StreamMetadata.DEFAULT)
    }

    private fun pcmBuffer(vararg samples: Short): ByteBuffer {
        val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach(buffer::putShort)
        buffer.flip()
        return buffer
    }

    @Test
    fun `stays inactive when disabled`() {
        val processor = AudioLevelMeterProcessor(isEnabled = { false })
        assertSame(AudioProcessor.AudioFormat.NOT_SET, processor.configure(monoFormat))
        assertFalse(processor.isActive)
    }

    @Test
    fun `stays inactive for non 16-bit input`() {
        val processor = AudioLevelMeterProcessor(isEnabled = { true })
        val floatFormat = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT)
        assertSame(AudioProcessor.AudioFormat.NOT_SET, processor.configure(floatFormat))
        assertFalse(processor.isActive)
    }

    @Test
    fun `keeps the input format for 16-bit input`() {
        val processor = AudioLevelMeterProcessor(isEnabled = { true })
        assertSame(monoFormat, processor.configure(monoFormat))
        assertTrue(processor.isActive)
    }

    @Test
    fun `passes input through unchanged`() {
        val processor = processor()
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
    fun `ignores empty input`() {
        val processor = processor()
        processor.queueInput(ByteBuffer.allocate(0))
        assertEquals(0, processor.output.remaining())
        assertEquals(0f, processor.currentAudioLevel, 0f)
    }

    @Test
    fun `reports zero level for silence`() {
        val processor = processor()
        processor.queueInput(pcmBuffer(0, 0, 0, 0))
        assertEquals(0f, processor.currentAudioLevel, 0f)
    }

    @Test
    fun `measures the gained rms of a constant signal`() {
        val processor = processor()
        processor.queueInput(pcmBuffer(8192, 8192, 8192, 8192))
        assertEquals(0.7f * 0.75f, processor.currentAudioLevel, 1e-4f)
    }

    @Test
    fun `clamps loud signals to full level`() {
        val processor = processor()
        processor.queueInput(pcmBuffer(16384, 16384, 16384, 16384))
        assertEquals(0.7f, processor.currentAudioLevel, 1e-4f)
    }

    @Test
    fun `measures only the first channel`() {
        val loudLeft = processor(stereoFormat)
        loudLeft.queueInput(pcmBuffer(8192, 0, 8192, 0))
        assertEquals(0.7f * 0.75f, loudLeft.currentAudioLevel, 1e-4f)

        val loudRight = processor(stereoFormat)
        loudRight.queueInput(pcmBuffer(0, 8192, 0, 8192))
        assertEquals(0f, loudRight.currentAudioLevel, 0f)
    }

    @Test
    fun `smooths the level across buffers`() {
        val processor = processor()
        processor.queueInput(pcmBuffer(8192, 8192))
        processor.output
        processor.queueInput(pcmBuffer(8192, 8192))
        assertEquals(0.3f * (0.7f * 0.75f) + 0.7f * 0.75f, processor.currentAudioLevel, 1e-4f)
    }

    @Test
    fun `resets the level on flush`() {
        val processor = processor()
        processor.queueInput(pcmBuffer(8192, 8192))
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        assertEquals(0f, processor.currentAudioLevel, 0f)
    }
}
