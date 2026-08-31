package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * Namespacing object for JNI native function declarations.
 *
 * Loads the pocketcasts_voice_capture native library at class initialization time.
 */
internal object OboeNative {
    init {
        System.loadLibrary("pocketcasts_voice_capture")
        Timber.i("Oboe native library loaded")
    }

    // Atomic capture + VAD start (single mutex scope — prevents race with close).
    // Passes AssetManager so the Silero VAD ONNX session can be initialized.
    external fun nativeStartCaptureAndVad(sampleRate: Int, channels: Int, assetManager: AssetManager): Boolean

    // Atomic capture + VAD stop
    external fun nativeStopCaptureAndVad()

    external fun nativeIsCapturing(): Boolean
    external fun nativeWaitForVadEvent(timeoutMs: Int): Int
    external fun nativeGetSpeechPcm(buffer: ShortArray): Int
    external fun nativeGetSpeechPcmSize(): Int
    external fun nativeGetSpeechOnsetSample(): Int
}

/** Configuration constants shared between Kotlin capture loop and native code. */
internal object OboeConfig {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNELS = 1
    const val FRAMES_PER_POLL = 1024 // 64ms at 16kHz
}

/**
 * Capture engine using Oboe native library via JNI.
 *
 * The Oboe audio callback writes samples into a native lock-free ring buffer.
 * A C++ VAD processing thread consumes that buffer, runs Silero VAD inference,
 * and signals speech events. A Kotlin coroutine blocks on these events and
 * emits [VoiceSegmenterResult] to downstream consumers.
 *
 * All JNI calls are made from the single collector coroutine context.
 */
internal class OboeCaptureEngine(
    private val assetManager: AssetManager,
) {

    @Volatile
    private var disposed = false

    fun startCapture(): Flow<VoiceSegmenterResult> = flow {
        if (!OboeNative.nativeStartCaptureAndVad(OboeConfig.SAMPLE_RATE_HZ, OboeConfig.CHANNELS, assetManager)) {
            throw MicrophoneCaptureException.InitializationFailed("Oboe stream creation failed")
        }

        try {
            while (currentCoroutineContext().isActive && !disposed) {
                when (val event = OboeNative.nativeWaitForVadEvent(500)) {
                    1 -> emit(VoiceSegmenterResult.SpeechStarted)

                    2 -> {
                        val totalSamples = OboeNative.nativeGetSpeechPcmSize()
                        if (totalSamples > 0) {
                            val buffer = ShortArray(totalSamples)
                            val copied = OboeNative.nativeGetSpeechPcm(buffer)
                            if (copied > 0) {
                                val speechOnsetSample = OboeNative.nativeGetSpeechOnsetSample()
                                require(speechOnsetSample in 0 until copied) {
                                    "Native VAD speech onset $speechOnsetSample outside 0 until $copied"
                                }
                                val frames = mutableListOf<PcmAudioFrame>()
                                var offset = 0
                                while (offset < copied) {
                                    val frameSize = minOf(OboeConfig.FRAMES_PER_POLL, copied - offset)
                                    val frameSamples = buffer.copyOfRange(offset, offset + frameSize)
                                    frames.add(PcmAudioFrame(frameSamples, OboeConfig.SAMPLE_RATE_HZ))
                                    offset += frameSize
                                }
                                emit(
                                    VoiceSegmenterResult.SpeechEnded(
                                        frames = frames,
                                        speechOnsetSample = speechOnsetSample,
                                    ),
                                )
                            }
                        }
                    }

                    0 -> { /* timeout */ }

                    -1 -> break
                }
            }
        } finally {
            OboeNative.nativeStopCaptureAndVad()
            disposed = true
        }
    }.flowOn(Dispatchers.IO)

    fun stopCapture() {
        disposed = true
    }

    val isRecording: Boolean
        get() = !disposed && OboeNative.nativeIsCapturing()
}
