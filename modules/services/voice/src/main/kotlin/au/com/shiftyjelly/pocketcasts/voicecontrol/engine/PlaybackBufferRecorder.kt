package au.com.shiftyjelly.pocketcasts.voicecontrol.engine

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rolling buffer of recently-played audio for cross-correlation.
 * Taps into the playback pipeline to maintain a ~2-second window
 * of PCM data that was just sent to the audio output.
 */
@Singleton
class PlaybackBufferRecorder @Inject constructor() {
    private val buffer: FloatArray = FloatArray(SAMPLE_RATE * BUFFER_DURATION_SECONDS)
    private var writePos = 0
    private var filled = false

    fun write(pcm: FloatArray) {
        for (sample in pcm) {
            buffer[writePos] = sample
            writePos = (writePos + 1) % buffer.size
            if (writePos == 0) filled = true
        }
    }

    fun snapshot(): FloatArray {
        if (!filled && writePos == 0) return FloatArray(0)
        val size = if (filled) buffer.size else writePos
        val result = FloatArray(size)
        if (filled) {
            // Wrap: copy from writePos to end, then start to writePos
            val tail = buffer.size - writePos
            buffer.copyInto(result, 0, writePos, buffer.size)
            buffer.copyInto(result, tail, 0, writePos)
        } else {
            buffer.copyInto(result, 0, 0, writePos)
        }
        return result
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val BUFFER_DURATION_SECONDS = 2
    }
}
