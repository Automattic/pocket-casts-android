package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Wake word detector using an openWakeWord Conv-Attention classifier trained via livekit-wakeword.
 *
 * The deployment threshold is loaded from [assets/oww/auris_eval.json] (optimal_threshold),
 * falling back to a hardcoded default if the file is missing or unreadable.
 *
 * Known limitation: the model was trained on synthetic TTS audio augmented with room impulse
 * responses and background noise. Scores on real voice captured through the phone microphone
 * are significantly lower than on synthetic training clips (~0.05 vs ~0.95). Improving
 * real-voice detection requires fine-tuning with phone-recorded wake word examples.
 */
@Singleton
class OpenWakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : WakeWordDetector {

    override val isReady: Boolean
        get() = ready

    @Volatile
    private var ready = false
    private val detectionThreshold: Float

    init {
        detectionThreshold = loadThreshold()
        try {
            val melModel = context.assets.open("oww/melspectrogram.onnx").use { it.readBytes() }
            val embedModel = context.assets.open("oww/embedding_model.onnx").use { it.readBytes() }
            val classifierModel = context.assets.open("oww/auris.onnx").use { it.readBytes() }

            ready = WakeWordJni.nativeInit(melModel, embedModel, classifierModel, detectionThreshold)
            if (ready) {
                Timber.i("OpenWakeWordDetector initialized (threshold=%.3f)", detectionThreshold)
            } else {
                Timber.e("OpenWakeWordDetector failed to initialize native pipeline")
            }
        } catch (e: Exception) {
            Timber.e(e, "OpenWakeWordDetector init failed")
            ready = false
        }
    }

    override suspend fun detect(segment: FloatArray, sampleRateHz: Int): WakeWordResult {
        if (!ready) return WakeWordResult(detected = false)
        if (sampleRateHz != 16000) return WakeWordResult(detected = false)

        return withContext(Dispatchers.IO) {
            try {
                val outOffset = FloatArray(1)
                val score = WakeWordJni.nativeDetect(segment, sampleRateHz, outOffset)
                if (score < 0f) {
                    WakeWordResult(detected = false)
                } else if (score >= detectionThreshold) {
                    Timber.i("Wake word detected (score=%.3f)", score)
                    WakeWordResult(
                        detected = true,
                        confidence = score.coerceAtMost(1f),
                        remainderSamples = extractRemainder(segment, outOffset[0].toInt()),
                    )
                } else {
                    Timber.i("Wake word score: %.3f (threshold=%.3f)", score, detectionThreshold)
                    WakeWordResult(detected = false)
                }
            } catch (e: Exception) {
                Timber.w(e, "Wake word detection failed")
                WakeWordResult(detected = false)
            }
        }
    }

    override fun release() {
        try {
            WakeWordJni.nativeRelease()
            ready = false
        } catch (e: Exception) {
            Timber.w(e, "Failed to release wake word detector")
        }
    }

    private fun loadThreshold(): Float {
        return try {
            val json = context.assets.open("oww/auris_eval.json").use { it.readBytes() }
            val threshold = JSONObject(String(json)).optDouble("optimal_threshold", -1.0)
            if (threshold > 0.0) {
                Timber.i("Loaded wake word threshold from auris_eval.json: %.3f", threshold)
                threshold.toFloat()
            } else {
                Timber.w("auris_eval.json missing optimal_threshold, using default")
                DEFAULT_THRESHOLD
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load auris_eval.json, using default threshold")
            DEFAULT_THRESHOLD
        }
    }

    companion object {
        private const val DEFAULT_THRESHOLD = 0.80f

        private const val SILENCE_THRESHOLD = 0.02f // RMS whisper floor
        private const val MIN_SILENCE_SAMPLES = 3200 // 200ms at 16kHz
        private const val WINDOW_SIZE_SAMPLES = 32000 // 2s at 16kHz
        private const val RMS_WINDOW = 80 // 5ms local RMS window at 16kHz
        private const val MIN_REMAINDER_SAMPLES = 8000 // 500ms minimum remainder

        /**
         * Extract the command remainder after the wake word using silence-gap detection.
         *
         * The natural pause between speaking the wake word and the command is the cut point.
         * This handles any wake word phrase ("Auris", "Hey Auris", custom wake words) at any
         * speaking speed without tunable constants.
         *
         * Algorithm:
         * 1. Scan forward from the end of the detection window (maxOffset + windowSize).
         * 2. Track silence runs using local RMS over a small window.
         * 3. When a silence run reaches minSilenceSamples, cut at its start.
         * 4. Return null if no gap found before 90% of the segment, or remainder < 500ms.
         *
         * @param segment The full VAD segment audio.
         * @param maxOffsetSample Sample offset where the max-score detection window started.
         * @return The command remainder audio, or null to fall back to the full segment.
         */
        internal fun extractRemainder(segment: FloatArray, maxOffsetSample: Int): FloatArray? {
            if (maxOffsetSample < 0) return null

            // Start scanning from the end of the detection window
            var scanFrom = maxOffsetSample + WINDOW_SIZE_SAMPLES
            if (scanFrom >= segment.size * 0.9) scanFrom = segment.size / 2

            var silenceCount = 0
            var gapStart = -1

            var i = scanFrom
            while (i < segment.size - RMS_WINDOW) {
                // Compute local RMS over small window
                var sumSq = 0f
                for (j in 0 until RMS_WINDOW) {
                    val s = segment[i + j]
                    sumSq += s * s
                }
                val rms = kotlin.math.sqrt(sumSq / RMS_WINDOW)

                if (rms < SILENCE_THRESHOLD) {
                    if (silenceCount == 0) gapStart = i
                    silenceCount += RMS_WINDOW
                    if (silenceCount >= MIN_SILENCE_SAMPLES) {
                        // Found silence gap — cut at its end
                        val cutPoint = gapStart + MIN_SILENCE_SAMPLES
                        if (segment.size - cutPoint < MIN_REMAINDER_SAMPLES) return null
                        return segment.copyOfRange(cutPoint, segment.size)
                    }
                } else {
                    silenceCount = 0
                    gapStart = -1
                }

                // If we're past 90% of the segment with no gap found, give up
                if (i > segment.size * 0.9 && gapStart < 0) return null

                i += RMS_WINDOW
            }
            return null
        }
    }
}
