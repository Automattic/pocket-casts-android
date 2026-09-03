package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Wake word detector using an openWakeWord Conv-Attention classifier trained via livekit-wakeword.
 *
 * The deployment threshold is loaded from [assets/oww/auris_eval.json]. Missing, invalid,
 * or legacy threshold fields leave the detector unready instead of changing its operating point.
 */
@Singleton
class OpenWakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) : WakeWordDetector {

    override val isReady: Boolean
        get() = ready

    @Volatile
    private var ready = false
    val detectionThreshold: Float

    init {
        val thresholdResult = WakeWordThresholdLoader.load(context.assets)
        detectionThreshold = thresholdResult.getOrNull() ?: Float.NaN
        if (thresholdResult.isFailure) {
            Timber.e(thresholdResult.exceptionOrNull(), "Wake word threshold validation failed")
        } else {
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
    }

    override suspend fun detect(
        segment: FloatArray,
        sampleRateHz: Int,
        speechOnsetSample: Int,
    ): WakeWordResult {
        if (!ready) return WakeWordResult(detected = false, error = true)
        if (sampleRateHz != 16000) return WakeWordResult(detected = false, error = true)
        if (speechOnsetSample !in 0..segment.size) return WakeWordResult(detected = false, error = true)

        return withContext(Dispatchers.IO) {
            try {
                val outOffset = FloatArray(1)
                val detectorSamples = onsetAlignedSamples(segment, speechOnsetSample)
                val score = WakeWordJni.nativeDetect(detectorSamples, sampleRateHz, outOffset)
                when {
                    score < 0f -> WakeWordResult(detected = false, error = true)

                    score >= detectionThreshold -> {
                        Timber.i("[VoicePipeline] wake %.3f >= %.3f", score, detectionThreshold)
                        val onsetRel = outOffset[0].toInt().takeIf { it >= 0 } ?: 0
                        WakeWordResult(
                            detected = true,
                            confidence = score.coerceAtMost(1f),
                            completionSample = (speechOnsetSample + onsetRel).coerceIn(0, segment.size),
                        )
                    }

                    else -> {
                        Timber.i("[VoicePipeline] wake %.3f < %.3f", score, detectionThreshold)
                        WakeWordResult(detected = false, confidence = score.coerceAtMost(1f))
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Wake word detection failed")
                WakeWordResult(detected = false, error = true)
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

    companion object {
        internal fun onsetAlignedSamples(segment: FloatArray, speechOnsetSample: Int): FloatArray {
            require(speechOnsetSample in 0..segment.size) {
                "Speech onset $speechOnsetSample outside segment of ${segment.size} samples"
            }
            return if (speechOnsetSample == 0) segment else segment.copyOfRange(speechOnsetSample, segment.size)
        }
    }
}
