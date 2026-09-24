package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/** Passthrough processor that copies the player's decoded PCM into [FingerprintPcmTap]. */
@OptIn(UnstableApi::class)
class FingerprintTapAudioProcessor(
    private val tap: FingerprintPcmTap,
    private val isEnabled: () -> Boolean,
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (isEnabled()) inputAudioFormat else AudioProcessor.AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        tap.onPcm(inputBuffer.asReadOnlyBuffer(), inputAudioFormat)
        replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }
}
