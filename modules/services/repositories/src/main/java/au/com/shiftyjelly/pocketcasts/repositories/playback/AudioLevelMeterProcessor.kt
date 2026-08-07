package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
class AudioLevelMeterProcessor(
    private val isEnabled: () -> Boolean,
) : BaseAudioProcessor() {

    @Volatile
    var currentAudioLevel: Float = 0f
        private set

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (isEnabled() && inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        measure(inputBuffer)
        replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        currentAudioLevel = 0f
    }

    override fun onReset() {
        currentAudioLevel = 0f
    }

    private fun measure(buffer: ByteBuffer) {
        val samples = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val channelCount = inputAudioFormat.channelCount
        val frameCount = samples.remaining() / channelCount
        if (frameCount == 0) return
        var sumOfSquares = 0.0
        for (frame in 0 until frameCount) {
            val sample = samples.get(frame * channelCount) / 32768.0
            sumOfSquares += sample * sample
        }
        val level = min(sqrt(sumOfSquares / frameCount) * LEVEL_GAIN, 1.0).toFloat()
        currentAudioLevel = LEVEL_SMOOTHING * currentAudioLevel + (1f - LEVEL_SMOOTHING) * level
    }

    companion object {
        private const val LEVEL_GAIN = 3.0
        private const val LEVEL_SMOOTHING = 0.3f
    }
}
