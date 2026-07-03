package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SherpaOnnxKwsDetector @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) : WakeWordDetector {

    private var spotter: KeywordSpotter? = null

    override val isReady: Boolean
        get() = spotter != null

    init {
        try {
            val config = KeywordSpotterConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = "kws/encoder.onnx",
                        decoder = "kws/decoder.onnx",
                        joiner = "kws/joiner.onnx",
                    ),
                    tokens = "kws/tokens.txt",
                    modelType = "zipformer2",
                    modelingUnit = "bpe",
                    bpeVocab = "kws/bpe.model",
                ),
                keywordsFile = "kws/keywords.txt",
            )
            spotter = KeywordSpotter(context.assets, config)
            Timber.i("SherpaOnnxKwsDetector initialized with bundled model")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize sherpa-onnx KWS detector")
            spotter = null
        }
    }

    // Audio is fed to the model in fixed-size chunks matching the Oboe capture frame size.
    // The KeywordSpotter streaming API expects incremental input; feeding one large array
    // can produce feature frame counts that aren't compatible with the model's internal
    // reshape nodes, causing a native Ort::Exception that crashes the process.
    private companion object {
        const val CHUNK_SAMPLES = 1024 // matches OboeConfig.FRAMES_PER_POLL
    }

    override suspend fun detect(segment: FloatArray, sampleRateHz: Int): WakeWordResult {
        val kws = spotter ?: return WakeWordResult(detected = false)

        return try {
            val stream = kws.createStream()
            var detected = false
            var keywordEndSec = 0f

            var offset = 0
            while (offset < segment.size) {
                val remaining = segment.size - offset
                val chunkSize = minOf(CHUNK_SAMPLES, remaining)
                stream.acceptWaveform(segment.copyOfRange(offset, offset + chunkSize), sampleRateHz)
                offset += chunkSize

                while (kws.isReady(stream)) {
                    kws.decode(stream)
                    val result = kws.getResult(stream)
                    if (result.keyword.isNotEmpty()) {
                        Timber.i("KWS detected: %s", result.keyword)
                        detected = true
                        if (result.timestamps.size >= 2) {
                            keywordEndSec = result.timestamps[1]
                        }
                    }
                    kws.reset(stream)
                }
            }
            stream.release()

            if (!detected) return WakeWordResult(detected = false)

            // If the keyword is at the start of the segment, trim it to get the
            // command remainder ("Auris, skip forward" → "skip forward")
            val remainder = if (keywordEndSec > 0.1f && keywordEndSec < segment.size / sampleRateHz * 0.9f) {
                val endSample = (keywordEndSec * sampleRateHz).toInt().coerceAtMost(segment.size)
                segment.copyOfRange(endSample, segment.size)
            } else {
                null
            }

            WakeWordResult(detected = true, confidence = 1.0f, remainderSamples = remainder)
        } catch (e: Exception) {
            Timber.w(e, "KWS detection failed")
            WakeWordResult(detected = false)
        }
    }

    override fun release() {
        spotter?.release()
        spotter = null
    }
}
