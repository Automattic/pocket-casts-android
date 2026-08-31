package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineCanaryModelConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * NVIDIA Canary Flash 182m backend for de/es/fr (English capability is the model's own
 * but English routes to SenseVoice for speed).
 *
 * Canary Flash is a distilled multilingual model with native speech translation to
 * English. It runs through sherpa-onnx `OfflineRecognizer` with an `OfflineCanaryModelConfig`
 * that selects source/target languages. For the de/es/fr cohort the default task is
 * **translate** (`srcLang` = OS locale, `tgtLang` = "en") so commands arrive as English,
 * matching the monolingual intent-router design.
 */
@Singleton
class CanaryFlashBackend @Inject constructor(
    private val currentLocale: () -> Locale = { Locale.getDefault() },
) : AsrBackend {

    private var recognizer: OfflineRecognizer? = null
    private var modelDir: File? = null

    override suspend fun ensureReady(): Result<Unit> = withContext(Dispatchers.IO) {
        val dir = modelDir
        if (dir == null || !dir.exists()) {
            return@withContext Result.failure(IllegalStateException("Canary Flash model directory not set"))
        }
        val encoderFile = File(dir, CANARY_ENCODER_FILENAME)
        val decoderFile = File(dir, CANARY_DECODER_FILENAME)
        val tokensFile = File(dir, CANARY_TOKENS_FILENAME)
        if (!encoderFile.exists() || !decoderFile.exists() || !tokensFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Canary Flash model files missing"))
        }
        val srcLang = canarySourceLanguage()
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    canary = OfflineCanaryModelConfig(
                        encoder = encoderFile.absolutePath,
                        decoder = decoderFile.absolutePath,
                        srcLang = srcLang,
                        tgtLang = "en",
                        usePnc = true,
                    ),
                    tokens = tokensFile.absolutePath,
                    numThreads = 4,
                    provider = "cpu",
                ),
            )
            recognizer = OfflineRecognizer(config = config)
            Timber.i("CanaryFlashBackend ready (src=%s, tgt=en)", srcLang)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Canary Flash initialization failed")
            Result.failure(e)
        }
    }

    override suspend fun transcribe(samples: FloatArray, sampleRateHz: Int): AsrResult = withContext(Dispatchers.IO) {
        val rec = recognizer
        if (rec == null) {
            return@withContext AsrResult(text = "", detectedLanguage = null)
        }
        try {
            val stream = rec.createStream()
            stream.acceptWaveform(samples, sampleRateHz)
            rec.decode(stream)
            val result = rec.getResult(stream)
            stream.release()
            val trimmed = result.text.trim()
            if (trimmed.isEmpty()) {
                AsrResult(text = "", detectedLanguage = canarySourceLanguage())
            } else {
                // Canary translates to English natively, so the transcript is English.
                AsrResult(text = trimmed, detectedLanguage = "en")
            }
        } catch (e: Exception) {
            Timber.e(e, "Canary Flash transcription failed")
            AsrResult(text = "", detectedLanguage = null)
        }
    }

    /**
     * Sets the model directory path. Called by
     * [au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager] after download.
     */
    fun setModelDir(dir: File) {
        modelDir = dir
    }

    override val requiredModel: ModelSpec = ModelSpec(
        files = listOf(
            ModelFile(url = "$CANARY_BASE_URL/$CANARY_ENCODER_FILENAME", filename = CANARY_ENCODER_FILENAME),
            ModelFile(url = "$CANARY_BASE_URL/$CANARY_DECODER_FILENAME", filename = CANARY_DECODER_FILENAME),
            ModelFile(url = "$CANARY_BASE_URL/$CANARY_TOKENS_FILENAME", filename = CANARY_TOKENS_FILENAME),
        ),
        targetDir = CANARY_TARGET_DIR,
    )

    override val capabilities: AsrCapabilities = AsrCapabilities(
        supportedLanguages = setOf("en", "de", "es", "fr"),
        canTranslateToEnglish = true,
        requiresSnapdragon = false,
    )

    override fun release() {
        recognizer?.release()
        recognizer = null
    }

    private fun canarySourceLanguage(): String = currentLocale().language

    companion object {
        // INT8 export of NVIDIA canary-180m-flash (en/de/es/fr), confirmed on HuggingFace:
        // csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8
        private const val CANARY_BASE_URL =
            "https://hf-mirror.com/csukuangfj/sherpa-onnx-nemo-canary-180m-flash-en-es-de-fr-int8/resolve/main"
        private const val CANARY_ENCODER_FILENAME = "encoder.int8.onnx"
        private const val CANARY_DECODER_FILENAME = "decoder.int8.onnx"
        private const val CANARY_TOKENS_FILENAME = "tokens.txt"
        private const val CANARY_TARGET_DIR = "canary-flash-model"
    }
}
