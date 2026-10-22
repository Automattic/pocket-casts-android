package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintPcmTap
import java.nio.ByteBuffer

/** Reports each buffer's media timestamp to [FingerprintPcmTap] before the processor chain consumes it. */
@OptIn(UnstableApi::class)
internal class FingerprintTapAudioSink(
    sink: AudioSink,
    private val tap: FingerprintPcmTap,
) : ForwardingAudioSink(sink) {

    // Buffer timestamps arrive in renderer time; the renderer reports this offset back to media time.
    private var outputStreamOffsetUs = 0L

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        this.outputStreamOffsetUs = outputStreamOffsetUs
        super.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        tap.onSinkBuffer(presentationTimeUs - outputStreamOffsetUs)
        return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
    }

    override fun flush() {
        tap.onSinkFlush()
        super.flush()
    }

    override fun reset() {
        tap.onSinkFlush()
        super.reset()
    }
}
