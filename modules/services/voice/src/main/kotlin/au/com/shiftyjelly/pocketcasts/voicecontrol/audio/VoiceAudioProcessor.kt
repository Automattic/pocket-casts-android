package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

import android.Manifest
import androidx.annotation.RequiresPermission
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Relays VAD events from the C++ processing thread to downstream consumers.
 *
 * VAD is performed natively by [OboeCaptureEngine] via Silero VAD ONNX inference.
 * The Kotlin coroutine blocks on event signals rather than polling per-frame,
 * allowing the CPU to enter deep idle states during silence.
 */
@Singleton
class VoiceAudioProcessor @Inject constructor(
    private val microphoneCapture: MicrophoneCapture,
) {
    /**
     * Start processing audio from the microphone and emit voice segmenter results.
     *
     * @return Flow of VoiceSegmenterResult objects indicating speech activity
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startProcessing(): Flow<VoiceSegmenterResult> {
        return microphoneCapture.startCapture()
    }

    /**
     * Stop audio processing and release microphone resources.
     */
    fun stopProcessing() {
        microphoneCapture.stopCapture()
        Timber.i("Voice audio processing stopped")
    }

    /**
     * Check if audio processing is currently active.
     */
    val isProcessing: Boolean
        get() = microphoneCapture.isRecording
}
