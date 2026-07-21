package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.exoplayer.audio.AudioSink
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintPcmTap
import java.nio.ByteBuffer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FingerprintTapAudioSinkTest {

    private val delegate = mock<AudioSink>()
    private val tap = mock<FingerprintPcmTap>()
    private val sink = FingerprintTapAudioSink(delegate, tap)
    private val buffer = ByteBuffer.allocate(4)

    @Test
    fun `reports the raw presentation time before any stream offset`() {
        sink.handleBuffer(buffer, 5_000_000L, 1)
        verify(tap).onSinkBuffer(5_000_000L)
    }

    @Test
    fun `converts renderer time to media time using the stream offset`() {
        sink.setOutputStreamOffsetUs(2_000_000L)
        sink.handleBuffer(buffer, 5_000_000L, 1)
        verify(tap).onSinkBuffer(3_000_000L)
    }

    @Test
    fun `applies offset updates to subsequent buffers`() {
        sink.setOutputStreamOffsetUs(2_000_000L)
        sink.handleBuffer(buffer, 5_000_000L, 1)
        sink.setOutputStreamOffsetUs(10_000_000L)
        sink.handleBuffer(buffer, 12_000_000L, 1)
        verify(tap).onSinkBuffer(3_000_000L)
        verify(tap).onSinkBuffer(2_000_000L)
    }

    @Test
    fun `forwards buffers and offsets to the wrapped sink`() {
        whenever(delegate.handleBuffer(any(), any(), any())).thenReturn(true)
        sink.setOutputStreamOffsetUs(2_000_000L)
        assertTrue(sink.handleBuffer(buffer, 5_000_000L, 1))
        verify(delegate).setOutputStreamOffsetUs(2_000_000L)
        verify(delegate).handleBuffer(buffer, 5_000_000L, 1)
    }

    @Test
    fun `flush clears the tap anchor`() {
        sink.flush()
        verify(tap).onSinkFlush()
        verify(delegate).flush()
    }

    @Test
    fun `reset clears the tap anchor`() {
        sink.reset()
        verify(tap).onSinkFlush()
        verify(delegate).reset()
    }
}
