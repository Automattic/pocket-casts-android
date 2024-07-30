package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import au.com.shiftyjelly.pocketcasts.repositories.playback.ShiftyTrimSilenceProcessor.NoiseState.MaybeSilent
import au.com.shiftyjelly.pocketcasts.repositories.playback.ShiftyTrimSilenceProcessor.NoiseState.Noisy
import au.com.shiftyjelly.pocketcasts.repositories.playback.ShiftyTrimSilenceProcessor.NoiseState.Silent
import java.nio.ByteBuffer
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

/**
 * An [audio processor][androidx.media3.common.audio.AudioProcessor] that skips silence in the input stream.
 * Input and output are 16-bit PCM encoded.
 *
 * @param [minimumSilenceDuration] The minimum duration of audio that must be below [silenceThresholdLevel]
 * to classify that part of audio as silent. Supports microseconds resolution.
 *
 * @param [paddingSilenceDuration] The duration of silence by which to extend non-silent sections.
 * The value must not exceed [minimumSilenceDuration]. Supports microseconds resolution.
 *
 * @param [silenceThresholdLevel] The absolute level below which an individual PCM sample is classified as silent.
 */
@OptIn(UnstableApi::class)
class ShiftyTrimSilenceProcessor(
    private val minimumSilenceDuration: Duration,
    private val paddingSilenceDuration: Duration,
    private val silenceThresholdLevel: Short,
    private var onSkippedListener: SkippedListener,
) : BaseAudioProcessor() {
    private var bytesPerFrame: Int = 0

    /**
     * Sets whether to skip silence in the input. May be changed only after draining data
     * through the processor. The value returned by this or [isActive] may change, and the processor
     * must be [flushed][flush] before queueing more data.
     */
    var enabled = false

    /**
     * Buffers audio data that may be classified as silence while in [NoiseState.MaybeSilent]. If
     * the input becomes noisy before the buffer has filled, it will be output. Otherwise, the buffer
     * contents will be dropped and the state will transition to [NoiseState.Silent].
     */
    private var maybeSilenceBuffer = Util.EMPTY_BYTE_ARRAY
    private var maybeSilenceBufferSize = 0

    /**
     * Stores the latest part of the input while silent. It will be output as padding if the next
     * input is noisy.
     */
    private var paddingBuffer = Util.EMPTY_BYTE_ARRAY
    private var paddingBufferSize = 0

    private var state = Noisy
    private var hasOutputNoise = false

    /**
     * The total number of frames of input audio that were skipped due to being classified as
     * silence since the last call to [flush].
     */
    var skippedFrames = 0L
        private set

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        return if (enabled) inputAudioFormat else AudioFormat.NOT_SET
    }

    override fun isActive(): Boolean = super.isActive() && enabled

    override fun queueInput(inputBuffer: ByteBuffer) {
        while (inputBuffer.hasRemaining() && !hasPendingOutput()) {
            when (state) {
                Noisy -> processNoisy(inputBuffer)
                MaybeSilent -> processMaybeSilent(inputBuffer)
                Silent -> processSilent(inputBuffer)
            }
        }
    }

    override fun onQueueEndOfStream() {
        if (maybeSilenceBufferSize > 0) {
            output(maybeSilenceBuffer, maybeSilenceBufferSize)
        }
        if (!hasOutputNoise) {
            skippedFrames += paddingBufferSize / bytesPerFrame
        }
        hasOutputNoise = false
        maybeSilenceBuffer = Util.EMPTY_BYTE_ARRAY
        maybeSilenceBufferSize = 0
        paddingBuffer = Util.EMPTY_BYTE_ARRAY
        paddingBufferSize = 0
    }

    override fun onFlush() {
        if (enabled) {
            onSkippedListener.onSkippedFrames(skippedFrames.frameCountToDuration())

            bytesPerFrame = inputAudioFormat.bytesPerFrame
            val maybeSilenceBufferSize = minimumSilenceDuration.toFrameCount() * bytesPerFrame
            if (maybeSilenceBuffer.size != maybeSilenceBufferSize) {
                maybeSilenceBuffer = ByteArray(maybeSilenceBufferSize)
            }
            paddingBufferSize = paddingSilenceDuration.toFrameCount() * bytesPerFrame
            if (paddingBuffer.size != paddingBufferSize) {
                paddingBuffer = ByteArray(paddingBufferSize)
            }
        }
        state = Noisy
        skippedFrames = 0
        maybeSilenceBufferSize = 0
        hasOutputNoise = false
    }

    override fun onReset() {
        enabled = false
        hasOutputNoise = false
        maybeSilenceBuffer = Util.EMPTY_BYTE_ARRAY
        maybeSilenceBufferSize = 0
        paddingBuffer = Util.EMPTY_BYTE_ARRAY
        paddingBufferSize = 0
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in noisy state, updating the state if needed.
     */
    private fun processNoisy(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()

        // Check if there's any noise within the maybe silence buffer duration
        inputBuffer.limit(min(limit, inputBuffer.position() + maybeSilenceBuffer.size))
        val noiseLimit = findNoiseLimit(inputBuffer)
        if (noiseLimit == inputBuffer.position()) {
            state = MaybeSilent
        } else {
            inputBuffer.limit(noiseLimit)
            output(inputBuffer)
        }

        // Restore the limit
        inputBuffer.limit(limit)
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in maybe silent state, updating the state if needed.
     */
    private fun processMaybeSilent(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val noisePosition = findNoisePosition(inputBuffer)
        val maybeSilenceInputSize = noisePosition - inputBuffer.position()
        val maybeSilenceBufferRemaining = maybeSilenceBuffer.size - maybeSilenceBufferSize
        if (noisePosition < limit && maybeSilenceInputSize < maybeSilenceBufferRemaining) {
            // The maybe silence buffer isn't full, so output it and switch back to the noisy state.
            output(maybeSilenceBuffer, maybeSilenceBufferSize)
            maybeSilenceBufferSize = 0
            state = Noisy
        } else {
            // Fill as much of the maybe silence buffer as possible.
            val bytesToWrite = min(maybeSilenceInputSize, maybeSilenceBufferRemaining)
            inputBuffer.limit(inputBuffer.position() + bytesToWrite)
            inputBuffer[maybeSilenceBuffer, maybeSilenceBufferSize, bytesToWrite]
            maybeSilenceBufferSize += bytesToWrite
            if (maybeSilenceBufferSize == maybeSilenceBuffer.size) {
                // We've reached a period of silence, so skip it, taking in to account padding for both
                // the noisy to silent transition and any future silent to noisy transition.
                if (hasOutputNoise) {
                    output(maybeSilenceBuffer, paddingBufferSize)
                    skippedFrames += (maybeSilenceBufferSize - paddingBufferSize * 2) / bytesPerFrame
                } else {
                    skippedFrames += (maybeSilenceBufferSize - paddingBufferSize) / bytesPerFrame
                }
                updatePaddingBuffer(inputBuffer, maybeSilenceBuffer, maybeSilenceBufferSize)
                maybeSilenceBufferSize = 0
                state = Silent
            }

            // Restore the limit.
            inputBuffer.limit(limit)
        }
    }

    /**
     * Incrementally processes new input from [inputBuffer] while in silent state, updating the state if needed.
     */
    private fun processSilent(inputBuffer: ByteBuffer) {
        val limit = inputBuffer.limit()
        val noisyPosition = findNoisePosition(inputBuffer)
        inputBuffer.limit(noisyPosition)
        skippedFrames += inputBuffer.remaining() / bytesPerFrame
        updatePaddingBuffer(inputBuffer, paddingBuffer, paddingBufferSize)
        if (noisyPosition < limit) {
            // Output the padding, which may include previous input as well as new input, then transition
            // back to the noisy state.
            output(paddingBuffer, paddingBufferSize)
            state = Noisy

            // Restore the limit.
            inputBuffer.limit(limit)
        }
    }

    /**
     * Copies [size] elements from [data] to populate a new output buffer from the processor.
     */
    private fun output(data: ByteArray, size: Int) {
        replaceOutputBuffer(size).put(data, 0, size).flip()
        if (size > 0) {
            hasOutputNoise = true
        }
    }

    /**
     * Copies remaining bytes from [data] to populate a new output buffer from the processor.
     */
    private fun output(data: ByteBuffer) {
        val size = data.remaining()
        replaceOutputBuffer(size).put(data).flip()
        if (size > 0) {
            hasOutputNoise = true
        }
    }

    /**
     * Fills [paddingBuffer] using data from [input], plus any additional buffered data
     * at the end of [buffer] (up to its [size]) required to fill it, advancing the input
     * position.
     */
    private fun updatePaddingBuffer(input: ByteBuffer, buffer: ByteArray, size: Int) {
        val fromInputSize = min(input.remaining(), paddingBufferSize)
        val fromBufferSize = paddingBufferSize - fromInputSize
        buffer.copyInto(
            destination = paddingBuffer,
            destinationOffset = 0,
            startIndex = size - fromBufferSize,
            endIndex = size,
        )
        input.position(input.limit() - fromInputSize)
        input.get(paddingBuffer, fromBufferSize, fromInputSize)
    }

    /**
     * Returns the number of input frames corresponding to microseconds of audio.
     */
    private fun Duration.toFrameCount() = (inWholeMicroseconds * inputAudioFormat.sampleRate / C.MICROS_PER_SECOND).toInt()

    /**
     * Returns the duration of input frames.
     */
    private fun Long.frameCountToDuration() = (this * C.MICROS_PER_SECOND / inputAudioFormat.sampleRate).microseconds

    /**
     * Returns the earliest byte position in [position, limit) of [buffer] that contains a frame
     * classified as a noisy frame, or the limit of the buffer if no such frame exists.
     */
    private fun findNoisePosition(buffer: ByteBuffer): Int {
        // The input is in ByteOrder.nativeOrder(), which is little endian on Android.
        for (i in buffer.position()..<buffer.limit() step 2) {
            if (buffer.getShort(i).toInt().absoluteValue > silenceThresholdLevel) {
                // Round to the start of the frame.
                return bytesPerFrame * (i / bytesPerFrame)
            }
        }
        return buffer.limit()
    }

    /**
     * Returns the earliest byte position in [position, limit) of [buffer] such that all frames
     * from the byte position to the limit are classified as silent.
     */
    private fun findNoiseLimit(buffer: ByteBuffer): Int {
        // The input is in ByteOrder.nativeOrder(), which is little endian on Android.
        for (i in (buffer.limit() - 2) downTo buffer.position() step 2) {
            if (buffer.getShort(i).toInt().absoluteValue > silenceThresholdLevel) {
                // Return the start of the next frame.
                return bytesPerFrame * (i / bytesPerFrame) + bytesPerFrame
            }
        }
        return buffer.position()
    }

    fun interface SkippedListener {
        fun onSkippedFrames(duration: Duration)
    }

    private enum class NoiseState {
        Noisy,
        MaybeSilent,
        Silent,
    }

    companion object {
        const val DEFAULT_SILENCE_THRESHOLD_LEVEL: Short = 250
    }
}
